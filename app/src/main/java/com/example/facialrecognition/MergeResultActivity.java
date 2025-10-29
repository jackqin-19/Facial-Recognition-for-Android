package com.example.facialrecognition;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.facialrecognition.model.CountRecord;
import com.example.facialrecognition.model.ImageData;
import com.example.facialrecognition.model.Person;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MergeResultActivity extends AppCompatActivity {

    private TextView tvTotalCount;
    private LinearLayout llResultList;
    private ImageButton btnExport;
    private Button btnSaveRecord;
    private List<ImageData> imageDataList;
    private int totalCount;
    private int overlappingCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_merge_result);

        Log.d("Navigation", "MergeResultActivity创建");

        // 初始化视图
        tvTotalCount = findViewById(R.id.tv_total_count);
        llResultList = findViewById(R.id.ll_result_list);
        btnExport = findViewById(R.id.btn_export);
        btnSaveRecord = findViewById(R.id.btn_save_record);
        ImageButton btnBack = findViewById(R.id.btn_back);
        ImageButton btnHistory = findViewById(R.id.btn_history);

        // 初始化数据列表
        imageDataList = new ArrayList<>();
        
        try {
            Log.d("Navigation", "开始获取Intent数据");
            
            // 安全地从Bundle中获取数据，而不是直接从Intent获取
            Bundle extras = getIntent().getExtras();
            if (extras == null) {
                Log.e("Navigation", "Intent中没有额外数据");
                Toast.makeText(this, "数据传输错误", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 方法1：使用getParcelable安全获取
            ImageData imageData = extras.getParcelable("IMAGE_DATA");
            
            // 方法2：如果方法1失败，尝试创建一个新的ImageData实例
            if (imageData == null) {
                Log.w("Navigation", "无法直接获取IMAGE_DATA，创建默认实例");
                // 创建一个空的ImageData实例
                imageData = new ImageData("", null, ImageData.AreaType.FRONT);
            } else {
                Log.d("Navigation", "成功获取IMAGE_DATA");
                
                // 修复：清理imageData中的Person列表，确保没有null元素
                if (imageData.getDetectedPersons() != null) {
                    List<Person> cleanPersons = new ArrayList<>();
                    for (Person p : imageData.getDetectedPersons()) {
                        if (p != null) {
                            cleanPersons.add(p);
                        }
                    }
                    imageData.setDetectedPersons(cleanPersons);
                }
                
                // 确保clearOperationType是有效字符串
                if (imageData.getClearOperationType() == null || imageData.getClearOperationType().contains("未执行")) {
                    imageData.setClearOperationType("default");
                }
            }
            
            // 添加到列表
            imageDataList.add(imageData);
            Log.d("Navigation", "添加数据到列表，人数: " + imageData.getTotalCount());
            
            // 如果没有获取到数据，尝试COUNT_RECORD
            if (imageDataList.isEmpty()) {
                Log.d("Navigation", "数据列表为空，尝试获取COUNT_RECORD");
                CountRecord countRecord = extras.getParcelable("COUNT_RECORD");
                Log.d("Navigation", "尝试获取COUNT_RECORD: " + (countRecord != null ? "成功" : "失败"));
                
                if (countRecord != null) {
                    imageDataList = countRecord.getImageDataList();
                    if (imageDataList == null) {
                        imageDataList = new ArrayList<>();
                    }
                }
            }
            
            Log.d("Navigation", "数据列表初始化完成，size: " + imageDataList.size());

            // 执行合并统计
            performMerge();
            Log.d("Navigation", "合并统计完成，总人数: " + totalCount);
            
            // 更新UI
            updateTotalCount();
            showResultList();
            checkForOverlaps();
            Log.d("Navigation", "UI更新完成");

        } catch (Exception e) {
            Log.e("Navigation", "MergeResultActivity初始化失败: " + e.getMessage(), e);
            Toast.makeText(this, "初始化失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            // 即使出错也继续设置按钮监听器，确保用户可以返回
        }

        // 设置按钮监听器
        btnBack.setOnClickListener(v -> finish());
        btnExport.setOnClickListener(v -> showExportOptions());
        btnSaveRecord.setOnClickListener(v -> saveRecord());
        btnHistory.setOnClickListener(v -> goToHistory());
    }

    private void performMerge() {
        // 模拟合并算法：计算所有未被删除的标记点总数
        totalCount = 0;
        for (ImageData data : imageDataList) {
            for (Person person : data.getDetectedPersons()) {
                if (!person.isMarkedAsDeleted()) {
                    totalCount++;
                }
            }
        }
        
        // 模拟重叠检测（实际应该基于位置和相似度判断）
        overlappingCount = calculateOverlappingPersons();
    }

    private int calculateOverlappingPersons() {
        // 这是一个简化的重叠检测算法
        // 实际应用中应该基于位置、轮廓相似度等进行更复杂的判断
        if (imageDataList.size() > 1) {
            int totalPersons = 0;
            for (ImageData data : imageDataList) {
                totalPersons += data.getTotalCount();
            }
            // 假设总人数减去合并后的人数就是重叠人数
            return totalPersons - totalCount;
        }
        return 0;
    }

    private void checkForOverlaps() {
        if (overlappingCount > 0) {
            Toast.makeText(this, "检测到可能的重叠区域，涉及约" + overlappingCount + "人，建议手动核对", Toast.LENGTH_LONG).show();
        }
    }

    private void updateTotalCount() {
        tvTotalCount.setText("合并后总人数：" + totalCount + " 人");
    }

    private void showResultList() {
        llResultList.removeAllViews();
        
        for (int i = 0; i < imageDataList.size(); i++) {
            ImageData data = imageDataList.get(i);
            
            // 创建区域结果视图
            View resultView = getLayoutInflater().inflate(R.layout.item_merge_result, llResultList, false);
            
            // 设置区域信息
            TextView tvAreaType = resultView.findViewById(R.id.tv_area_type);
            TextView tvSystemCount = resultView.findViewById(R.id.tv_system_count);
            TextView tvManualAdd = resultView.findViewById(R.id.tv_manual_add);
            TextView tvManualDelete = resultView.findViewById(R.id.tv_manual_delete);
            TextView tvClearOperation = resultView.findViewById(R.id.tv_clear_operation);
            ImageView ivThumbnail = resultView.findViewById(R.id.iv_thumbnail);
            Button btnEdit = resultView.findViewById(R.id.btn_edit);
            
            // 显示区域类型
            String areaTypeName = "未知区域";
            switch (data.getAreaType()) {
                case FRONT:
                    areaTypeName = "前排";
                    break;
                case MIDDLE:
                    areaTypeName = "中排";
                    break;
                case BACK:
                    areaTypeName = "后排";
                    break;
            }
            tvAreaType.setText(areaTypeName);
            
            // 显示统计数据
            tvSystemCount.setText("系统识别：" + data.getRecognizedCount() + " 人");
            tvManualAdd.setText("手动补标：" + data.getManuallyAddedCount() + " 人");
            tvManualDelete.setText("手动删除：" + data.getManuallyDeletedCount() + " 人");
            
            // 显示清空操作记录
            String clearOpText = "未执行清空操作";
            // 这里应该根据记录显示实际的清空操作类型
            tvClearOperation.setText(clearOpText);
            
            // 显示缩略图
            ivThumbnail.setImageBitmap(data.getThumbnail());
            
            // 设置编辑按钮监听器
            final int index = i;
            btnEdit.setOnClickListener(v -> editImageData(index));
            
            llResultList.addView(resultView);
        }
    }

    private void editImageData(int index) {
        // 跳回编辑页面
        Intent intent = new Intent(this, RecognitionActivity.class);
        intent.putExtra("IMAGE_PATH", imageDataList.get(index).getImagePath());
        intent.putExtra("AREA_TYPE", imageDataList.get(index).getAreaType().ordinal());
        startActivity(intent);
    }

    private void showExportOptions() {
        PopupMenu popupMenu = new PopupMenu(this, btnExport);
        popupMenu.getMenuInflater().inflate(R.menu.export_options_menu, popupMenu.getMenu());
        
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_export_image) {
                exportAsImage();
            } else if (itemId == R.id.menu_export_pdf) {
                exportAsPDF();
            }
            return true;
        });
        
        popupMenu.show();
    }

    private void exportAsImage() {
        // 生成报告图片
        Toast.makeText(this, "正在生成图片报告...", Toast.LENGTH_SHORT).show();
        
        // 模拟导出过程
        new Thread(() -> {
            try {
                // 创建长图
                Bitmap reportBitmap = createReportBitmap();
                String filePath = saveReportImage(reportBitmap);
                
                runOnUiThread(() -> {
                    Toast.makeText(MergeResultActivity.this, "图片导出成功", Toast.LENGTH_SHORT).show();
                    // 分享图片
                    shareImage(filePath);
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(MergeResultActivity.this, "图片导出失败", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void exportAsPDF() {
        // 模拟PDF导出
        Toast.makeText(this, "正在生成PDF报告...", Toast.LENGTH_SHORT).show();
        
        // 实际应用中应该使用PDF生成库，如iText或PdfBox
        new Thread(() -> {
            try {
                Thread.sleep(2000); // 模拟处理时间
                runOnUiThread(() -> {
                    Toast.makeText(MergeResultActivity.this, "PDF导出成功", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(MergeResultActivity.this, "PDF导出失败", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private Bitmap createReportBitmap() {
        // 创建一个长图作为报告
        // 这里简化处理，实际应用中应该根据内容动态计算高度
        int width = 720;
        int height = 1280;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);

        // 绘制标题和统计信息
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(24);
        canvas.drawText("教室人数清点报告", 50, 50, paint);
        
        paint.setTextSize(20);
        canvas.drawText("清点时间：" + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()), 50, 90, paint);
        canvas.drawText("合并后总人数：" + totalCount + " 人", 50, 130, paint);

        // 绘制分隔线
        paint.setStrokeWidth(2);
        canvas.drawLine(50, 150, width - 50, 150, paint);

        // 绘制各区域统计数据
        int yPos = 190;
        for (ImageData data : imageDataList) {
            String areaTypeName = "未知区域";
            switch (data.getAreaType()) {
                case FRONT:
                    areaTypeName = "前排";
                    break;
                case MIDDLE:
                    areaTypeName = "中排";
                    break;
                case BACK:
                    areaTypeName = "后排";
                    break;
            }
            
            canvas.drawText(areaTypeName + "统计：", 50, yPos, paint);
            yPos += 40;
            canvas.drawText("  系统识别：" + data.getRecognizedCount() + " 人", 50, yPos, paint);
            yPos += 30;
            canvas.drawText("  手动补标：" + data.getManuallyAddedCount() + " 人", 50, yPos, paint);
            yPos += 30;
            canvas.drawText("  手动删除：" + data.getManuallyDeletedCount() + " 人", 50, yPos, paint);
            yPos += 50;
        }

        return bitmap;
    }

    private String saveReportImage(Bitmap bitmap) throws IOException {
        // 保存报告图片到存储
        File directory = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "reports");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        
        String fileName = "report_" + System.currentTimeMillis() + ".png";
        File file = new File(directory, fileName);
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        }
        
        return file.getAbsolutePath();
    }

    private void shareImage(String filePath) {
        // 分享图片
        File imageFile = new File(filePath);
        Uri imageUri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".fileprovider",
                imageFile);

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
        shareIntent.setType("image/*");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "分享报告"));
    }

    private void saveRecord() {
        // 保存记录到历史
        CountRecord record = new CountRecord(imageDataList, totalCount);
        // 这里应该将记录保存到数据库或文件中
        
        // 计算重叠人数
        record.setOverlappingPersonCount(overlappingCount);
        
        Toast.makeText(this, "记录已保存", Toast.LENGTH_SHORT).show();
    }

    private void goToHistory() {
        // 跳转到历史记录页面
        Intent intent = new Intent(this, HistoryActivity.class);
        startActivity(intent);
    }
}