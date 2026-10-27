<div align="center">

![ApkSigner 图标](src/main/resources/imgs/icon.png)

# ApkSigner

面向 Android 开发者的跨平台 APK 图形化签名工具。

[English](docs/en/README.md) · [项目仓库](https://github.com/hbdzp/ApkSigner) · [下载](https://github.com/hbdzp/ApkSigner/releases) · [构建记录](https://github.com/hbdzp/ApkSigner/actions/workflows/build.yml)

![构建状态](https://img.shields.io/github/actions/workflow/status/hbdzp/ApkSigner/build.yml?branch=master)
![许可证](https://img.shields.io/badge/license-GPL--3.0--or--later-blue)

</div>

ApkSigner 使用 [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/) 构建，支持 macOS、Windows 和 Linux。当前仓库由 [hbdzp](https://github.com/hbdzp) 维护，基于 [JI,XIAOYONG 的 ApkSigner](https://github.com/jixiaoyong/ApkSigner) 修改；原作者联系邮箱为 jixiaoyong1995@gmail.com。

## 功能

- 使用 Android SDK 的 `apksigner` 对 APK 进行签名，支持工具版本提供的 V1、V2、V3、V4 签名方案。
- 管理和切换签名配置，按 APK 包名匹配已保存的配置，支持批量签名和查看签名信息。
- 可选执行 `zipalign`，设置输出目录和应用内 `JAVA_HOME`。
- 支持中英文界面、浅色和深色主题，并提供运行日志。

当前版本使用已有 keystore，不提供生成或批量导入签名文件的功能。签名配置保存在本机，请保护好密钥库和本地配置文件。

## 安装与使用

1. 版本发布后，从 [Releases](https://github.com/hbdzp/ApkSigner/releases) 下载对应系统的安装包；也可以从 [Actions 构建记录](https://github.com/hbdzp/ApkSigner/actions/workflows/build.yml)下载成功运行的 Artifacts。下载 Actions 产物通常需要登录 GitHub。
2. 准备 Android SDK 的 Build Tools、Java 开发环境和签名 keystore。
3. 在应用的「设置信息」中配置 Build Tools 版本目录，必要时指定 `JAVA_HOME`；在「签名信息」中填写 keystore 路径、别名和密码。
4. 在「签名 APP」中选择 APK、签名配置和签名方案，然后执行签名。V4 签名还会生成对应的 `.idsig` 文件。

macOS 请根据芯片选择 ARM64 或 Intel 安装包。只有目标 Build Tools 支持的签名方案可以使用。

## 从源码构建

需要 JDK 17。使用本仓库的 Gradle Wrapper：

```bash
# macOS / Linux：运行测试并构建当前平台安装包
./gradlew build packageReleaseDistributionForCurrentOS

# Windows PowerShell
.\gradlew.bat build packageReleaseDistributionForCurrentOS
```

构建与测试说明见 [已确认问题的修复与验证](docs/confirmed-issues-fixes.md)。

GitHub Actions 会对分支推送、Pull Request 和手动触发运行跨平台构建，并上传 macOS ARM64、macOS Intel、Windows x64、Linux x64 安装包和测试报告。推送形如 `v1.2.3` 的版本标签时，所有平台构建和测试成功后，工作流会自动创建 GitHub Release 并上传安装包；形如 `v1.2.3-beta.1` 的标签会创建为预发布版本。

发布前，请先把 `build.gradle.kts` 中的 `packageVersion` 设置为标签对应的三段版本号，并将此修改提交。标签版本与 `packageVersion` 不一致时，工作流会停止，避免发布版本不符的安装包。

例如，当前 `packageVersion` 为 `1.2.2` 时，可以在包含上述修改的提交上推送标签：

```bash
git tag v1.2.2
git push https://github.com/hbdzp/ApkSigner.git v1.2.2
```

## 界面预览

| 签名信息 | 签名应用 | 设置 | 深色主题 |
| --- | --- | --- | --- |
| ![签名信息](docs/screenshort/sign_info.png) | ![签名应用](docs/screenshort/sign_app.png) | ![设置](docs/screenshort/sign_settings.png) | ![深色主题](docs/screenshort/sign_app_dark.png) |

## 许可证与上游署名

原项目 Copyright (C) JI,XIAOYONG。本仓库的修改由 [hbdzp](https://github.com/hbdzp) 维护。项目按 [GNU GPL v3 或更高版本](LICENSE)提供，不提供任何担保。

原项目 README 同时列出了以下附加条款：

- **保留作者信息**：使用、复制、修改或分发本项目的源代码时，必须保留原作者的姓名和联系信息。
- **Logo 使用限制**：未经原作者明确书面许可，不得使用、复制、修改或分发本项目的 Logo。
- **源码链接**：在使用、复制、修改或分发本项目的源代码、软件作品时，必须在显著位置（例如本软件「设置信息」页面底部）提供指向原项目源码网址的链接。

  文本内容为「本项目基于 JI,XIAOYONG 的开源项目 ApkSigner 开发，你可以免费在 <https://github.com/jixiaoyong/ApkSigner> 获取项目源代码」。
