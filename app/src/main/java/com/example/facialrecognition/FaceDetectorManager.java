package com.example.facialrecognition;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Looper;

import com.example.facialrecognition.model.ImageData;
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
    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface DetectionCallback {
        void onDetectionCompleted(List<Person> detectedPersons);
        void onError(Exception e);
    }

    public FaceDetectorManager(Context context) {
        this.context = context;

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

        detector.process(image)
                .addOnSuccessListener(faces -> {
                    // 在UI线程上返回检测结果
                    mainHandler.post(() -> {
                        List<Person> detectedPersons = convertFacesToPersons(faces, bitmap.getWidth(), bitmap.getHeight());
                        callback.onDetectionCompleted(detectedPersons);
                    });
                })
                .addOnFailureListener(e -> {
                    // 在UI线程上返回错误
                    mainHandler.post(() -> {
                        callback.onError(e);
                    });
                });
    }

    /**
     * 将ML Kit的Face对象转换为我们应用的Person对象
     */
    private List<Person> convertFacesToPersons(List<Face> faces, int imageWidth, int imageHeight) {
        List<Person> persons = new ArrayList<>();

        // 为每个人脸分配一个ID
        int id = 1;
        for (Face face : faces) {
            // 获取人脸中心坐标
            PointF facePosition = new PointF();
            facePosition.x = face.getBoundingBox().centerX();
            facePosition.y = face.getBoundingBox().centerY();

            // 创建Person对象
            Person person = new Person(id, facePosition);
            person.setUncertain(false); // 默认设置为确定的检测结果

            // 可以根据人脸置信度来设置uncertain状态
            // 但ML Kit的API没有直接提供置信度值，可以根据其他特征判断

            persons.add(person);
            id++;
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