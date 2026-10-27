import io.github.jixiaoyong.beans.CommandResult
import io.github.jixiaoyong.beans.SignType
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.file.Files
import kotlin.test.*
import kotlinx.coroutines.test.runTest
import org.junit.Assume.assumeFalse

class ApkSignerTest {
    private lateinit var directory: File
    private lateinit var sdk: File
    private lateinit var apk: File

    @BeforeTest fun setUp() {
        assumeFalse(System.getProperty("os.name").contains("Windows"))
        directory = Files.createTempDirectory("signer tests ").toFile()
        sdk = File(directory, "sdk").apply { mkdirs() }
        apk = File(directory, "input with spaces.apk").apply { writeText("input") }
        assertNull(ApkSigner.setupJavaHome(System.getProperty("java.home")))
    }

    @AfterTest fun tearDown() {
        ApkSigner.setupJavaHome(System.getProperty("java.home"))
        if (::directory.isInitialized) directory.deleteRecursively()
    }

    @Test fun `checks tool with configured Java in path containing spaces`() {
        val jdk = File(directory, "portable jdk")
        executable(File(jdk, "bin/java"), "exit 0")
        File(jdk, "include").mkdir()
        assertNull(ApkSigner.setupJavaHome(jdk.path))
        val tool = executable(File(sdk, "apksigner"), "[ \"\$JAVA_HOME\" = '${jdk.path}' ] || exit 4\necho 0.9")
        assertNull(ApkSigner.setupApkSigner(tool.path))
    }

    @Test fun `accepts java exe when configuring Windows JDK`() {
        val jdk = File(directory, "windows jdk")
        executable(File(jdk, "bin/java.exe"), "exit 0")
        File(jdk, "include").mkdir()
        val actualOs = System.getProperty("os.name")
        try {
            System.setProperty("os.name", "Windows 11")
            assertNull(ApkSigner.setupJavaHome(jdk.path))
        } finally {
            System.setProperty("os.name", actualOs)
        }
    }

    @Test fun `missing native tool is rejected`() {
        assertNotNull(ApkSigner.setupZipAlign(File(directory, "missing").path))
        assertNotNull(ApkSigner.setupAapt(File(directory, "missing").path))
    }

    @Test fun `Windows also rejects missing native tools`() {
        val actualOs = System.getProperty("os.name")
        try {
            System.setProperty("os.name", "Windows 11")
            assertNotNull(ApkSigner.setupZipAlign(File(directory, "missing.exe").path))
            assertNotNull(ApkSigner.setupAapt(File(directory, "missing.exe").path))
        } finally {
            System.setProperty("os.name", actualOs)
        }
    }

    @Test fun `retains verification output`() {
        configure()
        val result = ApkSigner.getApkSignInfo(apk.path)
        assertContains(assertIs<CommandResult.Success<*>>(result).result.toString(), "Verified using v1 scheme: true")
    }

    @Test fun `extracts package without shell or grep`() = runTest {
        configure()
        assertEquals("io.example.app", ApkSigner.getApkPackageName(apk.path))
    }

    @Test fun `alignment launch failure never reports success`() {
        configure()
        File(sdk, "zipalign").delete()
        assertIs<CommandResult.Error<*>>(sign(zipAlign = true))
        assertFalse(File(directory, "sign-called").exists())
    }

    @Test fun `alignment nonzero exit aborts signing`() {
        configure(zipBody = "echo alignment-failed >&2\nexit 9")
        val result = assertIs<CommandResult.Error<*>>(sign(zipAlign = true))
        assertContains(result.message.toString(), "alignment-failed")
        assertFalse(File(directory, "sign-called").exists())
    }

    @Test fun `missing signed output is an error`() {
        configure(createOutput = false)
        assertIs<CommandResult.Error<*>>(sign())
    }

    @Test fun `old signed output is preserved when signer creates nothing`() {
        configure(createOutput = false)
        val previous = File(directory, "input with spaces_signed.apk").apply { writeText("previous") }
        assertIs<CommandResult.Error<*>>(sign())
        assertEquals("previous", previous.readText())
    }

    @Test fun `v4 sidecar is saved next to signed APK`() {
        configure(createSidecar = true)
        val result = sign(types = listOf(SignType.V2, SignType.V4))
        val signedPath = assertIs<CommandResult.Success<*>>(result).result.toString()
        assertEquals("v4", File("$signedPath.idsig").readText())
    }

    @Test fun `missing v4 sidecar leaves previous signed APK untouched`() {
        configure()
        val previous = File(directory, "input with spaces_signed.apk").apply { writeText("previous") }
        val result = sign(types = listOf(SignType.V2, SignType.V4))
        assertIs<CommandResult.Error<*>>(result)
        assertEquals("previous", previous.readText())
    }

    @Test fun `resigning without v4 removes obsolete sidecar`() {
        configure(createSidecar = true)
        val first = assertIs<CommandResult.Success<*>>(sign(types = listOf(SignType.V2, SignType.V4)))
        val sidecar = File("${first.result}.idsig")
        assertTrue(sidecar.exists())
        configure()
        assertIs<CommandResult.Success<*>>(sign())
        assertFalse(sidecar.exists(), "A sidecar for the previous APK must not remain")
    }

