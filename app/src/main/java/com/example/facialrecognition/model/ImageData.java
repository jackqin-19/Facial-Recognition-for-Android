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
        imagePath = in.readString();
        bitmap = in.readParcelable(Bitmap.class.getClassLoader());
        thumbnail = in.readParcelable(Bitmap.class.getClassLoader());
        areaType = AreaType.values()[in.readInt()];
        detectedPersons = in.createTypedArrayList(Person.CREATOR);
        recognizedCount = in.readInt();
        manuallyAddedCount = in.readInt();
        manuallyDeletedCount = in.readInt();
        clearOperationType = in.readString();
    }

    public static final Creator<ImageData> CREATOR = new Creator<ImageData>() {
        @Override
        public ImageData createFromParcel(Parcel in) {
            return new ImageData(in);
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
        parcel.writeParcelable(bitmap, i);
        parcel.writeParcelable(thumbnail, i);
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
        int count = 0;
        for (Person person : detectedPersons) {
            if (!person.isMarkedAsDeleted()) {
                count++;
            }
        }
        return count;
    }

    // 获取不确定人数
    public int getUncertainCount() {
        int count = 0;
        for (Person person : detectedPersons) {
            if (person.isUncertain() && !person.isMarkedAsDeleted()) {
                count++;
            }
        }
        return count;
    }
}