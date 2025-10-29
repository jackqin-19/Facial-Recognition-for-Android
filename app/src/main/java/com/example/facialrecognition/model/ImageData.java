package com.example.facialrecognition.model;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class ImageData implements Parcelable {
    public enum AreaType {
        FRONT, MIDDLE, BACK
    }

    private String imagePath;
    private Bitmap bitmap;
    private Bitmap thumbnail;
    private AreaType areaType;
    private List<Person> detectedPersons;
    private int recognizedCount; // 系统识别的人数
    private int manuallyAddedCount; // 手动添加的人数
    private int manuallyDeletedCount; // 手动删除的人数
    private String clearOperationType; // 执行过的清空操作类型

    public ImageData(String imagePath, Bitmap bitmap, AreaType areaType) {
        // 对于模拟数据，使用空字符串或检查路径是否存在
        this.imagePath = imagePath;
        this.bitmap = bitmap;
        // 生成缩略图
        if (bitmap != null) {
            this.thumbnail = Bitmap.createScaledBitmap(bitmap, 200, 150, true);
        }
        this.areaType = areaType;
        this.detectedPersons = new ArrayList<>();
        this.recognizedCount = 0;
        this.manuallyAddedCount = 0;
        this.manuallyDeletedCount = 0;
        this.clearOperationType = "未执行清空操作";
    }

    protected ImageData(Parcel in) {
        try {
            imagePath = in.readString();
            // 从Parcel读取时，bitmap会是null
            // 在需要时通过imagePath重新加载
            bitmap = null;
            thumbnail = in.readParcelable(Bitmap.class.getClassLoader());
            
            // 安全地读取枚举值，添加边界检查
            int areaTypeOrdinal = in.readInt();
            if (areaTypeOrdinal >= 0 && areaTypeOrdinal < AreaType.values().length) {
                areaType = AreaType.values()[areaTypeOrdinal];
            } else {
                // 当枚举值无效时，使用默认值
                areaType = AreaType.FRONT;
            }
            
            // 确保detectedPersons不为null
            detectedPersons = in.createTypedArrayList(Person.CREATOR);
            if (detectedPersons == null) {
                detectedPersons = new ArrayList<>();
            }
            
            recognizedCount = in.readInt();
            manuallyAddedCount = in.readInt();
            manuallyDeletedCount = in.readInt();
            
            // 确保clearOperationType不为null
            clearOperationType = in.readString();
            if (clearOperationType == null) {
                clearOperationType = "未执行清空操作";
            }
        } catch (Exception e) {
            // 捕获所有异常，确保应用不会崩溃
            // 初始化默认值
            imagePath = "";
            bitmap = null;
            thumbnail = null;
            areaType = AreaType.FRONT;
            detectedPersons = new ArrayList<>();
            recognizedCount = 0;
            manuallyAddedCount = 0;
            manuallyDeletedCount = 0;
            clearOperationType = "未执行清空操作";
        }
    }

    public static final Creator<ImageData> CREATOR = new Creator<ImageData>() {
        @Override
        public ImageData createFromParcel(Parcel in) {
            try {
                return new ImageData(in);
            } catch (Exception e) {
                // 捕获所有异常，返回一个空的实例而不是导致应用崩溃
                return new ImageData("", null, AreaType.FRONT);
            }
        }

        @Override
        public ImageData[] newArray(int size) {
            return new ImageData[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(imagePath);
        // 不传递完整的Bitmap，只传递图片路径
        // 避免TransactionTooLargeException异常
        parcel.writeParcelable(null, i); // bitmap设为null
        parcel.writeParcelable(thumbnail, i); // 仍然传递缩略图，它很小
        parcel.writeInt(areaType.ordinal());
        parcel.writeTypedList(detectedPersons);
        parcel.writeInt(recognizedCount);
        parcel.writeInt(manuallyAddedCount);
        parcel.writeInt(manuallyDeletedCount);
        parcel.writeString(clearOperationType);
    }

    // Getters and setters
    public String getImagePath() {
        return imagePath;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public Bitmap getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(Bitmap thumbnail) {
        this.thumbnail = thumbnail;
    }

    public AreaType getAreaType() {
        return areaType;
    }

    public List<Person> getDetectedPersons() {
        return detectedPersons;
    }

    public void setDetectedPersons(List<Person> detectedPersons) {
        this.detectedPersons = detectedPersons;
    }

    public int getRecognizedCount() {
        return recognizedCount;
    }

    public void setRecognizedCount(int recognizedCount) {
        this.recognizedCount = recognizedCount;
    }

    public int getManuallyAddedCount() {
        return manuallyAddedCount;
    }

    public void setManuallyAddedCount(int manuallyAddedCount) {
        this.manuallyAddedCount = manuallyAddedCount;
    }

    public int getManuallyDeletedCount() {
        return manuallyDeletedCount;
    }

    public void setManuallyDeletedCount(int manuallyDeletedCount) {
        this.manuallyDeletedCount = manuallyDeletedCount;
    }

    public String getClearOperationType() {
        return clearOperationType;
    }

    public void setClearOperationType(String clearOperationType) {
        this.clearOperationType = clearOperationType;
    }

    // 获取当前实际显示的人数
    public int getTotalCount() {
        // 添加detectedPersons列表的空值检查
        if (detectedPersons == null) {
            return 0;
        }
        
        int count = 0;
        for (Person person : detectedPersons) {
            // 添加Person对象的空值检查
            if (person != null && !person.isMarkedAsDeleted()) {
                count++;
            }
        }
        return count;
    }

    // 获取不确定人数
    public int getUncertainCount() {
        // 添加detectedPersons列表的空值检查
        if (detectedPersons == null) {
            return 0;
        }
        
        int count = 0;
        for (Person person : detectedPersons) {
            // 添加Person对象的空值检查
            if (person != null && person.isUncertain() && !person.isMarkedAsDeleted()) {
                count++;
            }
        }
        return count;
    }
}