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
import android.graphics.Bitmap;
import android.graphics.Canvas;
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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.facialrecognition.model.ImageData;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
    private RadioGroup radioGroupArea;
    private TextView tvGuide;
    private Canvas overlayCanvas;
    private Paint overlayPaint;
    private ImageData.AreaType currentAreaType = ImageData.AreaType.MIDDLE;
    private boolean isFirstUse = true;
    private boolean isSurfaceActive = true; // Flag to track if surface is active
    private ProcessCameraProvider cameraProvider; // Keep reference to camera provider
    private int cameraRetryCount = 0;
    private static final int MAX_CAMERA_RETRIES = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        previewView = findViewById(R.id.preview_view);
        radioGroupArea = findViewById(R.id.radio_group_area);
        tvGuide = findViewById(R.id.tv_guide);
        Button btnCapture = findViewById(R.id.btn_capture);
        ImageButton btnBack = findViewById(R.id.btn_back);

        // 初始化画笔
        overlayPaint = new Paint();
        overlayPaint.setStyle(Paint.Style.STROKE);
        overlayPaint.setStrokeWidth(3);

        // 区域选择监听器
        radioGroupArea.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_front) {
                currentAreaType = ImageData.AreaType.FRONT;
                overlayPaint.setColor(Color.RED);
                tvGuide.setText("请将前排座位纳入此框内拍摄，避免遗漏前排边缘人群");
                if (isFirstUse) {
                    showFirstUseGuide();
                    isFirstUse = false;
                }
            } else if (checkedId == R.id.radio_middle) {
                currentAreaType = ImageData.AreaType.MIDDLE;
                overlayPaint.setColor(Color.YELLOW);
                tvGuide.setText("请将中排座位完整纳入此框，覆盖前后排衔接区域");
            } else if (checkedId == R.id.radio_back) {
                currentAreaType = ImageData.AreaType.BACK;
                overlayPaint.setColor(Color.BLUE);
                tvGuide.setText("请将后排座位纳入此框，可适当抬高镜头覆盖后排角落");
            }
            // 更新预览区域
            updatePreviewOverlay();
        });

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

    private void showFirstUseGuide() {
        Toast.makeText(this, "红色框线对应前排区域，按框线拍摄更精准", Toast.LENGTH_LONG).show();
    }

    private void updatePreviewOverlay() {
        // 在预览上绘制辅助框线
        previewView.post(() -> {
            // 这里简化处理，实际应该使用自定义View或SurfaceView来绘制
            // 辅助线将在拍照后合成到图片中
        });
    }

    private void startCamera() {
        // 检查相机是否可用
        if (!isCameraAvailable()) {
            Toast.makeText(this, "设备没有可用相机", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // 检查存储目录是否存在
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir != null && !storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                Log.e(TAG, "无法创建存储目录");
                Toast.makeText(this, "无法创建图片存储目录", Toast.LENGTH_SHORT).show();
            }
        }
        
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

            } catch (ExecutionException | InterruptedException e) {
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
        String areaName = getAreaName(currentAreaType);
        String fileName = "CLASS_" + areaName + "_" + timeStamp + ".jpg";

        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        
        // 确保目录存在
        if (storageDir == null) {
            Log.e(TAG, "存储目录不存在");
            Toast.makeText(this, "无法访问存储目录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!storageDir.exists() && !storageDir.mkdirs()) {
            Log.e(TAG, "无法创建存储目录");
            Toast.makeText(this, "无法创建存储目录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        File imageFile = new File(storageDir, fileName);

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(imageFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                // 图片保存成功后，添加辅助线
                addGuidelinesToImage(imageFile.getAbsolutePath(), currentAreaType, () -> {
                    // 扫描文件使其在相册中可见
                    MediaScannerConnection.scanFile(CameraActivity.this,
                            new String[]{imageFile.getAbsolutePath()},
                            null, null);

                    // 跳转到识别页面
                    Intent intent = new Intent(CameraActivity.this, RecognitionActivity.class);
                    intent.putExtra("IMAGE_PATH", imageFile.getAbsolutePath());
                    intent.putExtra("AREA_TYPE", currentAreaType.ordinal());
                    startActivity(intent);
                });
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Toast.makeText(CameraActivity.this, "拍照失败: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getAreaName(ImageData.AreaType areaType) {
        switch (areaType) {
            case FRONT:
                return "FRONT";
            case MIDDLE:
                return "MIDDLE";
            case BACK:
                return "BACK";
            default:
                return "UNKNOWN";
        }
    }

    private void addGuidelinesToImage(String imagePath, ImageData.AreaType areaType, Runnable onComplete) {
        // 在工作线程中处理图片，但只有在surface活动时才执行
        if (!isSurfaceActive) {
            if (onComplete != null) {
                runOnUiThread(onComplete);
            }
            return;
        }
        
        cameraExecutor.execute(() -> {
            // 检查surface是否仍然活动，以避免在废弃的surface上操作
            if (!isSurfaceActive) {
                runOnUiThread(() -> {
                    if (onComplete != null) {
                        onComplete.run();
                    }
                });
                return;
            }
            
            try {
                // 这里简化处理，实际应该加载图片并添加辅助线
                // 辅助线将根据区域类型在不同位置绘制
                // 确保回调在UI线程执行
                runOnUiThread(() -> {
                    if (onComplete != null && isSurfaceActive) { // 再次检查，避免在执行期间surface被销毁
                        onComplete.run();
                    }
                });
            } catch (Exception e) {
                if (isSurfaceActive) { // 只有在surface活动时才显示错误
                    runOnUiThread(() -> Toast.makeText(CameraActivity.this, "添加辅助线失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
                // 即使失败也应该执行回调，确保后续流程继续
                runOnUiThread(() -> {
                    if (onComplete != null && isSurfaceActive) {
                        onComplete.run();
                    }
                });
            }
        });
    }

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