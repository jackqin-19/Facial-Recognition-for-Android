package com.example.facialrecognition;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

/**
 * 操作指南页面
 */
public class GuideActivity extends AppCompatActivity {

    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);

        // 初始化返回按钮
        btnBack = findViewById(R.id.btn_back);

        // 设置返回按钮点击事件
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // 关闭当前页面，返回上一页
            }
        });

        // 添加返回键处理回调
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish(); // 处理返回操作
            }
        });
    }
}