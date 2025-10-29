package com.example.facialrecognition;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.facialrecognition.model.CountRecord;
import com.example.facialrecognition.model.ImageData;
import com.example.facialrecognition.model.Person;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ConfirmRecognitionActivity extends AppCompatActivity {

    private ImageView imageView;
    private TextView tvPersonCount;
    private TextView tvRecognitionInfo;
    private Button btnSave;
    private Button btnCancel;
    private ImageData imageData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_recognition);

        // 初始化视图
        imageView = findViewById(R.id.iv_preview);
        tvPersonCount = findViewById(R.id.tv_person_count);
        tvRecognitionInfo = findViewById(R.id.tv_recognition_info);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);

        try {
            // 安全获取数据
            Intent intent = getIntent();
            if (intent == null) {
                handleError("无数据传递");
                return;
            }
            
            // 优先尝试从JSON获取数据
            String json = intent.getStringExtra("image_json");
            if (json != null && !json.isEmpty()) {
                Log.d("ConfirmRecognition", "获取到JSON数据，开始解析");
                
                try {
                    // 使用Gson解析JSON数据
                    Gson gson = new Gson();
                    this.imageData = gson.fromJson(json, ImageData.class);
                    
                    if (this.imageData != null) {
                        Log.d("ConfirmRecognition", "成功将JSON解析为ImageData对象");
                        // 安全显示数据
                        displayRecognitionInfo();
                    } else {
                        Log.e("ConfirmRecognition", "JSON解析为null");
                        handleError("数据解析失败");
                    }
                } catch (Exception e) {
                    Log.e("ConfirmRecognition", "JSON解析异常", e);
                    // 如果JSON解析失败，尝试备用方式
                    tryFallbackData(intent);
                }
            } else {
                Log.d("ConfirmRecognition", "未获取到JSON数据，尝试备用方式");
                // 如果没有JSON数据，尝试备用方式
                tryFallbackData(intent);
            }
        } catch (Exception e) {
            Log.e("ConfirmRecognition", "初始化错误", e);
            handleError("数据处理失败");
        }

        // 设置按钮监听器
        btnSave.setOnClickListener(v -> saveRecognition());
        btnCancel.setOnClickListener(v -> cancelRecognition());
    }
    
    /**
     * 尝试使用备用方式获取数据
     */
    private void tryFallbackData(Intent intent) {
        try {
            // 尝试获取基本统计数据
            int recognizedCount = intent.getIntExtra("RECOGNIZED_COUNT", 0);
            int manuallyAddedCount = intent.getIntExtra("MANUALLY_ADDED_COUNT", 0);
            int manuallyDeletedCount = intent.getIntExtra("MANUALLY_DELETED_COUNT", 0);
            
            Log.d("ConfirmRecognition", "尝试备用方式获取数据，识别数: " + recognizedCount + 
                    ", 手动添加: " + manuallyAddedCount + ", 手动删除: " + manuallyDeletedCount);
            
            // 创建一个基本的ImageData对象
            ImageData fallbackData = new ImageData("", null);
            fallbackData.setRecognizedCount(recognizedCount);
            fallbackData.setManuallyAddedCount(manuallyAddedCount);
            fallbackData.setManuallyDeletedCount(manuallyDeletedCount);
            
            this.imageData = fallbackData;
            displayRecognitionInfo();
            
            Log.d("ConfirmRecognition", "备用方式获取数据成功，计算人数: " + 
                    (recognizedCount + manuallyAddedCount - manuallyDeletedCount));
        } catch (Exception e) {
            Log.e("ConfirmRecognition", "备用方式获取数据失败", e);
            handleError("数据获取失败");
        }
    }
    
    // 错误处理方法
    private void handleError(String message) {
        Log.e("ConfirmRecognition", message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        // 创建一个空的ImageData对象作为备选，至少可以显示界面
        this.imageData = new ImageData("", null);
        // 显示空数据的界面
        displayRecognitionInfo();
    }

    private void displayRecognitionInfo() {
        // 添加空值检查
        if (imageData == null) {
            return;
        }
        
        // 设置预览图片
        if (imageData.getBitmap() != null) {
            imageView.setImageBitmap(imageData.getBitmap());
        } else if (imageData.getThumbnail() != null) {
            imageView.setImageBitmap(imageData.getThumbnail());
        }

        // 获取有效人数 - 使用多种方式确保准确性
        int visibleCount = imageData.getValidPersonCount();
        
        // 如果直接计算返回0，尝试使用另一种计算方式
        if (visibleCount == 0 && imageData != null) {
            // 使用识别计数和手动调整来计算人数
            visibleCount = imageData.getRecognizedCount() + imageData.getManuallyAddedCount() - imageData.getManuallyDeletedCount();
            
            // 确保不会出现负数
            visibleCount = Math.max(0, visibleCount);
        }
        
        Log.d("ConfirmRecognition", "显示人数: " + visibleCount);
        tvPersonCount.setText(String.format(Locale.getDefault(), "识别到 %d 人", visibleCount));

        // 简单的识别信息
        tvRecognitionInfo.setText("是否保存此次识别结果到历史记录？");
    }

    private void saveRecognition() {
        try {
            // 创建ImageData列表
            List<ImageData> imageDataList = new ArrayList<>();
            imageDataList.add(imageData);
            
            // 创建CountRecord - 使用有效人数
            int validCount = imageData.getValidPersonCount();
            CountRecord record = new CountRecord(imageDataList, validCount);
            
            // 使用HistoryManager保存记录
            HistoryManager.getInstance().saveRecord(record);
            Log.d("ConfirmRecognition", "记录已保存到HistoryManager，ID: " + record.getRecordId() + "，人数: " + validCount);
            
            Toast.makeText(this, "识别记录已保存", Toast.LENGTH_SHORT).show();
            
            // 返回主页面
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e("ConfirmRecognition", "保存识别记录失败", e);
            Toast.makeText(this, "保存失败，请重试", Toast.LENGTH_SHORT).show();
        }
    }

    private void cancelRecognition() {
        // 取消识别，直接返回主界面
        Log.d("ConfirmRecognition", "取消识别");
        Toast.makeText(this, "已取消识别", Toast.LENGTH_SHORT).show();
        
        // 返回到主界面
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限被授予，重新尝试保存
                saveRecognition();
            } else {
                Toast.makeText(this, "需要存储权限才能保存记录", Toast.LENGTH_SHORT).show();
            }
        }
    }
}