    @Test fun `batch rejects colliding output names`() = runTest {
        configure()
        val other = File(directory, "other/${apk.name}").apply { parentFile.mkdirs(); writeText("another APK") }
        val results = ApkSigner.alignAndSignApk(listOf(apk.path, other.path), "test.jks", "test",
            "secret-store", "secret-key", signedApkDirectory = directory.path,
            zipAlign = false, signVersions = listOf(SignType.V1), onProgress = {})
        assertEquals(2, results.size)
        assertTrue(results.all { it is CommandResult.Error<*> }, "Neither colliding APK may overwrite the other")
        assertFalse(File(directory, "sign-called").exists())
    }

    @Test fun `uppercase APK input is preserved`() {
        configure()
        val uppercase = File(directory, "input.APK").apply { writeText("preserved input") }
        val result = ApkSigner.alignAndSignApk(uppercase.path, "test.jks", "test", "secret-store", "secret-key",
            zipAlign = false, signVersions = listOf(SignType.V1), onProgress = {})
        assertIs<CommandResult.Success<*>>(result)
        assertEquals("preserved input", uppercase.readText())
        assertEquals(File(directory, "input_signed.apk").path, result.result)
    }

    @Test fun `uses environment passwords and disables unselected schemes`() {
        configure()
        val bytes = ByteArrayOutputStream()
        val original = System.out
        val result = try {
            System.setOut(PrintStream(bytes))
            sign()
        } finally {
            System.setOut(original)
        }
        assertIs<CommandResult.Success<*>>(result)
        val args = File(directory, "arguments").readLines()
        assertEquals("false", args[args.indexOf("--v3-signing-enabled") + 1])
        assertEquals("true", args[args.indexOf("--v1-signing-enabled") + 1])
        assertTrue(args[args.indexOf("--ks-pass") + 1].startsWith("env:"))
        assertTrue(args[args.indexOf("--key-pass") + 1].startsWith("env:"))
        assertFalse(args.joinToString().contains("secret-store"))
        assertFalse(bytes.toString().contains("secret-store"))
        assertFalse(bytes.toString().contains("secret-key"))
        assertEquals("secret-store|secret-key", File(directory, "passwords-received").readText())
    }

    @Test fun `old signer receives only supported scheme flags`() {
        configure(schemes = listOf(1, 2))
        assertIs<CommandResult.Success<*>>(sign())
        assertFalse(File(directory, "arguments").readText().contains("--v3-signing-enabled"))
    }

    @Test fun `unsupported selected scheme is a clear error`() {
        configure(schemes = listOf(1, 2))
        val result = sign(types = listOf(SignType.V3))
        assertIs<CommandResult.Error<*>>(result)
        assertContains(result.message.toString(), "V3")
        assertFalse(File(directory, "sign-called").exists())
    }

    @Test fun `alignment preserves existing similarly named file`() {
        configure()
        val existing = File(directory, "input with spaces_aligned.apk").apply { writeText("keep") }
        assertIs<CommandResult.Success<*>>(sign(zipAlign = true))
        assertEquals("keep", existing.readText())
        assertTrue(directory.listFiles()!!.none { it.name.startsWith("apksigner-aligned-") })
    }

    private fun sign(zipAlign: Boolean = false, types: List<SignType> = listOf(SignType.V1)): CommandResult =
        ApkSigner.alignAndSignApk(apk.path, "test.jks", "test", "secret-store", "secret-key",
            zipAlign = zipAlign, signVersions = types, onProgress = {})

    private fun configure(
        zipBody: String = "for arg do out=\"\$arg\"; done\nprintf aligned > \"\$out\"",
        schemes: List<Int> = listOf(1, 2, 3, 4),
        createOutput: Boolean = true,
        createSidecar: Boolean = false
    ) {
        val tool = executable(File(sdk, "apksigner"), """
            if [ "${'$'}1" = version ]; then echo 0.9; exit 0; fi
            if [ "${'$'}2" = --help ]; then
              echo '${schemes.joinToString(" ") { "--v$it-signing-enabled" }}'
              exit 0
            fi
            if [ "${'$'}1" = verify ]; then echo 'Verified using v1 scheme: true'; exit 0; fi
            touch '${directory.path}/sign-called'
            printf '%s\n' "${'$'}@" > '${directory.path}/arguments'
            previous=
            for arg do
              if [ "${'$'}previous" = --ks-pass ]; then storeRef="${'$'}{arg#env:}"; fi
              if [ "${'$'}previous" = --key-pass ]; then keyRef="${'$'}{arg#env:}"; fi
              if [ "${'$'}previous" = --out ]; then out="${'$'}arg"; fi
              previous="${'$'}arg"
            done
            storePwd=${'$'}(printenv "${'$'}storeRef")
            keyPwd=${'$'}(printenv "${'$'}keyRef")
            printf '%s|%s' "${'$'}storePwd" "${'$'}keyPwd" > '${directory.path}/passwords-received'
            ${if (createOutput) "printf signed > \"${'$'}out\"" else ":"}
            ${if (createSidecar) "printf v4 > \"${'$'}out.idsig\"" else ":"}
        """.trimIndent())
        executable(File(sdk, "zipalign"), zipBody)
        executable(File(sdk, "aapt"), "[ \"\$1\" = dump ] || exit 2\necho \"package: name='io.example.app' versionCode='1'\"")
        assertNull(ApkSigner.setupApkSigner(tool.path))
        assertNull(ApkSigner.setupZipAlign(File(sdk, "zipalign").path))
        assertNull(ApkSigner.setupAapt(File(sdk, "aapt").path))
    }

    private fun executable(file: File, body: String): File = file.apply {
        parentFile.mkdirs()
        writeText("#!/bin/sh\n# The Android Open Source Project\n$body\n")
        check(setExecutable(true))
    }
}
