# 本机 Android 开发环境

安装时间：2026-09-13。

| 工具 | 版本 / 路径 |
| --- | --- |
| Android Studio | 2026.1.4.7，`C:\Program Files\Android\Android Studio` |
| JDK | Temurin 17.0.20.1，`C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot` |
| Android SDK | `C:\Users\QVQQQ\Android\Sdk` |
| SDK Platform | Android 15 / API 35 |
| Build Tools | 35.0.0 |
| Platform Tools / adb | 37.0.1 / 1.0.41 |
| Gradle | Wrapper 8.11.1 |

Android Studio 与 JDK 通过 winget 安装。SDK 命令行工具来自 Google 官方下载并验证 SHA-256。Gradle 发行包验证官方哈希，Wrapper 也启用了发行包哈希校验。

## 使用

环境变量变更后，关闭并重新打开终端 / Android Studio。项目本地 `local.properties` 指向 SDK，`.gradle/config.properties` 与 `.idea/gradle.xml` 指定 Gradle 使用 JDK 17；这些机器相关文件被 Git 忽略。

打开 Android Studio，选择 Open 并打开当前工程。首次启动如出现设置向导，选择已有 SDK 路径 `C:\Users\QVQQQ\Android\Sdk`。在 Settings → Build, Execution, Deployment → Build Tools → Gradle 中确认 Gradle JDK 为本机 JDK 17（或 GRADLE_LOCAL_JAVA_HOME）。Android Studio 自身使用它附带的运行时。

```powershell
java -version
adb version
.\gradlew.bat testDebugUnitTest assembleDebug
```

中文工程路径已配置 `android.overridePathCheck=true`。Windows 下如工程路径含非 ASCII 字符，根构建脚本会把模块构建产物放到 `GRADLE_USER_HOME/recipe-import-build/<工程路径哈希>/app`，解决 JDK 17 测试进程的类加载问题；源码位置不变。若后续引入 NDK 或其他工具后仍出现路径兼容错误，再改用英文路径。

本次生成的 Debug APK 已复制到工程内 `artifacts/recipe-import-debug.apk`（被 Git 忽略）。后续构建的原始 APK 位于上述构建目录中的 `outputs/apk/debug/app-debug.apk`，不会自动更新 artifacts 中的副本。

## 验证状态

- Android Studio 安装成功，安装文件可读取。
- JDK 17 运行正常。
- Gradle Wrapper 生成成功。
- SDK Platform 35、Build Tools 35.0.0 安装完成，adb version 验证通过。
- 用户级 JAVA_HOME、ANDROID_HOME 和 PATH 已配置。
- Figma 界面更新后：13 个单元测试与 2 个真机界面测试通过，assembleDebug 和 lintDebug 通过。
- 已在 PLK110 / Android 16 真机运行并截图核对三个页面；尚未安装模拟器系统镜像。
