# 已确认问题的修复与验证

基于提交 `970e80d`，验证日期：2026-10-27。

## 修复内容

| 范围 | 修复 |
| --- | --- |
| 签名密码 | 使用 apksigner 的 `env:` 参数传入密码，完整命令日志中只出现环境变量名称。`SignInfoBean.toString()` 原本已经不包含密码。 |
| Java 配置（关联 #21、#25、#17） | 启动时先加载 JAVA_HOME；apksigner 版本检查传入该配置；Windows 校验 `bin/java.exe`。 |
| 进程执行 | 可执行文件与参数分别传入；保留一次性读取的合并输出；关闭日志时仍排空输出，避免大量输出堵塞。 |
| 工具路径 | native 工具直接校验文件和执行权限；移除未经过 shell 却调用 `command -v` 的检查；Windows 不再忽略文件不存在的结果。 |
| 对齐和签名 | 对齐失败立即返回错误；签名先写临时文件并验证后再替换原输出，失败时保留原有输出；V4 签名文件同步移动，取消 V4 后删除过期签名文件；批量输出同名时拒绝覆盖；临时文件会清理，大写 `.APK` 不覆盖输入文件。 |
| 签名版本（关联 #8、#16、#27） | 从 `sign --help` 探测可用参数，仅传工具支持的参数；未选中的受支持版本显式禁用；用户选择不支持的版本时明确报错。 |
| 自动匹配（关联 #9 评论） | 直接执行 `aapt dump badging` 并在 Kotlin 中解析包名，移除无效的管道和 grep。 |
| 批量结果 | 只有全部文件成功才报告成功；每个文件的结果只出现一次；未完成状态返回错误而非强制类型转换。 |
| 目录选择（关联 #27） | 异常在事件线程中捕获并完整恢复调用方；使用公开协程 API；已取消的排队操作不会继续打开对话框。 |
| 签名类型初始化 | 延迟初始化签名列表，避免先访问 V1 时列表含 null。 |

这些修复针对代码中证实的缺陷，不代表信息不足的 #31、#23、#20 或 #27 原始报错已经定位或复现。#26 和 #9 的批量导入等功能需求未包含在此修复中。

## 验证结果

- macOS、JDK 17、Android SDK Build Tools 34.0.0。
- `build` 和 macOS DMG 打包成功；31 个测试全部通过，无失败或跳过。
- 真实 SDK 测试动态创建 keystore 和无代码 APK，完成包名读取、对齐、签名与验签；覆盖 V1、V1+V2、V1+V2+V4，确认 V3 未启用并检查 V4 签名文件。
- 模拟工具回归覆盖不支持的签名参数、含空格路径、环境变量密码、对齐失败、缺失输出、批量结果和目录选择异常。
- Windows 的 Java 文件名和 native 文件不存在分支已用测试覆盖，但尚未在 Windows 系统上执行完整 `.bat` 和图形界面流程。
- GitHub Actions 工作流通过 actionlint 检查，配置了 Windows、Linux 和 macOS 两种架构的构建及真实 SDK 集成测试。
- 构建仍有仓库原有的弃用、unchecked cast、KSP 资源提示；此次未升级构建工具链。

## 运行测试

普通回归测试：

```bash
bash ./gradlew test
```

真实 Android SDK 集成测试是可选的。设置两个环境变量，再运行测试；路径须指向当前操作系统可执行的 Build Tools 和已安装平台的 android.jar：

```bash
APKSIGNER_TEST_BUILD_TOOLS=/path/to/android/sdk/build-tools/34.0.0 \
APKSIGNER_TEST_ANDROID_JAR=/path/to/android/sdk/platforms/android-34/android.jar \
bash ./gradlew build
```

所有测试密钥和 APK 都在临时目录生成并清理。使用 shell 模拟 SDK 的测试在 Windows 上跳过；其余测试和显式启用的真实 SDK 集成测试可执行。
