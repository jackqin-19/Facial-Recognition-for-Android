package com.example.facialrecognition.model;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CountRecord implements Parcelable {
    private String recordId;
    private long timestamp;
    private List<ImageData> imageDataList;
    private int totalPersonCount;
    private int overlappingPersonCount; // 重叠人数
    private boolean isRecycleBin = false; // 是否在回收站
    private long deletedTimestamp;
    private Bitmap thumbnail;

    public CountRecord() {
        this.recordId = "REC" + System.currentTimeMillis();
        this.timestamp = System.currentTimeMillis();
        this.imageDataList = new ArrayList<>();
        this.totalPersonCount = 0;
        this.overlappingPersonCount = 0;
        this.isRecycleBin = false;
        this.deletedTimestamp = 0;
    }

    public CountRecord(List<ImageData> imageDataList, int totalCount) {
        this.recordId = "REC" + System.currentTimeMillis();
        this.timestamp = System.currentTimeMillis();
        this.imageDataList = imageDataList;
        this.totalPersonCount = totalCount;
        this.isRecycleBin = false;
        
        // 从第一个图片生成缩略图
        if (imageDataList != null && !imageDataList.isEmpty() && imageDataList.get(0).getThumbnail() != null) {
            this.thumbnail = imageDataList.get(0).getThumbnail();
        }
    }

    // 创建模拟记录的静态方法
    public static CountRecord createMockRecord(int index) {
        List<ImageData> mockImageDataList = new ArrayList<>();
        
        // 创建模拟的ImageData - 不再使用区域类型，只创建一个图片数据
        int personCount = 20 + index * 5;
        List<Person> mockPersons = new ArrayList<>();
        for (int i = 0; i < personCount; i++) {
            mockPersons.add(new Person(i + 1, null, 0.8f));
        }
        
        // 使用空字符串避免文件访问错误，因为模拟数据中的图片文件并不存在
        String mockImagePath = "";
        ImageData mockImageData = new ImageData(mockImagePath, null);
        mockImageData.setDetectedPersons(mockPersons);
        mockImageDataList.add(mockImageData);
        
        // 创建并返回模拟记录
        CountRecord record = new CountRecord(mockImageDataList, 80 + index * 2);
        // 设置不同的时间戳
        record.timestamp = System.currentTimeMillis() - (long)(index * 86400000 * (0.5 + Math.random()));
        return record;
    }

    protected CountRecord(Parcel in) {
        recordId = in.readString();
        timestamp = in.readLong();
        imageDataList = in.createTypedArrayList(ImageData.CREATOR);
        totalPersonCount = in.readInt();
        overlappingPersonCount = in.readInt();
        isRecycleBin = in.readByte() != 0;
        deletedTimestamp = in.readLong();
        thumbnail = in.readParcelable(Bitmap.class.getClassLoader());
    }

    public static final Creator<CountRecord> CREATOR = new Creator<CountRecord>() {
        @Override
        public CountRecord createFromParcel(Parcel in) {
            return new CountRecord(in);
        }

        @Override
        public CountRecord[] newArray(int size) {
            return new CountRecord[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(recordId);
        parcel.writeLong(timestamp);
        parcel.writeTypedList(imageDataList);
        parcel.writeInt(totalPersonCount);
        parcel.writeInt(overlappingPersonCount);
        parcel.writeByte((byte) (isRecycleBin ? 1 : 0));
        parcel.writeLong(deletedTimestamp);
        parcel.writeParcelable(thumbnail, i);
    }

    // 移至回收站
    public void moveToTrash() {
        this.isRecycleBin = true;
        this.deletedTimestamp = System.currentTimeMillis();
    }

    // 从回收站恢复
    public void restoreFromTrash() {
        this.isRecycleBin = false;
        this.deletedTimestamp = 0;
    }

    // 判断是否已在回收站超过7天
    public boolean isOver7DaysInTrash() {
        if (!isRecycleBin) return false;
        long sevenDaysInMillis = 7 * 24 * 60 * 60 * 1000;
        return System.currentTimeMillis() - deletedTimestamp > sevenDaysInMillis;
    }

    // Getters and setters
    public String getRecordId() {
        return recordId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public List<ImageData> getImageDataList() {
        return imageDataList;
    }

    public void setImageDataList(List<ImageData> imageDataList) {
        this.imageDataList = imageDataList;
    }

    public void addImageData(ImageData imageData) {
        this.imageDataList.add(imageData);
    }

    public int getTotalPersonCount() {
        return totalPersonCount;
    }

    public void setTotalPersonCount(int totalPersonCount) {
        this.totalPersonCount = totalPersonCount;
    }

    public int getOverlappingPersonCount() {
        return overlappingPersonCount;
    }

    public void setOverlappingPersonCount(int overlappingPersonCount) {
        this.overlappingPersonCount = overlappingPersonCount;
    }

    public boolean isRecycleBin() {
        return isRecycleBin;
    }

    public void setRecycleBin(boolean recycleBin) {
        isRecycleBin = recycleBin;
    }

    public long getDeletedTimestamp() {
        return deletedTimestamp;
    }

    public Bitmap getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(Bitmap thumbnail) {
        this.thumbnail = thumbnail;
    }

    // 获取时间戳的格式化字符串
    public String getFormattedTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
        return sdf.format(new Date(timestamp));
    }

    // 获取第一个图片的缩略图，用于列表展示
    public ImageData getFirstImageData() {
        if (imageDataList != null && !imageDataList.isEmpty()) {
            return imageDataList.get(0);
        }
        return null;
    }

    // 更新总人数（考虑重叠）
    public void updateTotalPersonCount() {
        // 这里简化处理，实际应该进行智能合并，排除重复标记
        int count = 0;
        for (ImageData imageData : imageDataList) {
            count += imageData.getTotalCount();
        }
        // 减去重叠人数
        this.totalPersonCount = Math.max(0, count - overlappingPersonCount);
    }
}