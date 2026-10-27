import io.github.jixiaoyong.beans.CommandResult
import io.github.jixiaoyong.beans.SignType
import kotlinx.coroutines.*
import java.io.IOException
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.util.Locale

/**
 * @author : jixiaoyong
 * @description ： 使用ADB build tools中的zipalign和apksigner.jar对齐并签名APK
 * 上述两个文件可以替换为你自己的文件
 * 官方限制：API 24以上即使开启V1签名也无法生效
 * 他们的使用可以参考官网文章：
 * 签名apk https://developer.android.com/studio/command-line/apksigner?hl=zh-cn#options-sign
 * apksigner：https://developer.android.com/studio/command-line/apksigner?hl=zh-cn#usage-sign
 * zipalign：https://developer.android.com/studio/command-line/zipalign?hl=zh-cn
 * @email : jixiaoyong1995@gmail.com
 * @date : 3/8/2023
 */
object ApkSigner {

    private const val ANDROID_BUILD_TOOLS_DIR_EXAMPLE =
        "/some/directory/to/your/android/sdk/build-tools/34.0.0/"
    private const val AOSP_NAME = "The Android Open Source Project"

    private lateinit var apkSignerCmdPath: String
    private lateinit var zipAlignCmdPath: String
    private lateinit var aaptCmdPath: String
    var javaHomePath: String? = null
        private set

    val apkSignerPath get() = if (::apkSignerCmdPath.isInitialized) apkSignerCmdPath else null
    val zipAlignPath get() = if (::zipAlignCmdPath.isInitialized) zipAlignCmdPath else null
    val aaptPath get() = if (::aaptCmdPath.isInitialized) aaptCmdPath else null

    fun isInitialized(): Boolean {
        return (::apkSignerCmdPath.isInitialized && ::zipAlignCmdPath.isInitialized)
    }

    /**
     * 使用此类之前必须先初始化相关配置
     * @param androidBuildToolsDir android build tools的目录
     * @return 返回失败原因，可对用户展示，返回null则表示正常初始化
     */
    fun init(androidBuildToolsDir: String?): String? {
        val buildTools =
            if (androidBuildToolsDir.isNullOrBlank()) return "" else File(androidBuildToolsDir)
        if (!buildTools.exists() || !buildTools.isDirectory) {
            return "指定的Android SDK中build-tools目录无效或不存在($androidBuildToolsDir)，请重新选择。\n该目录一般为${ANDROID_BUILD_TOOLS_DIR_EXAMPLE}"
        }

        val apkSignerPath = "$androidBuildToolsDir${File.separator}apksigner"
        // windows追加bat
        val apkSignerPathWithBat = if (System.getProperties().getProperty("os.name").contains("Windows")) {
            "$apkSignerPath.bat"
        } else {
            apkSignerPath
        }
        val apkSignerResult = setupApkSigner(apkSignerPathWithBat)
        if (null != apkSignerResult) {
            return apkSignerResult
        }

        val zipAlignPath = "$androidBuildToolsDir${File.separator}zipalign"
        // windows追加exe
        val zipAlignPathWithExe = if (System.getProperties().getProperty("os.name").contains("Windows")) {
            "$zipAlignPath.exe"
        } else {
            zipAlignPath
        }
        val zipAlignResult = setupZipAlign(zipAlignPathWithExe)
        if (null != zipAlignResult) {
            return zipAlignResult
        }

        val aaptPath = "$androidBuildToolsDir${File.separator}aapt"
        // windows追加exe
        val aaptPathExe = if (System.getProperties().getProperty("os.name").contains("Windows")) {
            "$aaptPath.exe"
        } else {
            aaptPath
        }
        val aaptResult = setupAapt(aaptPathExe)
        if (null != aaptResult) {
            return aaptResult
        }
        return null
    }

    fun setupApkSigner(apkSignerPath: String): String? {
        // 校验文件是否为AOSP提供的apk signer，而非本APP避免误操作导致无限循环启动
        val apkSignerFile = File(apkSignerPath)
        if (!apkSignerFile.isFile) {
            return "apkSigner命令(${apkSignerPath})不存在，请重新选择。"
        }
        val isAospScript = try {
            apkSignerFile.readText().contains(AOSP_NAME)
        } catch (e: IOException) {
            return "无法读取apkSigner命令($apkSignerPath)：${e.message}"
        }
        if (!isAospScript) {
            return "apkSigner命令(${apkSignerPath})不是${AOSP_NAME}官方提供的，请重新选择。"
        }

        val result = RunCommandUtil.runCommand(apkSignerPath, "version", tag = "apk signer", javaHome = javaHomePath)

        return if (result != null) {
            "apkSigner命令(${apkSignerPath})检查失败，请重试（${result.message}）"
        } else {
            apkSignerCmdPath = apkSignerPath
            null
        }
    }

