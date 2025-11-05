package com.example.facialrecognition;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Looper;

import com.example.facialrecognition.model.Person;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * 人脸检测管理器，使用Google ML Kit的人脸检测API实现人脸检测功能
 */
public class FaceDetectorManager {

    private final FaceDetector detector;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface DetectionCallback {
        void onDetectionCompleted(List<Person> detectedPersons);
        void onError(Exception e);
    }

    public FaceDetectorManager(Context context) {
        // 配置人脸检测器选项
        // 使用高精度模式，启用人脸关键点检测
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();

        // 创建人脸检测器实例
        detector = FaceDetection.getClient(options);
    }

    /**
     * 检测图像中的人脸
     * @param bitmap 要检测的图像
     * @param callback 检测完成的回调
     */
    public void detectFaces(Bitmap bitmap, final DetectionCallback callback) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        
        // 提取成功处理逻辑
        detector.process(image)
                .addOnSuccessListener(faces -> handleDetectionSuccess(faces, bitmap, callback))
                .addOnFailureListener(e -> handleDetectionFailure(e, callback));
    }
    
    // 处理检测成功的情况
    private void handleDetectionSuccess(List<Face> faces, Bitmap bitmap, DetectionCallback callback) {
        mainHandler.post(() -> {
            List<Person> detectedPersons = convertFacesToPersons(faces, bitmap.getWidth(), bitmap.getHeight());
            callback.onDetectionCompleted(detectedPersons);
        });
    }
    
    // 处理检测失败的情况
    private void handleDetectionFailure(Exception e, DetectionCallback callback) {
        mainHandler.post(() -> callback.onError(e));
    }

    /**
     * 将ML Kit的Face对象转换为我们应用的Person对象
     */
    private List<Person> convertFacesToPersons(List<Face> faces, int imageWidth, int imageHeight) {
        List<Person> persons = new ArrayList<>(faces.size());
        
        // 为每个人脸分配一个ID并创建Person对象
        for (int i = 0; i < faces.size(); i++) {
            Face face = faces.get(i);
            int id = i + 1;
            // 直接在构造函数中使用坐标，避免创建中间PointF对象
            PointF facePosition = new PointF(face.getBoundingBox().centerX(), face.getBoundingBox().centerY());
            
            // 创建Person对象并设置确定性
            Person person = new Person(id, facePosition, 0.9f);
            person.setUncertain(false);
            
            persons.add(person);
        }
        
        return persons;
    }

    /**
     * 释放资源
     */
    public void close() {
        detector.close();
    }
}