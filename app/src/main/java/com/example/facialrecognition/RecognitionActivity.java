package com.example.facialrecognition;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import com.example.facialrecognition.FaceDetectorManager;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.example.facialrecognition.model.ImageData;
import com.example.facialrecognition.model.Person;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Matrix;

public class RecognitionActivity extends AppCompatActivity {

    private CustomImageView imageView;
    private TextView tvPersonCount;
    private ImageButton btnUndo, btnRedo;
    private Button btnClearManual;
    private TextView tvLowConfidenceWarning;
    private ImageData imageData;
    private Bitmap originalBitmap;
    private Bitmap markedBitmap;
    private Matrix imageMatrix = new Matrix();
    private FaceDetectorManager faceDetectorManager;
    private List<Operation> operationHistory;
    private int currentOperationIndex;
    private boolean isFirstDeleteSystem = true;
    private float scale = 1.0f;
    private float lastScaleFactor = 1.0f;
    private float translateX = 0f;
    private float translateY = 0f;
    private float lastTouchX = 0f;
    private float lastTouchY = 0f;
    private int mode = 0; // 0: 无操作, 1: 单指拖动, 2: 双指缩放
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;

    private enum OperationType {
        ADD_MANUAL, DELETE_MANUAL, DELETE_SYSTEM, RESTORE_SYSTEM, CONFIRM_LOW_CONFIDENCE
    }

    private class Operation {
        OperationType type;
        Person person;

        public Operation(OperationType type, Person person) {
            this.type = type;
            this.person = person;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognition);

        // 初始化视图
        imageView = findViewById(R.id.image_view);
        imageView.setScaleType(ImageView.ScaleType.MATRIX); // 设置为矩阵缩放类型，这是使用矩阵变换的关键
        tvPersonCount = findViewById(R.id.tv_person_count);
        btnUndo = findViewById(R.id.btn_undo);
        btnRedo = findViewById(R.id.btn_redo);
        btnClearManual = findViewById(R.id.btn_clear_manual);
        tvLowConfidenceWarning = findViewById(R.id.tv_low_confidence_warning);
        ImageButton btnBack = findViewById(R.id.btn_back);
        Button btnNext = findViewById(R.id.btn_next);

        // 初始化操作历史
        operationHistory = new ArrayList<>();
        currentOperationIndex = -1;
        
        // 初始化人脸检测器管理器
        faceDetectorManager = new FaceDetectorManager(this);

        // 获取传递的图片路径和区域类型
        String imagePath = getIntent().getStringExtra("IMAGE_PATH");
        int areaTypeOrdinal = getIntent().getIntExtra("AREA_TYPE", 1);
        ImageData.AreaType areaType = ImageData.AreaType.values()[areaTypeOrdinal];

        // 加载图片
        loadImage(imagePath, areaType);

        // 设置手势检测器
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
        gestureDetector = new GestureDetector(this, new GestureListener());

        // 设置触摸监听器 - 实现缩放、拖动和点击功能
        imageView.setOnTouchListener((v, event) -> {
            // 先处理缩放手势检测器
            boolean scaleResult = scaleGestureDetector.onTouchEvent(event);
            
            // 再处理手势检测器
            boolean gestureResult = gestureDetector.onTouchEvent(event);
            
            // 处理基础触摸事件
            int action = event.getAction() & MotionEvent.ACTION_MASK;
            
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    // 单指按下，记录初始位置
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    mode = 1; // 单指拖动模式
                    break;
                    
                case MotionEvent.ACTION_MOVE:
                    // 只有在单指模式且不是缩放过程中才处理拖动
                    if (mode == 1 && !scaleGestureDetector.isInProgress()) {
                        float dx = event.getX() - lastTouchX;
                        float dy = event.getY() - lastTouchY;
                        
                        // 小位移也处理，提高灵敏度
                        if (Math.abs(dx) > 0.5f || Math.abs(dy) > 0.5f) {
                            translateX += dx;
                            translateY += dy;
                            applyMatrix();
                            
                            lastTouchX = event.getX();
                            lastTouchY = event.getY();
                        }
                    }
                    break;
                    
                case MotionEvent.ACTION_UP:
                    // 手指抬起，结束当前操作模式
                    mode = 0;
                    // 确保对于点击事件调用performClick
                    v.performClick();
                    break;
                    
                case MotionEvent.ACTION_CANCEL:
                    // 取消操作
                    mode = 0;
                    break;
                    
                case MotionEvent.ACTION_POINTER_DOWN:
                    // 第二根手指按下，进入多手指模式
                    mode = 2; // 多指模式
                    break;
                    
                case MotionEvent.ACTION_POINTER_UP:
                    // 一根手指抬起，如果还有一根手指，切换到单指模式
                    if (event.getPointerCount() == 2) {
                        mode = 1;
                        // 重置触摸点，避免突然跳动
                        lastTouchX = event.getX(event.getPointerCount() - 1);
                        lastTouchY = event.getY(event.getPointerCount() - 1);
                    } else if (event.getPointerCount() == 1) {
                        mode = 0;
                    }
                    break;
            }
            
            // 返回true表示消费了事件
            return scaleResult || gestureResult || true;
        });