    fun setupZipAlign(zipAlignPath: String): String? {
        val error = checkNativeTool(zipAlignPath, "zipAlign")
        if (error == null) zipAlignCmdPath = zipAlignPath
        return error
    }

    private fun checkNativeTool(path: String, name: String): String? {
        val file = File(path)
        return when {
            !file.isFile -> "$name 命令($path)不存在或不是文件，请重新选择。"
            !file.canExecute() -> "$name 命令($path)没有执行权限。"
            else -> null
        }
    }

    internal fun javaExecutable(javaHome: String): File =
        File(javaHome, if (System.getProperty("os.name").contains("Windows")) "bin/java.exe" else "bin/java")

    /**
     * 检测 JAVA_HOME 路径是否有效，并确保它指向 JDK 目录
     */
    fun setupJavaHome(javaHome: String): String? {
        if (javaHome.isBlank()) {
            return "JAVA_HOME 路径不能为空"
        }

        val javaExecutable = javaExecutable(javaHome)
        if (!javaExecutable.isFile || !javaExecutable.canExecute()) {
            return "JAVA_HOME 路径无效，未找到 java 可执行文件"
        }

        // 检查是否包含 include 目录或 bin/javac，以确定是否为 JDK 目录（JDK8及以下可用tools.jar补充判断）
        val includeDir = File(javaHome, "include")
        val javacFile = File(javaHome, "bin${File.separator}javac")
        val javacExeFile = File(javaHome, "bin${File.separator}javac.exe")
        val toolsJar = File(javaHome, "lib${File.separator}tools.jar")
        val isJdk = includeDir.exists() || javacFile.exists() || javacExeFile.exists() || toolsJar.exists()
        if (!isJdk) {
            return "JAVA_HOME 路径无效，它似乎指向 JRE 目录而不是 JDK 目录"
        }

        // 尝试运行 java -version 命令，检查 JDK 是否可以正常运行
        try {
            val result = RunCommandUtil.runCommandWithResult(javaExecutable.absolutePath, "-version",
                printLog = false, printError = false)
            if (result is CommandResult.Error<*>) {
                return "JAVA_HOME 路径无效，java 命令运行失败：${result.message}"
            }
        } catch (e: IOException) {
            return "JAVA_HOME 路径无效，无法运行 java 命令: ${e.message}"
        }

        javaHomePath = javaHome
        return null
    }

    fun setupAapt(aaptPath: String): String? {
        val error = checkNativeTool(aaptPath, "aapt")
        if (error == null) aaptCmdPath = aaptPath
        return error
    }


    /**
     * 获取当前apk文件的包名
     */
    suspend fun getApkPackageName(apkFilePath: String?): String? {
        if (apkFilePath.isNullOrBlank() || !::aaptCmdPath.isInitialized) {
            return null
        }
        val result = withContext(Dispatchers.IO) {
            RunCommandUtil.runCommandWithResult(
                aaptCmdPath, "dump", "badging", apkFilePath,
                tag = "aapt",
                javaHome = javaHomePath
            )
        }
        return if (result is CommandResult.Success<*>) {
            val commandResult = result.result?.toString()
            val regex = "package: name='(.*?)'".toRegex()
            val matchResult = commandResult?.let { regex.find(it) }
            matchResult?.groups?.get(1)?.value
        } else {
            null
        }
    }

