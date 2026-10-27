<div align="center">

![ApkSigner icon](../../src/main/resources/imgs/icon.png)

# ApkSigner

A cross-platform graphical APK signing tool for Android developers.

[中文](../../README.md) · [Repository](https://github.com/hbdzp/ApkSigner) · [Downloads](https://github.com/hbdzp/ApkSigner/releases) · [Builds](https://github.com/hbdzp/ApkSigner/actions/workflows/build.yml)

![Build status](https://img.shields.io/github/actions/workflow/status/hbdzp/ApkSigner/build.yml?branch=master)
![License](https://img.shields.io/badge/license-GPL--3.0--or--later-blue)

</div>

ApkSigner is built with [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/) and runs on macOS, Windows and Linux. This fork is maintained by [hbdzp](https://github.com/hbdzp) and is based on [ApkSigner by JI,XIAOYONG](https://github.com/jixiaoyong/ApkSigner), whose contact email is jixiaoyong1995@gmail.com.

## Features

- Sign APKs with Android SDK `apksigner`, using V1, V2, V3 and V4 schemes supported by the selected Build Tools version.
- Manage signing configurations, match saved configurations by APK package name, and sign or inspect multiple APKs.
- Optionally run `zipalign` and configure the output directory and in-app `JAVA_HOME`.
- Use the application in Chinese or English, with light or dark themes and accessible logs.

The application uses an existing keystore; generating or bulk-importing signing files is not supported. Signing configurations are saved locally, so protect your keystore and local configuration files.

## Installation and usage

1. When a version is available, download an installer from [Releases](https://github.com/hbdzp/ApkSigner/releases), or download Artifacts from a successful [Actions run](https://github.com/hbdzp/ApkSigner/actions/workflows/build.yml). Downloading Actions artifacts normally requires a GitHub login.
2. Prepare Android SDK Build Tools, a Java development environment and a signing keystore.
3. In Settings, configure the Build Tools version directory and specify `JAVA_HOME` if needed. In Sign Info, enter the keystore path, alias and passwords.
4. In Sign App, select APKs, a signing configuration and signature schemes, then sign. V4 signing also produces an `.idsig` file.

For macOS, choose the ARM64 or Intel installer for your processor. The available signing schemes depend on your Build Tools version.

## Build from source

Install JDK 17 and use the Gradle Wrapper included in this repository:

```bash
# macOS / Linux: run tests and build an installer for the current platform
./gradlew build packageReleaseDistributionForCurrentOS

# Windows PowerShell
.\gradlew.bat build packageReleaseDistributionForCurrentOS
```

See the [fix and verification notes](../confirmed-issues-fixes.md) for test details.

GitHub Actions builds on branch pushes, pull requests and manual runs, and uploads installers and test reports for macOS ARM64, macOS Intel, Windows x64 and Linux x64. Pushing a version tag such as `v1.2.3` creates a GitHub Release with all four installers after every platform builds and tests successfully. A tag such as `v1.2.3-beta.1` creates a pre-release.

Before tagging, set and commit `packageVersion` in `build.gradle.kts` to the matching three-part version. The workflow rejects mismatched tags to avoid publishing installers with the wrong version.

For example, when `packageVersion` is `1.3.0`, push a tag pointing to the commit containing the changes:

```bash
git tag v1.3.0
git push https://github.com/hbdzp/ApkSigner.git v1.3.0
```

## Screenshots

| Sign info | Sign app | Settings | Dark theme |
| --- | --- | --- | --- |
| ![Sign info](../screenshort/sign_info.png) | ![Sign app](../screenshort/sign_app.png) | ![Settings](../screenshort/sign_settings.png) | ![Dark theme](../screenshort/sign_app_dark.png) |

## License and attribution

Original project Copyright (C) JI,XIAOYONG. Changes in this fork are maintained by [hbdzp](https://github.com/hbdzp). The project is provided under the [GNU GPL v3 or later](../../LICENSE), without warranty.

The original project README also lists the following additional terms:

- **Retention of Author Information**: When using, copying, modifying, or distributing the source code of this project, the name and contact information of the original author must be retained.
- **Logo Use Restriction**: The Logo of this project may not be used, copied, modified, or distributed without the explicit written permission of the original author.
- **Source Code Link**: When using, copying, modifying, or distributing the source code or software works of this project, a link pointing to the original project source code URL must be provided in a prominent location (e.g., at the bottom of the software's "Settings Information" page).

  The text content should be: "This project is developed based on JI,XIAOYONG's open-source project ApkSigner. You can obtain the project source code for free at <https://github.com/jixiaoyong/ApkSigner>."
