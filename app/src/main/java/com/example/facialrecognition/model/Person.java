package com.example.facialrecognition.model;

import android.graphics.PointF;
import android.os.Parcel;
import android.os.Parcelable;

public class Person implements Parcelable {
    private int id;
    private PointF position; // 中心点位置
    private float confidence; // 置信度
    private boolean isManual; // 是否是手动添加的
    private boolean isMarkedAsDeleted; // 是否被标记为删除
    private boolean isUncertain; // 是否是不确定标记

    public Person(int id, PointF position, float confidence) {
        this.id = id;
        this.position = position;
        this.confidence = confidence;
        this.isManual = false;
        this.isMarkedAsDeleted = false;
        this.isUncertain = confidence < 0.7f; // 假设0.7为低置信度阈值
    }

    public Person(int id, PointF position) {
        this.id = id;
        this.position = position;
        this.confidence = 1.0f; // 手动添加的置信度为1
        this.isManual = true;
        this.isMarkedAsDeleted = false;
        this.isUncertain = false;
    }

    protected Person(Parcel in) {
        id = in.readInt();
        position = in.readParcelable(PointF.class.getClassLoader());
        confidence = in.readFloat();
        isManual = in.readByte() != 0;
        isMarkedAsDeleted = in.readByte() != 0;
        isUncertain = in.readByte() != 0;
    }

    public static final Creator<Person> CREATOR = new Creator<Person>() {
        @Override
        public Person createFromParcel(Parcel in) {
            return new Person(in);
        }

        @Override
        public Person[] newArray(int size) {
            return new Person[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeParcelable(position, i);
        parcel.writeFloat(confidence);
        parcel.writeByte((byte) (isManual ? 1 : 0));
        parcel.writeByte((byte) (isMarkedAsDeleted ? 1 : 0));
        parcel.writeByte((byte) (isUncertain ? 1 : 0));
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public PointF getPosition() {
        return position;
    }

    public void setPosition(PointF position) {
        this.position = position;
    }

    public float getConfidence() {
        return confidence;
    }

    public boolean isManual() {
        return isManual;
    }

    public boolean isMarkedAsDeleted() {
        return isMarkedAsDeleted;
    }

    public void setMarkedAsDeleted(boolean markedAsDeleted) {
        isMarkedAsDeleted = markedAsDeleted;
    }

    public boolean isUncertain() {
        return isUncertain;
    }

    public void setUncertain(boolean uncertain) {
        isUncertain = uncertain;
    }

    // 计算与另一个Person的距离，用于检测重复
    public double distanceTo(Person other) {
        float dx = this.position.x - other.position.x;
        float dy = this.position.y - other.position.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
}