    /**
     * 对齐并签名APK文件，签名后的文件添加_signed后缀,即x_signed.apk
     * @param apkFilePathList 待签名的apk文件绝对路径的列表
     * @param keyStorePath 签名文件路径
     * @param keyAlias 表示 signer 在密钥库中的私钥和证书数据的别名的名称
     * @param keyStorePwd 包含 signer 私钥和证书的密钥库的密码
     * @param keyPwd signer 私钥的密码。
     * @param zipAlign 是否需要对齐
     * @param signedApkDirectory 签名之后的文件输出路径，默认为apkFilePath对应的x.apk所在的文件夹
     * @return 返回结果 io.github.jixiaoyong.beans.CommandResult 成功或失败，及信息
     */
    suspend fun alignAndSignApk(
        apkFilePathList: List<String>,
        keyStorePath: String,
        keyAlias: String,
        keyStorePwd: String,
        keyPwd: String,
        signedApkDirectory: String? = null,
        zipAlign: Boolean = true,
        signVersions: List<SignType> = SignType.DEF_SIGN_TYPES,
        onProgress: (String) -> Unit
    ): List<CommandResult> = coroutineScope {
        val caseInsensitive = System.getProperty("os.name").let { it.contains("Windows") || it.contains("Mac") }
        val outputs = apkFilePathList.map { path ->
            val output = signedOutputFile(path, signedApkDirectory).absoluteFile.normalize().path
            if (caseInsensitive) output.lowercase(Locale.ROOT) else output
        }
        val counts = outputs.groupingBy { it }.eachCount()
        val deferredList = apkFilePathList.mapIndexed { index, path ->
            async {
                if (counts.getValue(outputs[index]) > 1) {
                    CommandResult.Error("多个 APK 的输出文件名相同：${signedOutputFile(path, signedApkDirectory)}")
                } else {
                    alignAndSignApk(path, keyStorePath, keyAlias, keyStorePwd, keyPwd,
                        signedApkDirectory, zipAlign, signVersions, onProgress)
                }
            }
        }

        deferredList.awaitAll()
    }

    /**
     * 对齐并签名APK文件，签名后的文件添加_signed后缀,即x_signed.apk
     * @param apkFilePath 待签名的apk文件绝对路径
     * @param keyStorePath 签名文件路径
     * @param keyAlias 表示 signer 在密钥库中的私钥和证书数据的别名的名称
     * @param keyStorePwd 包含 signer 私钥和证书的密钥库的密码
     * @param keyPwd signer 私钥的密码。
     * @param zipAlign 是否需要对齐
     * @param signedApkDirectory 签名之后的文件输出路径，默认为apkFilePath对应的x.apk所在的文件夹
     * @return 返回结果 io.github.jixiaoyong.beans.CommandResult 成功或失败，及信息
     */
    fun alignAndSignApk(
        apkFilePath: String,
        keyStorePath: String,
        keyAlias: String,
        keyStorePwd: String,
        keyPwd: String,
        signedApkDirectory: String? = null,
        zipAlign: Boolean = true,
        signVersions: List<SignType> = SignType.DEF_SIGN_TYPES,
        onProgress: (String) -> Unit
    ): CommandResult {

        if (!isInitialized()) {
            return CommandResult.Error("请先初始化相关配置")
        }

        val input = File(apkFilePath)
        if (!input.isFile || !input.extension.equals("apk", ignoreCase = true)) {
            return CommandResult.Error("APK 文件无效：$apkFilePath")
        }
        if (signVersions.isEmpty()) return CommandResult.Error("请至少选择一种签名方式")

        var alignedFile: File? = null
        var temporarySigned: File? = null
        try {
            // Query capabilities rather than sending V3/V4 flags to older build-tools.
            val help = RunCommandUtil.runCommandWithResult(apkSignerCmdPath, "sign", "--help",
                javaHome = javaHomePath, printLog = false, printError = false)
            if (help !is CommandResult.Success<*>) {
                return CommandResult.Error("无法检查 apksigner 支持的签名方式：${(help as? CommandResult.Error<*>)?.message}")
            }
            val supported = SignType.ALL_SIGN_TYPES.filter {
                help.result.toString().contains("--v${it.type}-signing-enabled")
            }
            val unsupported = signVersions.filter { it !in supported }
            if (unsupported.isNotEmpty()) {
                return CommandResult.Error("当前 apksigner 不支持 ${unsupported.joinToString { it.name }}，请升级 Android SDK Build Tools 或取消对应选项。")
            }
            val signVersionParams = supported.flatMap {
                listOf("--v${it.type}-signing-enabled", (it in signVersions).toString())
            }

            val source = if (zipAlign) {
                val temporary = File.createTempFile("apksigner-aligned-", ".apk")
                alignedFile = temporary
                val result = zipAlignApk(apkFilePath, temporary.absolutePath, onProgress)
                if (result is CommandResult.Error<*>) return result
                temporary.absolutePath
            } else apkFilePath
            val output = signedOutputFile(apkFilePath, signedApkDirectory)
            val stagedOutput = File.createTempFile(".apksigner-", ".apk", output.absoluteFile.parentFile)
            temporarySigned = stagedOutput
            val processBuilder = RunCommandUtil.createProcessBuilder(javaHome = javaHomePath)
            // Only environment variable names enter the command line and logs.
            processBuilder.environment()["APKSIGNER_STORE_PASSWORD"] = keyStorePwd
            processBuilder.environment()["APKSIGNER_KEY_PASSWORD"] = keyPwd
            processBuilder.command(listOf(apkSignerCmdPath, "sign", "-v", "--ks", keyStorePath,
                "--ks-key-alias", keyAlias, "--key-pass", "env:APKSIGNER_KEY_PASSWORD",
                "--ks-pass", "env:APKSIGNER_STORE_PASSWORD") + signVersionParams +
                listOf("--out", stagedOutput.absolutePath, source))
            val result = executeTool(processBuilder, onProgress)
            if (result is CommandResult.Error<*>) return result
            if (!stagedOutput.isFile || stagedOutput.length() == 0L) {
                return CommandResult.Error("签名命令未生成有效的输出文件：${output.absolutePath}")
            }
            val stagedSidecar = File("${stagedOutput.absolutePath}.idsig")
            if (SignType.V4 in signVersions && (!stagedSidecar.isFile || stagedSidecar.length() == 0L)) {
                return CommandResult.Error("签名命令未生成有效的 V4 签名文件：${output.absolutePath}.idsig")
            }
            val outputSidecar = File("${output.absolutePath}.idsig")
            Files.move(stagedOutput.toPath(), output.toPath(), REPLACE_EXISTING)
            if (SignType.V4 in signVersions) {
                Files.move(stagedSidecar.toPath(), outputSidecar.toPath(), REPLACE_EXISTING)
            } else {
                Files.deleteIfExists(outputSidecar.toPath())
            }
            return CommandResult.Success(output.absolutePath)
        } catch (e: Exception) {
            Logger.error("签名失败", e)
            return CommandResult.Error("签名失败：${e.message}", e)
        } finally {
            alignedFile?.delete()
            temporarySigned?.let {
                it.delete()
                File("${it.absolutePath}.idsig").delete()
            }
        }
    }

