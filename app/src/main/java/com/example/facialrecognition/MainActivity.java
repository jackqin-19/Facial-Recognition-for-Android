package com.example.facialrecognition;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private Button btnTakePhoto, btnUploadPhoto, btnHistory, btnGuide;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 放宽StrictMode以避免SELinux权限问题
        android.os.StrictMode.setVmPolicy(new android.os.StrictMode.VmPolicy.Builder().build());
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 初始化按钮
        btnTakePhoto = findViewById(R.id.btn_take_photo);
        btnUploadPhoto = findViewById(R.id.btn_upload_photo);
        btnHistory = findViewById(R.id.btn_history);
        btnGuide = findViewById(R.id.btn_guide);
        
        // 设置点击事件监听器
        btnTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 跳转到相机页面
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                startActivity(intent);
            }
        });
        
        btnUploadPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 暂时显示提示，后续可实现图片选择功能
                Toast.makeText(MainActivity.this, "上传照片功能开发中", Toast.LENGTH_SHORT).show();
                // 后续可以实现选择图片然后跳转到RecognitionActivity
            }
        });
        
        btnHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 跳转到历史记录页面
                Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
                startActivity(intent);
            }
        });
        
        btnGuide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 跳转到操作指南页面
                Intent intent = new Intent(MainActivity.this, GuideActivity.class);
                startActivity(intent);
            }
        });
    }
}