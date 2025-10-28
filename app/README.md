# 朋友圈文案生成器

这是一个 Android 应用程序，可以根据用户选择的一系列照片生成朋友圈文案。

## 文件结构

- **app/build.gradle.kts**: Gradle 构建脚本，用于定义项目依赖项和构建设置。
- **src/main/AndroidManifest.xml**: 应用程序清单文件，用于声明应用程序组件和权限。
- **src/main/java/com/example/myapplication/MainActivity.kt**: 应用程序的主活动，用于显示 `PhotoCaptionScreen`。
- **src/main/java/com/example/myapplication/features/PhotoCaptionScreen.kt**: Jetpack Compose UI，用于显示照片选择器、所选照片、生成的文案和加载状态。
- **src/main/java/com/example/myapplication/features/PhotoCaptionViewModel.kt**: ViewModel，用于管理 `PhotoCaptionScreen` 的 UI 状态和业务逻辑。
- **src/main/java/com/example/myapplication/data/CaptionRepository.kt**: 数据仓库，用于从 API 获取数据。
- **src/main/java/com/example/myapplication/data/ApiService.kt**: Retrofit API 接口，用于定义 API 端点。
- **src/main/java/com/example/myapplication/data/RetrofitClient.kt**: Retrofit 客户端，用于创建 `ApiService` 实例。