    private fun signedOutputFile(apkFilePath: String, signedApkDirectory: String?): File {
        val input = File(apkFilePath)
        return File(signedApkDirectory ?: input.absoluteFile.parent, "${input.nameWithoutExtension}_signed.apk")
    }


    private fun zipAlignApk(
        apkFilePath: String,
        alignedApkPath: String,
        onProgress: (String) -> Unit
    ): CommandResult {
        val processBuilder = RunCommandUtil.createProcessBuilder(javaHome = javaHomePath)
        processBuilder.command(zipAlignCmdPath, "-v", "-f", "-p", "4", apkFilePath, alignedApkPath)
        val result = executeTool(processBuilder, onProgress)
        if (result is CommandResult.Error<*>) return result
        val output = File(alignedApkPath)
        return if (output.isFile && output.length() > 0L) CommandResult.Success(alignedApkPath)
        else CommandResult.Error("对齐命令未生成有效的输出文件：$alignedApkPath")
    }

    private fun executeTool(processBuilder: ProcessBuilder, onProgress: (String) -> Unit): CommandResult {
        Logger.log(processBuilder.command().joinToString(" "))
        val process = processBuilder.start()
        try {
            val output = StringBuilder()
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    Logger.info(line)
                    onProgress(line)
                    output.append(line).append('\n')
                }
            }
            val exitCode = process.waitFor()
            Logger.log("Exited with code: $exitCode")
            return if (exitCode == 0) CommandResult.Success(output.toString())
            else CommandResult.Error("exit code: $exitCode\n$output")
        } finally {
            process.destroy()
        }
    }

    /**
     * 获取Apk文件使用的签名
     * @param apkFilePath 签名的apk文件绝对路径
     * @return 返回结果 Pair(是否成功，失败原因或输出文件路径)
     */
    fun getApkSignInfo(
        apkFilePath: String,
    ): CommandResult {

        if (!isInitialized()) {
            return CommandResult.Error("请先初始化相关配置")
        }

        return if (apkFilePath.isNotEmpty()) {
            RunCommandUtil.runCommandWithResult(
                apkSignerCmdPath,
                "verify",
                "-v",
                "--print-certs",
                apkFilePath,
                javaHome = javaHomePath
            )
        } else {
            CommandResult.Error("apk文件路径不能为空")
        }
    }
}

