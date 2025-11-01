# 人脸识别计数应用

这是一个基于Android平台的人脸识别计数应用，能够通过摄像头拍摄或上传照片进行人脸检测和识别，并支持手动调整检测结果和保存历史记录。

## 功能特性

- 📷 **实时拍照**：使用设备摄像头拍摄照片进行人脸识别
- 🧠 **智能检测**：利用Google ML Kit进行人脸检测和识别
- ✏️ **手动调整**：支持手动添加、删除或修改检测到的人脸标记
- 💾 **历史记录**：保存和查看历史识别记录
- 📖 **操作指南**：提供详细的使用说明
- 🔄 **撤销重做**：支持操作的撤销和重做功能
- 📊 **计数统计**：显示识别到的总人数和详细统计信息

## 技术栈

- **开发语言**：Java
- **开发框架**：Android SDK (API 21-36)
- **人脸检测**：Google ML Kit Face Detection API
- **相机功能**：CameraX
- **数据处理**：Gson (JSON序列化)
- **UI组件**：AndroidX, Material Design
- **图像显示**：自定义ImageView支持缩放和手势操作

## 项目结构

```
src/main/java/com/example/facialrecognition/
├── MainActivity.java          # 主界面，提供功能入口
├── CameraActivity.java        # 相机界面，用于拍照
├── RecognitionActivity.java   # 识别结果编辑界面
├── ConfirmRecognitionActivity.java # 确认和保存界面
├── HistoryActivity.java       # 历史记录界面
├── GuideActivity.java         # 操作指南界面
├── FaceDetectorManager.java   # 人脸检测器管理类
└── model/                     # 数据模型
    ├── ImageData.java         # 图像数据模型
    ├── Person.java            # 人脸模型
    └── CountRecord.java       # 计数记录模型
```

## 安装说明

1. **克隆项目**
   ```bash
   git clone https://github.com/jackqin-19/Facial-Recognition-for-Android.git
   ```

2. **打开项目**
   - 使用Android Studio打开项目文件夹

3. **配置依赖**
   - 项目已包含所有必要依赖，Android Studio会自动同步
   - 主要依赖包括：
     - androidx.appcompat
     - androidx.camera
     - com.google.mlkit:face-detection
     - com.google.code.gson

4. **构建和运行**
   - 选择目标设备或模拟器
   - 点击Run按钮构建并运行应用

## 使用方法

1. **启动应用**
   - 打开应用后进入主界面，显示四个主要功能按钮

2. **拍照识别**
   - 点击"拍照"按钮进入相机界面
   - 对准目标人群，点击拍照按钮
   - 系统自动进行人脸识别，然后进入编辑界面

3. **编辑识别结果**
   - 在编辑界面可以看到自动识别的人脸标记
   - 点击已标记的人脸可以选择删除
   - 点击未标记区域可以手动添加人脸标记
   - 使用缩放和手势操作查看图片细节
   - 可以使用撤销和重做功能

4. **确认和保存**
   - 编辑完成后点击下一步
   - 确认页面显示识别统计信息
   - 点击"保存"按钮保存记录，或"取消"返回

5. **查看历史**
   - 在主界面点击"历史记录"按钮
   - 查看所有保存的识别记录

## 注意事项

- **权限要求**：应用需要相机权限来拍照
- **识别精度**：光线条件会影响识别效果，建议在光线充足的环境下使用
- **性能优化**：对于大型图片或多人场景，识别可能需要稍长时间
- **手动调整**：系统识别可能不完全准确，建议根据实际情况进行手动调整

## 开发说明

### 数据传递优化

项目使用Gson进行JSON序列化来传递复杂数据对象，避免了传统Parcelable实现中可能出现的序列化错误。主要优化点：

- 在Activity间传递数据时，将ImageData对象序列化为JSON字符串
- 避免传递大型Bitmap对象，采用图片路径引用的方式
- 实现多层数据获取机制，确保数据可靠性

### 错误处理

应用实现了全面的错误处理机制，包括：

- 权限请求和检查
- 空数据和异常情况处理
- 用户友好的错误提示
- 详细的日志记录便于调试

## 许可证

[MIT License](LICENSE)

## 贡献

欢迎提交Issue和Pull Request！

---

*本应用基于Google ML Kit开发，旨在提供便捷的人脸识别计数功能。*