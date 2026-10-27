import io.github.jixiaoyong.beans.CommandResult
import java.io.File

/**
 * @author : jixiaoyong
 * @description ：运行本机脚本
 * @email : jixiaoyong1995@gmail.com
 * @date : 3/10/2023
 */
object RunCommandUtil {

    /**
     * 执行程序，路径和每个参数必须分别传入（不经过 shell）。
     * @return 成功返回null，失败返回Exception类型错误原因
     */
    fun runCommand(
        vararg command: String,
        tag: String = "",
        printLog: Boolean = true,
        printError: Boolean = true,
        javaHome: String? = null
    ): Exception? {
        return when (val result = runCommandWithResult(*command, tag = tag,
            printLog = printLog, printError = printError, javaHome = javaHome)) {
            is CommandResult.Success<*> -> null
            is CommandResult.Error<*> -> Exception(result.message.toString(), result.error)
            else -> error("Unexpected command state")
        }
    }

    fun runCommandWithResult(
        vararg command: String,
        tag: String = "",
        printLog: Boolean = true,
        printError: Boolean = true,
        javaHome: String? = null
    ): CommandResult {
        val logTag = if (tag.isBlank()) "" else "$tag: "
        if (printLog) Logger.info("$tag command: ${command.joinToString(" ")}")

        var process: Process? = null
        return try {
            val running = createProcessBuilder(command.toList(), javaHome).start()
            process = running
            // Always drain the merged stream, including when logging is disabled.
            // Retain it here so callers never try to read an already-consumed stream.
            val output = running.inputStream.bufferedReader().use { it.readText() }
            val exitCode = running.waitFor()
            if (exitCode == 0) {
                if (printLog && output.isNotBlank()) Logger.info("$logTag$output")
                CommandResult.Success(output)
            } else {
                if (printError && output.isNotBlank()) Logger.error("$logTag$output")
                CommandResult.Error("${logTag}exit code: $exitCode\n$output")
            }
        } catch (e: Exception) {
            if (printError) Logger.error("$tag error: $e")
            CommandResult.Error("$tag error: $e", e)
        } finally {
            process?.destroy()
        }
    }

    fun createProcessBuilder(command: List<String>? = null, javaHome: String? = null): ProcessBuilder {
        val processBuilder = ProcessBuilder(command ?: emptyList())
        processBuilder.directory(File(System.getProperty("user.dir")))
        processBuilder.redirectErrorStream(true)
        if (javaHome != null) {
            processBuilder.environment()["JAVA_HOME"] = javaHome
        }
        return processBuilder
    }
}
