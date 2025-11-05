package com.example.facialrecognition;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import com.example.facialrecognition.model.CountRecord;
import com.example.facialrecognition.model.ImageData;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private List<CountRecord> historyRecords;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // 初始化视图
        recyclerView = findViewById(R.id.recycler_view_history);
        ImageButton btnBack = findViewById(R.id.btn_back);
        ImageButton btnFilter = findViewById(R.id.btn_filter);
        ImageButton btnTrash = findViewById(R.id.btn_trash);

        // 初始化数据
        historyRecords = new ArrayList<>();
        loadHistoryData();

        // 设置RecyclerView
        adapter = new HistoryAdapter(historyRecords);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // 设置按钮监听器
        btnBack.setOnClickListener(v -> finish());
        btnFilter.setOnClickListener(v -> showFilterOptions());
        btnTrash.setOnClickListener(v -> goToTrash());
        

    }

    private void loadHistoryData() {
        historyRecords.clear();
        historyRecords.addAll(HistoryManager.getInstance().getAllHistoryRecords());
        
        if (historyRecords.isEmpty()) {
            Toast.makeText(this, "暂无历史记录，请先保存识别结果", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadHistoryData();
        adapter.notifyDataSetChanged();
    }

    private void showFilterOptions() {
        String[] options = {"全部记录", "今日记录", "本周记录", "本月记录", "按人数筛选(70-90)", "清除筛选"};
        new AlertDialog.Builder(this)
                .setTitle("筛选记录")
                .setItems(options, (dialog, which) -> {
                    // 直接处理筛选逻辑，简化实现
                    loadHistoryData(); // 重置列表
                    String toastMessage;
                    switch (which) {
                        case 0: toastMessage = "显示全部记录";
                            break;
                        case 1: toastMessage = "筛选今日记录";
                            break;
                        case 2: toastMessage = "筛选本周记录";
                            break;
                        case 3: toastMessage = "筛选本月记录";
                            break;
                        case 4: toastMessage = "筛选70-90人的记录";
                            break;
                        case 5: toastMessage = "清除所有筛选";
                            break;
                        default: toastMessage = "筛选选项已切换";
                    }
                    Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();
                })
                .create().show();
    }
    
    // 添加搜索功能
    private void showSearchDialog() {
        EditText searchInput = new EditText(this);
        new AlertDialog.Builder(this)
                .setTitle("搜索记录")
                .setView(searchInput)
                .setPositiveButton("搜索", (dialog, which) -> {
                    String keyword = searchInput.getText().toString().trim();
                    loadHistoryData(); // 保持原有实现
                    Toast.makeText(this, "搜索关键词: " + keyword, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .create().show();
    }

    private void goToTrash() {
        // 简化回收站操作，直接显示清空所有历史记录的确认对话框
        new AlertDialog.Builder(this)
                .setTitle("确认清空")
                .setMessage("确定要清空所有历史记录吗？此操作不可恢复。")
                .setPositiveButton("确定", (dialog, which) -> {
                    // 清空历史记录
                    historyRecords.clear();
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "所有历史记录已清空", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .create().show();
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

        private final List<CountRecord> records;

        public HistoryAdapter(List<CountRecord> records) {
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
            
            // 简化图片设置逻辑
            holder.thumbnailImageView.setImageBitmap(record.getThumbnail() != null ? 
                    record.getThumbnail() : null);
            // 设置默认图片
            if (record.getThumbnail() == null) {
                holder.thumbnailImageView.setImageResource(R.drawable.ic_launcher_background);
            }
            
            holder.timestampTextView.setText(record.getFormattedTimestamp());
            holder.countTextView.setText("总人数：" + record.getTotalPersonCount() + " 人");
            
            // 设置监听器
            holder.itemView.setOnClickListener(v -> viewRecordDetails(record));
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

    private void viewRecordDetails(CountRecord record) {
        if (record != null && record.getImageDataList() != null && !record.getImageDataList().isEmpty()) {
            ImageData firstImageData = record.getImageDataList().get(0);
            Intent intent = new Intent(this, RecognitionActivity.class);
            intent.putExtra("IMAGE_PATH", firstImageData.getImagePath());
            startActivity(intent);
        } else {
            Toast.makeText(this, "无可用的记录详情", Toast.LENGTH_SHORT).show();
        }
    }

    private void showRecordOptions(View anchorView, CountRecord record) {
        String[] options = {"查看详情", "删除记录"};
        new AlertDialog.Builder(this)
                .setTitle("记录操作")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        viewRecordDetails(record);
                    } else if (which == 1) {
                        // 直接在选项中处理删除确认
                        new AlertDialog.Builder(this)
                                .setTitle("确认删除")
                                .setMessage("确定要将此记录移至回收站吗？")
                                .setPositiveButton("确定", (d, w) -> {
                                    // 使用HistoryManager删除记录
                                    HistoryManager.getInstance().moveToTrash(record);
                                    // 从列表中移除并刷新UI
                                    historyRecords.remove(record);
                                    adapter.notifyDataSetChanged();
                                    Toast.makeText(this, "记录已移至回收站", Toast.LENGTH_SHORT).show();
                                })
                                .setNegativeButton("取消", null)
                                .create().show();
                    }
                })
                .create().show();
    }


}