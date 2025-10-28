package com.example.facialrecognition;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.example.facialrecognition.model.CountRecord;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private List<CountRecord> historyRecords;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 放宽StrictMode策略以避免某些安全策略限制
        if (android.os.Build.VERSION.SDK_INT > 9) {
            android.os.StrictMode.ThreadPolicy policy = new android.os.StrictMode.ThreadPolicy.Builder().permitAll().build();
            android.os.StrictMode.setThreadPolicy(policy);
        }
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // 初始化视图
        recyclerView = findViewById(R.id.recycler_view_history);
        ImageButton btnBack = findViewById(R.id.btn_back);
        ImageButton btnFilter = findViewById(R.id.btn_filter);
        ImageButton btnTrash = findViewById(R.id.btn_trash);

        // 初始化数据
        historyRecords = new ArrayList<>();
        loadMockData(); // 加载模拟数据

        // 设置RecyclerView
        adapter = new HistoryAdapter(historyRecords);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // 设置按钮监听器
        btnBack.setOnClickListener(v -> finish());
        btnFilter.setOnClickListener(v -> showFilterOptions());
        btnTrash.setOnClickListener(v -> goToTrash());
    }

    private void loadMockData() {
        // 加载模拟的历史记录数据
        // 实际应用中应该从数据库或文件中加载真实数据
        for (int i = 0; i < 5; i++) {
            // 创建模拟的历史记录
            CountRecord mockRecord = CountRecord.createMockRecord(i);
            historyRecords.add(mockRecord);
        }
    }

    private void showFilterOptions() {
        PopupMenu popupMenu = new PopupMenu(this, findViewById(R.id.btn_filter));
        popupMenu.getMenuInflater().inflate(R.menu.filter_options_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_filter_week) {
                filterByTimeRange(7);
            } else if (itemId == R.id.menu_filter_month) {
                filterByTimeRange(30);
            } else if (itemId == R.id.menu_filter_operation) {
                filterByOperation();
            } else if (itemId == R.id.menu_filter_count) {
                filterByCountRange(70, 90);
            } else if (itemId == R.id.menu_filter_clear) {
                clearFilters();
            }
            return true;
        });

        popupMenu.show();
    }

    private void filterByTimeRange(int days) {
        // 按时间范围筛选
        // 实际应用中应该实现真正的筛选逻辑
        Toast.makeText(this, "筛选近" + days + "天的记录", Toast.LENGTH_SHORT).show();
    }

    private void filterByOperation() {
        // 按操作类型筛选
        Toast.makeText(this, "按操作类型筛选", Toast.LENGTH_SHORT).show();
    }

    private void filterByCountRange(int min, int max) {
        // 按人数范围筛选
        Toast.makeText(this, "筛选" + min + "-" + max + "人的记录", Toast.LENGTH_SHORT).show();
    }

    private void clearFilters() {
        // 清除所有筛选
        Toast.makeText(this, "清除所有筛选", Toast.LENGTH_SHORT).show();
    }

    private void goToTrash() {
        // 跳转到回收站页面
        Toast.makeText(this, "跳转到回收站", Toast.LENGTH_SHORT).show();
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

        private List<CountRecord> records;

        public HistoryAdapter(List<CountRecord> records) {
            this.records = records;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_history_record, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            CountRecord record = records.get(position);
            
            // 设置数据
            if (record.getThumbnail() != null) {
                holder.thumbnailImageView.setImageBitmap(record.getThumbnail());
            } else {
                // 设置默认占位图
                holder.thumbnailImageView.setImageResource(R.drawable.ic_launcher_background);
            }
            holder.timestampTextView.setText(record.getFormattedTimestamp());
            holder.countTextView.setText("总人数：" + record.getTotalPersonCount() + " 人");
            
            // 设置点击监听器
            holder.itemView.setOnClickListener(v -> viewRecordDetails(record));
            
            // 设置长按监听器
            holder.itemView.setOnLongClickListener(v -> {
                showRecordOptions(holder.itemView, record);
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return records.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView thumbnailImageView;
            TextView timestampTextView;
            TextView countTextView;

            public ViewHolder(View itemView) {
                super(itemView);
                thumbnailImageView = itemView.findViewById(R.id.iv_record_thumbnail);
                timestampTextView = itemView.findViewById(R.id.tv_record_timestamp);
                countTextView = itemView.findViewById(R.id.tv_record_count);
            }
        }
    }

    private void viewRecordDetails(CountRecord record) {
        // 查看记录详情
        Intent intent = new Intent(this, MergeResultActivity.class);
        intent.putExtra("COUNT_RECORD", record);
        startActivity(intent);
    }

    private void showRecordOptions(View anchorView, CountRecord record) {
        PopupMenu popupMenu = new PopupMenu(this, anchorView);
        popupMenu.getMenuInflater().inflate(R.menu.record_options_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_record_delete) {
                deleteRecord(record);
            } else if (itemId == R.id.menu_record_export) {
                exportRecord(record);
            }
            return true;
        });

        popupMenu.show();
    }

    private void deleteRecord(CountRecord record) {
        // 删除记录（移至回收站）
        record.moveToTrash();
        historyRecords.remove(record);
        adapter.notifyDataSetChanged();
        Toast.makeText(this, "记录已移至回收站", Toast.LENGTH_SHORT).show();
    }

    private void exportRecord(CountRecord record) {
        // 导出记录
        Toast.makeText(this, "导出记录", Toast.LENGTH_SHORT).show();
        // 实际应用中应该跳转到导出页面或直接执行导出操作
    }
}