import io.github.jixiaoyong.beans.CommandResult
import java.io.File
import java.nio.file.Files
import kotlin.test.*
import org.junit.Assume.assumeFalse

class RunCommandUtilTest {
    private lateinit var directory: File

    @BeforeTest fun setUp() {
        assumeFalse(System.getProperty("os.name").contains("Windows"))
        directory = Files.createTempDirectory("command tests ").toFile()
    }

    @AfterTest fun tearDown() {
        if (::directory.isInitialized) directory.deleteRecursively()
    }

    @Test fun `preserves stdout after logging`() {
        val script = script("printf 'tool output'")
        val result = RunCommandUtil.runCommandWithResult(script.path)
        assertEquals("tool output", assertIs<CommandResult.Success<*>>(result).result.toString().trim())
    }

    @Test fun `preserves stderr on nonzero exit`() {
        val script = script("printf 'invalid keystore' >&2\nexit 7")
        val result = assertIs<CommandResult.Error<*>>(RunCommandUtil.runCommandWithResult(script.path))
        assertContains(result.message.toString(), "invalid keystore")
        assertContains(result.message.toString(), "7")
    }

    @org.junit.Test(timeout = 10000) fun `quiet execution still drains large output`() {
        val script = script("i=0; while [ \"\$i\" -lt 10000 ]; do echo output; i=\$((i+1)); done")
        val result = RunCommandUtil.runCommandWithResult(script.path, printLog = false, printError = false)
        assertEquals(10000, assertIs<CommandResult.Success<*>>(result).result.toString().lineSequence().count { it == "output" })
    }

    @Test fun `arguments and JAVA_HOME survive spaces`() {
        val script = script("printf '%s|%s' \"\$1\" \"\$JAVA_HOME\"")
        val result = RunCommandUtil.runCommandWithResult(script.path, "argument with spaces", javaHome = "/jdk with spaces")
        assertEquals("argument with spaces|/jdk with spaces", assertIs<CommandResult.Success<*>>(result).result)
    }

    private fun script(body: String): File = File(directory, "tool with spaces").apply {
        writeText("#!/bin/sh\n$body\n")
        check(setExecutable(true))
    }
}
