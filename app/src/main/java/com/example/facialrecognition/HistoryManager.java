package com.example.facialrecognition;

import android.content.Context;
import android.util.Log;

import com.example.facialrecognition.model.CountRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * 历史记录管理器 - 单例模式
 * 负责管理识别记录的保存、加载和删除
 */
public class HistoryManager {
    private static final String TAG = "HistoryManager";
    private static HistoryManager instance;
    
    // 历史记录列表
    private final List<CountRecord> historyRecords;
    // 回收站列表
    private final List<CountRecord> trashRecords;
    
    private HistoryManager() {
        historyRecords = new ArrayList<>();
        trashRecords = new ArrayList<>();
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized HistoryManager getInstance() {
        if (instance == null) {
            instance = new HistoryManager();
        }
        return instance;
    }
    
    /**
     * 保存新的识别记录
     */
    public void saveRecord(CountRecord record) {
        if (record != null) {
            historyRecords.add(0, record); // 添加到列表开头
            Log.d(TAG, "记录已保存: " + record.getRecordId());
        }
    }
    
    /**
     * 获取所有历史记录
     */
    public List<CountRecord> getAllHistoryRecords() {
        return new ArrayList<>(historyRecords);
    }
    
    /**
     * 获取回收站记录
     */
    public List<CountRecord> getTrashRecords() {
        return new ArrayList<>(trashRecords);
    }
    
    /**
     * 将记录移至回收站
     */
    public void moveToTrash(CountRecord record) {
        if (record != null) {
            // 从历史记录中移除
            if (historyRecords.remove(record)) {
                // 标记为删除并添加到回收站
                record.moveToTrash();
                trashRecords.add(record);
                Log.d(TAG, "记录已移至回收站: " + record.getRecordId());
            }
        }
    }
    
    /**
     * 从回收站恢复记录
     */
    public void restoreFromTrash(CountRecord record) {
        if (record != null) {
            // 从回收站移除
            if (trashRecords.remove(record)) {
                // 恢复并添加到历史记录
                record.restoreFromTrash();
                historyRecords.add(0, record);
                Log.d(TAG, "记录已从回收站恢复: " + record.getRecordId());
            }
        }
    }
    
    /**
     * 永久删除记录
     */
    public void permanentDelete(CountRecord record) {
        boolean removed = historyRecords.remove(record) || trashRecords.remove(record);
        if (removed) {
            Log.d(TAG, "记录已永久删除: " + record.getRecordId());
        }
    }
    
    /**
     * 清空回收站
     */
    public void emptyTrash() {
        trashRecords.clear();
        Log.d(TAG, "回收站已清空");
    }
    
    /**
     * 清空所有历史记录（慎用）
     */
    public void clearAllHistory() {
        historyRecords.clear();
        Log.d(TAG, "所有历史记录已清空");
    }
    
    /**
     * 获取记录总数
     */
    public int getRecordCount() {
        return historyRecords.size();
    }
    
    /**
     * 获取回收站记录数
     */
    public int getTrashCount() {
        return trashRecords.size();
    }
}