package com.example.facialrecognition.model;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class ImageData implements Parcelable {
    // 添加版本控制
    private static final int PARCEL_VERSION = 1;
    
    private String imagePath;
    private Bitmap bitmap;
    private Bitmap thumbnail;
    private List<Person> detectedPersons;
    private int recognizedCount; // 系统识别的人数
    private int manuallyAddedCount; // 手动添加的人数
    private int manuallyDeletedCount; // 手动删除的人数
    private String clearOperationType; // 执行过的清空操作类型

    public ImageData(String imagePath, Bitmap bitmap) {
        // 对于模拟数据，使用空字符串或检查路径是否存在
        this.imagePath = imagePath;
        this.bitmap = bitmap;
        // 生成缩略图
        if (bitmap != null) {
            this.thumbnail = Bitmap.createScaledBitmap(bitmap, 200, 150, true);
        }
        this.detectedPersons = new ArrayList<>();
        this.recognizedCount = 0;
        this.manuallyAddedCount = 0;
        this.manuallyDeletedCount = 0;
        this.clearOperationType = "default";
    }

    protected ImageData(Parcel in) {
        try {
            // 读取版本号
            int version = in.readInt();
            
            if (version == PARCEL_VERSION) {
                imagePath = in.readString();
                // 从Parcel读取时，bitmap会是null
                // 在需要时通过imagePath重新加载
                bitmap = null;
                thumbnail = in.readParcelable(Bitmap.class.getClassLoader());
                
                // 确保detectedPersons不为null - 使用更安全的方式读取
                int size = in.readInt();
                detectedPersons = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    Person person = in.readParcelable(Person.class.getClassLoader());
                    if (person != null) {
                        detectedPersons.add(person);
                    }
                }
                
                recognizedCount = in.readInt();
                manuallyAddedCount = in.readInt();
                manuallyDeletedCount = in.readInt();
                
                // 确保clearOperationType不为null
                clearOperationType = in.readString();
                if (clearOperationType == null) {
                    clearOperationType = "default";
                }
            } else {
                // 处理旧版本或默认值
                imagePath = "";
                bitmap = null;
                thumbnail = null;
                detectedPersons = new ArrayList<>();
                recognizedCount = 0;
                manuallyAddedCount = 0;
                manuallyDeletedCount = 0;
                clearOperationType = "default";
            }
        } catch (Exception e) {
            // 捕获所有异常，确保应用不会崩溃
            // 初始化默认值
            imagePath = "";
            bitmap = null;
            thumbnail = null;
            detectedPersons = new ArrayList<>();
            recognizedCount = 0;
            manuallyAddedCount = 0;
            manuallyDeletedCount = 0;
            clearOperationType = "default";
        }
    }

    public static final Creator<ImageData> CREATOR = new Creator<ImageData>() {
        @Override
        public ImageData createFromParcel(Parcel in) {
            try {
                return new ImageData(in);
            } catch (Exception e) {
                // 捕获所有异常，返回一个空的实例而不是导致应用崩溃
                return new ImageData("", null);
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
        try {
            // 写入版本号
            parcel.writeInt(PARCEL_VERSION);
            
            parcel.writeString(imagePath);
            // 不传递完整的Bitmap，只传递图片路径
            // 避免TransactionTooLargeException异常
            parcel.writeParcelable(null, i); // bitmap设为null
            parcel.writeParcelable(thumbnail, i); // 仍然传递缩略图，它很小
            
            // 安全地写入列表大小和内容
            if (detectedPersons != null) {
                parcel.writeInt(detectedPersons.size());
                for (Person person : detectedPersons) {
                    parcel.writeParcelable(person, i);
                }
            } else {
                parcel.writeInt(0);
            }
            
            parcel.writeInt(recognizedCount);
            parcel.writeInt(manuallyAddedCount);
            parcel.writeInt(manuallyDeletedCount);
            parcel.writeString(clearOperationType);
        } catch (Exception e) {
            // 记录错误但不崩溃
            parcel.writeInt(PARCEL_VERSION); // 仍然写入版本号
            parcel.writeString("");
            parcel.writeParcelable(null, i);
            parcel.writeParcelable(null, i);
            parcel.writeInt(0); // 空列表
            parcel.writeInt(0);
            parcel.writeInt(0);
            parcel.writeInt(0);
            parcel.writeString("default");
        }
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
    
    // 添加获取有效人数的方法（排除已删除标记的人员）
    public int getValidPersonCount() {
        if (detectedPersons == null) {
            return 0;
        }
        
        int count = 0;
        for (Person person : detectedPersons) {
            if (person != null && !person.isMarkedAsDeleted()) {
                count++;
            }
        }
        return count;
    }
    
    // 获取personList（兼容新代码）
    public List<Person> getPersonList() {
        return getDetectedPersons();
    }
}