        // 设置按钮监听器
        btnUndo.setOnClickListener(v -> undo());
        btnRedo.setOnClickListener(v -> redo());
        btnClearManual.setOnClickListener(v -> showClearOptions());
        btnBack.setOnClickListener(v -> finish());
        btnNext.setOnClickListener(v -> goToNext());

        // 开始识别
        startRecognition();
    }

    private void loadImage(String imagePath, ImageData.AreaType areaType) {
        originalBitmap = BitmapFactory.decodeFile(imagePath);
        if (originalBitmap == null) {
            Toast.makeText(this, "图片加载失败", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 初始化ImageData
        imageData = new ImageData(imagePath, originalBitmap, areaType);
        markedBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        imageView.setImageBitmap(markedBitmap);
        
        // 重新应用缩放和位移
        applyMatrix();
    }

    private void startRecognition() {
        // 显示加载提示
        Toast.makeText(this, "正在进行人脸检测...", Toast.LENGTH_SHORT).show();

        // 使用Google ML Kit进行真实的人脸检测
        performRecognition();
    }

    private void performRecognition() {
        // 使用FaceDetectorManager进行真实的人脸检测
        if (faceDetectorManager != null && originalBitmap != null) {
            faceDetectorManager.detectFaces(originalBitmap, new FaceDetectorManager.DetectionCallback() {
                @Override
                public void onDetectionCompleted(List<Person> detectedPersons) {
                    // 设置检测到的人脸数据
                    imageData.setDetectedPersons(detectedPersons);
                    imageData.setRecognizedCount(detectedPersons.size());
                    
                    // 更新UI显示
                    runOnUiThread(() -> {
                        updateMarkedImage();
                        updatePersonCount();
                        updateLowConfidenceWarning();
                        
                        // 显示检测完成提示
                        String message = "成功检测到 " + detectedPersons.size() + " 个人脸";
                        Toast.makeText(RecognitionActivity.this, message, Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(Exception e) {
                    // 处理错误情况
                    runOnUiThread(() -> {
                        Toast.makeText(RecognitionActivity.this, "人脸检测失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("FaceDetection", "Error detecting faces: ", e);
                        
                        // 如果检测失败，生成少量模拟数据以便继续使用应用
                        generateFallbackData();
                    });
                }
            });
        } else {
            // 如果没有初始化，使用回退方案
            generateFallbackData();
        }
    }
    
    // 当人脸检测失败时使用的回退数据生成方法
    private void generateFallbackData() {
        List<Person> fallbackPersons = new ArrayList<>();
        fallbackPersons.add(new Person(1, new PointF(originalBitmap.getWidth() / 2f, originalBitmap.getHeight() / 2f)));
        imageData.setDetectedPersons(fallbackPersons);
        imageData.setRecognizedCount(1);
        updateMarkedImage();
        updatePersonCount();
        updateLowConfidenceWarning();
    }

    private void applyMatrix() {
        // 创建变换矩阵
        Matrix matrix = new Matrix();
        
        // 计算初始缩放比例（适应视图）
        float initScaleX = (float) imageView.getWidth() / originalBitmap.getWidth();
        float initScaleY = (float) imageView.getHeight() / originalBitmap.getHeight();
        float initScale = Math.min(initScaleX, initScaleY);
        
        // 应用初始缩放和居中
        matrix.postScale(initScale, initScale);
        matrix.postTranslate(
            (imageView.getWidth() - originalBitmap.getWidth() * initScale) / 2f,
            (imageView.getHeight() - originalBitmap.getHeight() * initScale) / 2f
        );
        
        // 应用用户手势变换 - 修改这里
        // 使用视图中心作为缩放中心点
        float centerX = imageView.getWidth() / 2f;
        float centerY = imageView.getHeight() / 2f;
        matrix.postScale(scale, scale, centerX, centerY);
        
        // 应用平移变换
        matrix.postTranslate(translateX, translateY);
        
        // 设置矩阵到ImageView
        imageView.setImageMatrix(matrix);
        imageView.invalidate(); // 强制重绘
    }
    
    private void updateMarkedImage() {
        // 创建新的markedBitmap
        markedBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(markedBitmap);

        Paint systemPaint = new Paint();
        systemPaint.setColor(Color.RED);
        systemPaint.setStyle(Paint.Style.STROKE);
        systemPaint.setStrokeWidth(3);
        systemPaint.setAlpha(128);

        Paint manualPaint = new Paint();
        manualPaint.setColor(Color.BLUE);
        manualPaint.setStyle(Paint.Style.STROKE);
        manualPaint.setStrokeWidth(3);
        manualPaint.setAlpha(128);

        Paint lowConfidencePaint = new Paint();
        lowConfidencePaint.setColor(Color.GRAY);
        lowConfidencePaint.setStyle(Paint.Style.STROKE);
        lowConfidencePaint.setStrokeWidth(3);
        lowConfidencePaint.setAlpha(128);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(50); // 增大文本大小以匹配更大的标记圈
        textPaint.setAntiAlias(true);

        for (Person person : imageData.getDetectedPersons()) {
            if (person.isMarkedAsDeleted()) continue;

            float radius = 50; // 增大标记圈大小
            float x = person.getPosition().x;
            float y = person.getPosition().y;
            
            // 添加调试输出
            Log.d("MarkPosition", "Mark at (" + x + ", " + y + ")");

            if (person.isManual()) {
                canvas.drawCircle(x, y, radius, manualPaint);
            } else if (person.isUncertain()) {
                canvas.drawCircle(x, y, radius, lowConfidencePaint);
                // 绘制问号
                textPaint.setColor(Color.GRAY);
                canvas.drawText("?", x - 8, y + 8, textPaint);
            } else {
                canvas.drawCircle(x, y, radius, systemPaint);
            }

            // 绘制序号
            textPaint.setColor(Color.WHITE);
            canvas.drawText(String.valueOf(person.getId()), x - 10, y + 10, textPaint);
        }

        imageView.setImageBitmap(markedBitmap);
        
        // 重新应用变换矩阵
        applyMatrix();
    }

    private void updatePersonCount() {
        int recognizedCount = imageData.getRecognizedCount();
        int manuallyAddedCount = imageData.getManuallyAddedCount();
        int manuallyDeletedCount = imageData.getManuallyDeletedCount();
        int totalCount = recognizedCount + manuallyAddedCount - manuallyDeletedCount;

        tvPersonCount.setText("当前人数：" + totalCount + " 人（系统识别 " + recognizedCount + " 人 + 手动补标 " + 
                manuallyAddedCount + " 人 - 手动删除 " + manuallyDeletedCount + " 人）");
    }

    private void updateLowConfidenceWarning() {
        int uncertainCount = 0;
        for (Person person : imageData.getDetectedPersons()) {
            if (person.isUncertain() && !person.isMarkedAsDeleted()) {
                uncertainCount++;
            }
        }
        
        if (uncertainCount > 0) {
            tvLowConfidenceWarning.setText("发现 " + uncertainCount + " 个疑似目标，请手动确认");
            tvLowConfidenceWarning.setVisibility(View.VISIBLE);
        } else {
            tvLowConfidenceWarning.setVisibility(View.GONE);
        }
    }

    private void showClearOptions() {
        PopupMenu popupMenu = new PopupMenu(this, btnClearManual);
        popupMenu.getMenuInflater().inflate(R.menu.clear_options_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_delete_manual) {
                clearManualMarks();
            } else if (itemId == R.id.menu_restore_system) {
                restoreSystemMarks();
            } else if (itemId == R.id.menu_full_reset) {
                fullReset();
            }
            return true;
        });

        popupMenu.show();
    }

    private void clearManualMarks() {
        // 删除所有手动标记
        int countBefore = imageData.getTotalCount();
        List<Person> toRemove = new ArrayList<>();
        for (Person person : imageData.getDetectedPersons()) {
            if (person.isManual()) {
                toRemove.add(person);
            }
        }
        imageData.getDetectedPersons().removeAll(toRemove);
        imageData.setManuallyAddedCount(0);
        
        // 记录操作
        addToHistory(null);
        
        updateMarkedImage();
        updatePersonCount();
        Toast.makeText(this, "已删除所有手动标记", Toast.LENGTH_SHORT).show();
    }

    private void restoreSystemMarks() {
        // 恢复所有被删除的系统标记
        for (Person person : imageData.getDetectedPersons()) {
            if (!person.isManual() && person.isMarkedAsDeleted()) {
                person.setMarkedAsDeleted(false);
                imageData.setManuallyDeletedCount(imageData.getManuallyDeletedCount() - 1);
            }
        }
        
        // 记录操作
        addToHistory(null);
        
        updateMarkedImage();
        updatePersonCount();
        updateLowConfidenceWarning();
        Toast.makeText(this, "已恢复所有系统标记", Toast.LENGTH_SHORT).show();
    }

    private void fullReset() {
        // 删除所有手动标记并恢复系统标记
        List<Person> toRemove = new ArrayList<>();
        for (Person person : imageData.getDetectedPersons()) {
            if (person.isManual()) {
                toRemove.add(person);
            } else if (person.isMarkedAsDeleted()) {
                person.setMarkedAsDeleted(false);
            }
        }
        imageData.getDetectedPersons().removeAll(toRemove);
        imageData.setManuallyAddedCount(0);
        imageData.setManuallyDeletedCount(0);
        
        // 记录操作
        addToHistory(null);
        
        updateMarkedImage();
        updatePersonCount();
        updateLowConfidenceWarning();
        Toast.makeText(this, "已重置到初始识别状态", Toast.LENGTH_SHORT).show();
    }

    private void addToHistory(Operation operation) {
        // 移除当前索引之后的所有操作
        while (operationHistory.size() > currentOperationIndex + 1) {
            operationHistory.remove(operationHistory.size() - 1);
        }
        
        // 添加新操作
        operationHistory.add(operation);
        currentOperationIndex = operationHistory.size() - 1;
        
        // 更新按钮状态
        updateUndoRedoButtons();
    }

    private void updateUndoRedoButtons() {
        btnUndo.setEnabled(currentOperationIndex >= 0);
        btnRedo.setEnabled(currentOperationIndex < operationHistory.size() - 1);
    }

    private void undo() {
        if (currentOperationIndex >= 0) {
            Operation operation = operationHistory.get(currentOperationIndex);
            // 执行撤销操作
            currentOperationIndex--;
            updateUndoRedoButtons();
            updateMarkedImage();
            updatePersonCount();
            updateLowConfidenceWarning();
        }
    }

    private void redo() {
        if (currentOperationIndex < operationHistory.size() - 1) {
            currentOperationIndex++;
            Operation operation = operationHistory.get(currentOperationIndex);
            // 执行重做操作
            updateUndoRedoButtons();
            updateMarkedImage();
            updatePersonCount();
            updateLowConfidenceWarning();
        }
    }

    private void goToNext() {
        try {
            Log.d("Navigation", "开始跳转到MergeResultActivity");
            
            // 检查imageData是否有效
            if (imageData == null) {
                Log.e("Navigation", "imageData为空，无法跳转");
                Toast.makeText(this, "数据错误，无法跳转", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 修复：安全检查detectedPersons列表，移除null元素
            if (imageData.getDetectedPersons() != null) {
                List<Person> cleanPersons = new ArrayList<>();
                for (Person p : imageData.getDetectedPersons()) {
                    if (p != null) {
                        cleanPersons.add(p);
                    }
                }
                imageData.setDetectedPersons(cleanPersons);
            }
            
            // 确保clearOperationType不为null或特殊值
            if (imageData.getClearOperationType() == null || imageData.getClearOperationType().contains("未执行")) {
                imageData.setClearOperationType("default");
            }
            
            Log.d("Navigation", "imageData有效，人数统计: " + imageData.getTotalCount());
            
            // 跳转到合并统计页面
            Intent intent = new Intent(this, MergeResultActivity.class);
            Log.d("Navigation", "创建Intent，目标: MergeResultActivity");
            
            // 使用Bundle来封装数据，增加一层隔离
            Bundle bundle = new Bundle();
            bundle.putParcelable("IMAGE_DATA", imageData);
            intent.putExtras(bundle);
            
            Log.d("Navigation", "通过Bundle添加IMAGE_DATA到Intent");
            
            startActivity(intent);
            Log.d("Navigation", "执行startActivity，跳转完成");
            
        } catch (Exception e) {
            Log.e("Navigation", "跳转到MergeResultActivity失败: " + e.getMessage(), e);
            Toast.makeText(this, "跳转失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            // 使用增量缩放计算新的缩放比例
            float scaleFactor = detector.getScaleFactor();
            float newScale = scale * scaleFactor;
            
            // 限制缩放范围，确保可以缩小（0.5倍）到放大（4倍）
            newScale = Math.max(0.5f, Math.min(newScale, 4.0f));
            
            // 只有当缩放比例发生足够变化时才应用，避免频繁重绘
            if (Math.abs(newScale - scale) > 0.01f) {
                scale = newScale;
                applyMatrix();
            }
            
            return true;
        }
        
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            // 必须返回true才能启动缩放手势，这对于实现缩小功能至关重要
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public void onLongPress(MotionEvent e) {
            // 长按处理 - 转换触摸坐标到原始图像坐标系
            float[] originalCoords = convertToOriginalCoordinates(e.getX(), e.getY());
            
            // 精确确保坐标在有效范围内
            originalCoords[0] = Math.max(0.0f, Math.min(originalCoords[0], (float)originalBitmap.getWidth()));
            originalCoords[1] = Math.max(0.0f, Math.min(originalCoords[1], (float)originalBitmap.getHeight()));
            
            // 只有当触摸点确实在图像范围内时才处理长按事件
            handleLongPress(originalCoords[0], originalCoords[1]);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            // 双击重置缩放和位置
            scale = 1.0f;
            translateX = 0f;
            translateY = 0f;
            applyMatrix();
            return true;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 释放人脸检测器资源
        if (faceDetectorManager != null) {
            faceDetectorManager.close();
        }
    }
    
    private float[] convertToOriginalCoordinates(float touchX, float touchY) {
        if (originalBitmap == null || imageView == null) {
            return new float[]{0, 0};
        }
        
        // 获取ImageView的变换矩阵
        Matrix imageMatrix = imageView.getImageMatrix();
        
        // 创建逆矩阵用于反向变换
        Matrix inverse = new Matrix();
        if (!imageMatrix.invert(inverse)) {
            // 如果无法反转矩阵，返回默认值
            return new float[]{0, 0};
        }
        
        // 应用逆矩阵转换触摸坐标
        float[] point = new float[]{touchX, touchY};
        inverse.mapPoints(point);
        
        // 确保坐标在有效范围内
        point[0] = Math.max(0, Math.min(point[0], originalBitmap.getWidth()));
        point[1] = Math.max(0, Math.min(point[1], originalBitmap.getHeight()));
        
        return point;
    }
    
    private void handleLongPress(float x, float y) {
        // 检查坐标有效性
        if (x < 0 || y < 0 || x > originalBitmap.getWidth() || y > originalBitmap.getHeight()) {
            // 坐标无效，不执行任何操作
            return;
        }
        
        // 检查是否点击了现有的标记
        Person clickedPerson = findPersonAtPosition(x, y);
        if (clickedPerson != null) {
            showPersonOptions(clickedPerson);
        } else {
            // 未点击现有标记，添加新的手动标记
            // 限制标记位置在图像边界内，留出20像素边距
            x = Math.max(20, Math.min(x, originalBitmap.getWidth() - 20));
            y = Math.max(20, Math.min(y, originalBitmap.getHeight() - 20));
            addManualMark(x, y);
        }
    }

    private Person findPersonAtPosition(float x, float y) {
        for (Person person : imageData.getDetectedPersons()) {
            if (person.isMarkedAsDeleted()) continue;
            
            float dx = x - person.getPosition().x;
            float dy = y - person.getPosition().y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            if (distance < 30) { // 固定点击区域范围，不受缩放影响
                return person;
            }
        }
        return null;
    }

    private void showPersonOptions(Person person) {
        PopupMenu popupMenu = new PopupMenu(this, imageView);
        
        if (person.isManual()) {
            // 手动标记：删除选项
            popupMenu.getMenu().add(0, 1, 0, "删除手动标记");
        } else if (person.isUncertain()) {
            // 低置信度标记：确认或删除选项
            popupMenu.getMenu().add(0, 2, 0, "确认为系统标记");
            popupMenu.getMenu().add(0, 3, 0, "删除此标记");
        } else {
            // 系统标记：删除选项（带二次确认）
            popupMenu.getMenu().add(0, 4, 0, "删除系统标记");
        }
        
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == 1) {
                deleteManualMark(person);
            } else if (itemId == 2) {
                confirmLowConfidence(person);
            } else if (itemId == 3) {
                deleteLowConfidenceMark(person);
            } else if (itemId == 4) {
                deleteSystemMark(person);
            }
            return true;
        });
        
        popupMenu.show();
    }

    private void addManualMark(float x, float y) {
        int newId = imageData.getDetectedPersons().size() + 1;
        Person newPerson = new Person(newId, new PointF(x, y));
        imageData.getDetectedPersons().add(newPerson);
        imageData.setManuallyAddedCount(imageData.getManuallyAddedCount() + 1);
        
        addToHistory(new Operation(OperationType.ADD_MANUAL, newPerson));
        updateMarkedImage();
        updatePersonCount();
    }

    private void deleteManualMark(Person person) {
        imageData.getDetectedPersons().remove(person);
        imageData.setManuallyAddedCount(imageData.getManuallyAddedCount() - 1);
        
        addToHistory(new Operation(OperationType.DELETE_MANUAL, person));
        updateMarkedImage();
        updatePersonCount();
    }

    private void deleteSystemMark(Person person) {
        if (isFirstDeleteSystem) {
            Toast.makeText(this, "删除后可通过'仅恢复系统标记'找回，无需重新识别", Toast.LENGTH_LONG).show();
            isFirstDeleteSystem = false;
        }
        
        person.setMarkedAsDeleted(true);
        imageData.setManuallyDeletedCount(imageData.getManuallyDeletedCount() + 1);
        
        addToHistory(new Operation(OperationType.DELETE_SYSTEM, person));
        updateMarkedImage();
        updatePersonCount();
    }

    private void confirmLowConfidence(Person person) {
        person.setUncertain(false);
        
        addToHistory(new Operation(OperationType.CONFIRM_LOW_CONFIDENCE, person));
        updateMarkedImage();
        updatePersonCount();
        updateLowConfidenceWarning();
    }

    private void deleteLowConfidenceMark(Person person) {
        person.setMarkedAsDeleted(true);
        imageData.setManuallyDeletedCount(imageData.getManuallyDeletedCount() + 1);
        
        addToHistory(new Operation(OperationType.DELETE_SYSTEM, person));
        updateMarkedImage();
        updatePersonCount();
        updateLowConfidenceWarning();
    }

}