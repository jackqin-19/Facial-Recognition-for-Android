package com.example.facialrecognition;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.facialrecognition.model.CountRecord;

import java.util.ArrayList;
import java.util.List;

public class TrashActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TrashAdapter adapter;
    private List<CountRecord> trashRecords;
    private ImageButton btnBack;
    private ImageButton btnEmptyTrash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trash);

        // 初始化视图
        recyclerView = findViewById(R.id.recycler_view_trash);
        btnBack = findViewById(R.id.btn_back);
        btnEmptyTrash = findViewById(R.id.btn_empty_trash);

        // 初始化数据
        trashRecords = new ArrayList<>();
        loadTrashData();

        // 设置RecyclerView
        adapter = new TrashAdapter(trashRecords);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // 设置按钮监听器
        btnBack.setOnClickListener(v -> finish());
        btnEmptyTrash.setOnClickListener(v -> showEmptyTrashConfirmation());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 每次返回此页面时重新加载回收站数据
        loadTrashData();
        adapter.notifyDataSetChanged();
    }

    /**
     * 加载回收站数据
     */
    private void loadTrashData() {
        trashRecords.clear();
        // 从HistoryManager获取回收站记录
        List<CountRecord> allTrashRecords = HistoryManager.getInstance().getTrashRecords();
        trashRecords.addAll(allTrashRecords);
        
        if (trashRecords.isEmpty()) {
            Toast.makeText(this, "回收站为空", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 显示清空回收站确认对话框
     */
    private void showEmptyTrashConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("确认清空回收站")
                .setMessage("确定要清空回收站吗？此操作不可恢复。")
                .setPositiveButton("确定", (dialog, which) -> {
                    // 清空回收站
                    HistoryManager.getInstance().emptyTrash();
                    // 刷新列表
                    loadTrashData();
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "回收站已清空", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .create().show();
    }

    /**
     * 恢复记录到历史记录
     */
    private void restoreRecord(CountRecord record) {
        if (record != null) {
            HistoryManager.getInstance().restoreFromTrash(record);
            trashRecords.remove(record);
            adapter.notifyDataSetChanged();
            Toast.makeText(this, "记录已恢复", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 永久删除记录
     */
    private void permanentlyDeleteRecord(CountRecord record) {
        if (record != null) {
            HistoryManager.getInstance().permanentDelete(record);
            trashRecords.remove(record);
            adapter.notifyDataSetChanged();
            Toast.makeText(this, "记录已永久删除", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 显示记录操作选项
     */
    private void showRecordOptions(View anchorView, CountRecord record) {
        String[] options = {"恢复记录", "永久删除"};
        new AlertDialog.Builder(this)
                .setTitle("记录操作")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // 恢复记录
                        restoreRecord(record);
                    } else if (which == 1) {
                        // 永久删除确认
                        new AlertDialog.Builder(this)
                                .setTitle("确认永久删除")
                                .setMessage("确定要永久删除此记录吗？此操作不可恢复。")
                                .setPositiveButton("确定", (d, w) -> {
                                    permanentlyDeleteRecord(record);
                                })
                                .setNegativeButton("取消", null)
                                .create().show();
                    }
                })
                .create().show();
    }

    /**
     * 回收站记录适配器
     */
    private class TrashAdapter extends RecyclerView.Adapter<TrashAdapter.ViewHolder> {

        private final List<CountRecord> records;

        public TrashAdapter(List<CountRecord> records) {
            this.records = records;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_history_record, parent, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            CountRecord record = records.get(position);
            
            // 设置缩略图
            holder.thumbnailImageView.setImageBitmap(record.getThumbnail() != null ? 
                    record.getThumbnail() : null);
            // 设置默认图片
            if (record.getThumbnail() == null) {
                holder.thumbnailImageView.setImageResource(R.drawable.ic_launcher_background);
            }
            
            holder.timestampTextView.setText(record.getFormattedTimestamp());
            holder.countTextView.setText("总人数：" + record.getTotalPersonCount() + " 人");
            
            // 设置监听器
            holder.itemView.setOnClickListener(v -> showRecordOptions(holder.itemView, record));
            holder.itemView.setOnLongClickListener(v -> {
                showRecordOptions(holder.itemView, record);
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return records.size();
        }

        // ViewHolder内部类
        class ViewHolder extends RecyclerView.ViewHolder {
            final ImageView thumbnailImageView;
            final TextView timestampTextView;
            final TextView countTextView;

            ViewHolder(View itemView) {
                super(itemView);
                thumbnailImageView = itemView.findViewById(R.id.iv_record_thumbnail);
                timestampTextView = itemView.findViewById(R.id.tv_record_timestamp);
                countTextView = itemView.findViewById(R.id.tv_record_count);
            }
        }
    }
}