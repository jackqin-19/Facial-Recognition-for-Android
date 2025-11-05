package com.example.facialrecognition;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraState;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 101;
    private static final String TAG = "CameraActivity";
    private PreviewView previewView;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private TextView tvGuide;
    private boolean isSurfaceActive = true; // Flag to track if surface is active
    private ProcessCameraProvider cameraProvider; // Keep reference to camera provider
    private int cameraRetryCount = 0;
    private static final int MAX_CAMERA_RETRIES = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        previewView = findViewById(R.id.preview_view);
        tvGuide = findViewById(R.id.tv_guide);
        Button btnCapture = findViewById(R.id.btn_capture);
        ImageButton btnBack = findViewById(R.id.btn_back);

        tvGuide.setText("请将整个需要识别的区域纳入相机画面");
        
        // 隐藏不再需要的区域选择RadioGroup
        findViewById(R.id.radio_group_area).setVisibility(View.GONE);
        
        // 显示简单的拍照指导
        showSimpleGuide();

        // 拍照按钮
        btnCapture.setOnClickListener(v -> takePhoto());

        // 返回按钮
        btnBack.setOnClickListener(v -> finish());

        cameraExecutor = Executors.newSingleThreadExecutor();

        // 检查相机权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            startCamera();
        }
    }

    private void showSimpleGuide() {
        Toast.makeText(this, "请确保画面清晰，尽量将所有需要识别的人群纳入画面", Toast.LENGTH_LONG).show();
    }

    private void startCamera() {
        // 检查相机是否可用
        if (!isCameraAvailable()) {
            Toast.makeText(this, "设备没有可用相机", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // 确保存储目录存在
        ensureStorageDirectory();
        
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // 创建预览，设置目标旋转
                Preview preview = new Preview.Builder()
                        .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                        .build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                this.cameraProvider = cameraProvider;
                cameraProvider.unbindAll();
                Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
                isSurfaceActive = true; // Set surface as active when camera is started
                
                // 添加相机状态监听
                camera.getCameraInfo().getCameraState().observe(this, cameraState -> {
                    Log.d(TAG, "相机状态: " + cameraState.getType());
                    
                    if (cameraState.getType() == CameraState.Type.CLOSED) {
                        Log.e(TAG, "相机意外关闭");
                        handleCameraError();
                    }
                });
                
                // 重置重试计数
                cameraRetryCount = 0;

            } catch (Exception e) {
                Log.e(TAG, "相机启动失败", e);
                Toast.makeText(this, "相机启动失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                handleCameraError();
            }
        }, ContextCompat.getMainExecutor(this));
    }
    
    // 检查相机是否可用
    private boolean isCameraAvailable() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
    }
    
    // 处理相机错误并实现重试机制
    // 确保存储目录存在并可写
    private boolean ensureStorageDirectory() {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir == null) {
            Log.e(TAG, "存储目录不存在");
            return false;
        }
        
        if (!storageDir.exists() && !storageDir.mkdirs()) {
            Log.e(TAG, "无法创建存储目录");
            Toast.makeText(this, "无法创建存储目录", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        return true;
    }
    
    // 处理相机错误并实现重试机制
    private void handleCameraError() {
        if (cameraRetryCount < MAX_CAMERA_RETRIES) {
            cameraRetryCount++;
            Log.i(TAG, "尝试重新启动相机，重试次数: " + cameraRetryCount);
            
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!isFinishing() && !isDestroyed() && isSurfaceActive) {
                    startCamera();
                }
            }, 1000); // 1秒后重试
        } else {
            Log.e(TAG, "达到最大重试次数，相机启动失败");
            runOnUiThread(() -> {
                Toast.makeText(CameraActivity.this, "相机启动失败，请重启应用或检查设备相机", Toast.LENGTH_LONG).show();
                // 返回到主界面
                finish();
            });
        }
    }

    private void takePhoto() {
        if (imageCapture == null || !isSurfaceActive) {
            Log.w(TAG, "无法拍照：相机未初始化或表面已销毁");
            return;
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "CLASS_CLASS_" + timeStamp + ".jpg";

        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        
        // 确保目录存在
        if (!ensureStorageDirectory()) {
            Toast.makeText(this, "无法访问存储目录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        File imageFile = new File(storageDir, fileName);

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(imageFile).build();

        // 创建一个完成回调，用于处理图片保存后的操作
        Runnable postCaptureAction = () -> {
            // 扫描文件使其在相册中可见
            MediaScannerConnection.scanFile(CameraActivity.this,
                    new String[]{imageFile.getAbsolutePath()},
                    null, null);

            // 跳转到识别页面
            Intent intent = new Intent(CameraActivity.this, RecognitionActivity.class);
            intent.putExtra("IMAGE_PATH", imageFile.getAbsolutePath());
            startActivity(intent);
        };
        
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), 
            new ImageCapture.OnImageSavedCallback() {
                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                    // 图片保存成功后，直接执行后续操作
                    runOnUiThread(postCaptureAction);
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    Toast.makeText(CameraActivity.this, "拍照失败: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }



    // 辅助线相关功能已移除

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "需要相机权限才能拍摄照片", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Mark surface as inactive to prevent buffer operations on abandoned surface
        isSurfaceActive = false;
        
        // 不在这里解除绑定，遵循CameraX最佳实践
        // 只标记状态，让CameraX生命周期管理来处理
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reactivate surface
        isSurfaceActive = true;
        
        // 只有在cameraProvider为null时才重新启动相机
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED && cameraProvider == null) {
            startCamera();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isSurfaceActive = false; // Ensure surface is marked as inactive on destroy
        
        // Release camera resources
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            cameraProvider = null; // 设置为null以释放引用
        }
        
        // Shutdown executor with timeout to prevent stuck threads
        cameraExecutor.shutdownNow();
    }
}