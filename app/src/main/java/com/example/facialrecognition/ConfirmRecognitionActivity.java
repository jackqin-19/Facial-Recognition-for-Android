package com.example.facialrecognition;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.facialrecognition.model.CountRecord;
import com.example.facialrecognition.model.ImageData;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import com.example.facialrecognition.model.ImageData;
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

        // 处理数据获取
        processIntentData();

        // 设置按钮监听器
        btnSave.setOnClickListener(v -> saveRecognition());
        btnCancel.setOnClickListener(v -> cancelRecognition());
    }
    
    /**
     * 处理Intent数据，优先使用JSON，失败则使用备用方式
     */
    private void processIntentData() {
        Intent intent = getIntent();
        if (intent == null) {
            Log.e("ConfirmRecognition", "无数据传递");
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
                imageData = gson.fromJson(json, ImageData.class);
                
                if (imageData != null) {
                    Log.d("ConfirmRecognition", "成功将JSON解析为ImageData对象");
                    displayRecognitionInfo();
                    return; // 成功解析，直接返回
                }
            } catch (Exception e) {
                Log.e("ConfirmRecognition", "JSON解析异常", e);
            }
            Log.d("ConfirmRecognition", "JSON解析失败，尝试备用方式");
        } else {
            Log.d("ConfirmRecognition", "未获取到JSON数据，尝试备用方式");
        }
        
        // 如果JSON解析失败或无JSON数据，尝试备用方式
        tryFallbackData(intent);
    }
    
    /**
     * 尝试使用备用方式获取数据
     */
    private void tryFallbackData(Intent intent) {
        try {
            // 获取基本统计数据
            int recognizedCount = intent.getIntExtra("RECOGNIZED_COUNT", 0);
            int manuallyAddedCount = intent.getIntExtra("MANUALLY_ADDED_COUNT", 0);
            int manuallyDeletedCount = intent.getIntExtra("MANUALLY_DELETED_COUNT", 0);
            
            // 创建基本的ImageData对象并设置数据
            ImageData fallbackData = new ImageData("", null);
            fallbackData.setRecognizedCount(recognizedCount);
            fallbackData.setManuallyAddedCount(manuallyAddedCount);
            fallbackData.setManuallyDeletedCount(manuallyDeletedCount);
            
            imageData = fallbackData;
            displayRecognitionInfo();
            
            Log.d("ConfirmRecognition", "备用方式获取数据成功，人数: " + 
                    Math.max(0, recognizedCount + manuallyAddedCount - manuallyDeletedCount));
        } catch (Exception e) {
            Log.e("ConfirmRecognition", "备用方式获取数据失败", e);
            handleError("数据获取失败");
        }
    }
    
    /**
     * 统一错误处理方法
     */
    private void handleError(String message) {
        Log.e("ConfirmRecognition", message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        // 创建空的ImageData对象以显示界面
        imageData = new ImageData("", null);
        displayRecognitionInfo();
    }

    private void displayRecognitionInfo() {
        // 空值检查
        if (imageData == null) {
            return;
        }
        
        // 设置预览图片
        if (imageData.getBitmap() != null) {
            imageView.setImageBitmap(imageData.getBitmap());
        } else if (imageData.getThumbnail() != null) {
            imageView.setImageBitmap(imageData.getThumbnail());
        }

        // 计算有效人数，确保不会出现负数
        int visibleCount = imageData.getValidPersonCount();
        if (visibleCount == 0) {
            visibleCount = Math.max(0, imageData.getRecognizedCount() + 
                    imageData.getManuallyAddedCount() - imageData.getManuallyDeletedCount());
        }
        
        Log.d("ConfirmRecognition", "显示人数: " + visibleCount);
        tvPersonCount.setText(String.format(Locale.getDefault(), "识别到 %d 人", visibleCount));
        tvRecognitionInfo.setText("是否保存此次识别结果到历史记录？");
    }

    private void saveRecognition() {
        try {
            // 计算有效人数
            int validCount = imageData.getValidPersonCount();
            
            // 创建记录并保存
            List<ImageData> imageDataList = new ArrayList<>();
            imageDataList.add(imageData);
            CountRecord record = new CountRecord(imageDataList, validCount);
            
            // 使用HistoryManager保存记录
            HistoryManager.getInstance().saveRecord(record);
            Log.d("ConfirmRecognition", "记录已保存，ID: " + record.getRecordId() + "，人数: " + validCount);
            
            Toast.makeText(this, "识别记录已保存", Toast.LENGTH_SHORT).show();
            navigateBackToMain();
        } catch (Exception e) {
            Log.e("ConfirmRecognition", "保存识别记录失败", e);
            Toast.makeText(this, "保存失败，请重试", Toast.LENGTH_SHORT).show();
        }
    }

    private void cancelRecognition() {
        Log.d("ConfirmRecognition", "取消识别");
        Toast.makeText(this, "已取消识别", Toast.LENGTH_SHORT).show();
        navigateBackToMain();
    }
    
    /**
     * 统一返回到主界面的方法
     */
    private void navigateBackToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}