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
import android.util.Log;

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
        // 放宽StrictMode策略以避免某些安全策略限制
        if (android.os.Build.VERSION.SDK_INT > 9) {
            android.os.StrictMode.ThreadPolicy policy = new android.os.StrictMode.ThreadPolicy.Builder().permitAll().build();
            android.os.StrictMode.setThreadPolicy(policy);
        }
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        
        // 首次启动时清理假记录并初始化一些测试数据（仅用于演示）
        initializeTestData();

        // 初始化视图
        recyclerView = findViewById(R.id.recycler_view_history);
        ImageButton btnBack = findViewById(R.id.btn_back);
        ImageButton btnFilter = findViewById(R.id.btn_filter);
        ImageButton btnTrash = findViewById(R.id.btn_trash);

        // 初始化数据
        historyRecords = new ArrayList<>();
        loadHistoryData(); // 加载真实历史记录

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
        // 从HistoryManager加载真实的历史记录
        historyRecords.clear();
        historyRecords.addAll(HistoryManager.getInstance().getAllHistoryRecords());
        
        // 如果没有真实记录，添加一条提示
        if (historyRecords.isEmpty()) {
            Toast.makeText(this, "暂无历史记录，请先保存识别结果", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 每次回到此页面时重新加载历史记录
        loadHistoryData();
        adapter.notifyDataSetChanged();
    }
    
    /**
     * 初始化测试数据（仅用于演示）
     */
    private void initializeTestData() {
        // 如果没有历史记录，添加一些测试数据
        if (HistoryManager.getInstance().getRecordCount() == 0) {
            Log.d("HistoryActivity", "初始化测试数据");
            // 这里可以添加测试数据，但为了避免假记录，我们不再添加模拟数据
            // 用户需要通过正常流程保存真实记录
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
        // 新的查看记录详情方式
        // 如果有图片数据，则跳转到RecognitionActivity查看第一个图片
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
        PopupMenu popupMenu = new PopupMenu(this, anchorView);
        popupMenu.getMenuInflater().inflate(R.menu.record_options_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_record_delete) {
                deleteRecord(record);
            }
            return true;
        });

        popupMenu.show();
    }

    private void deleteRecord(CountRecord record) {
        // 使用HistoryManager删除记录（移至回收站）
        HistoryManager.getInstance().moveToTrash(record);
        
        // 从当前列表中移除并刷新UI
        historyRecords.remove(record);
        adapter.notifyDataSetChanged();
        
        Toast.makeText(this, "记录已移至回收站", Toast.LENGTH_SHORT).show();
    }


}