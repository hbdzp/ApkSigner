import io.github.jixiaoyong.beans.CommandResult
import io.github.jixiaoyong.beans.SignType
import java.io.File
import java.nio.file.Files
import kotlin.test.*
import kotlinx.coroutines.test.runTest
import org.junit.Assume.assumeTrue

/** Opt in with APKSIGNER_TEST_BUILD_TOOLS and APKSIGNER_TEST_ANDROID_JAR. */
class RealApkSignerIntegrationTest {
    @Test fun `signs verifies and inspects a real APK`() = runTest {
        val toolsPath = System.getenv("APKSIGNER_TEST_BUILD_TOOLS")
        val androidJar = System.getenv("APKSIGNER_TEST_ANDROID_JAR")
        assumeTrue("Android SDK integration fixtures not configured", toolsPath != null && androidJar != null)
        val tools = File(toolsPath!!)
        val windows = System.getProperty("os.name").contains("Windows")
        val directory = Files.createTempDirectory("real signing test ").toFile()
        val password = "regression P@ss%&^word"
        try {
            val manifest = File(directory, "AndroidManifest.xml").apply {
                writeText("""
                    <manifest xmlns:android="http://schemas.android.com/apk/res/android" package="io.example.signingtest" android:versionCode="1" android:versionName="1.0">
                        <uses-sdk android:minSdkVersion="21" android:targetSdkVersion="28" />
                        <application android:hasCode="false" android:label="Signing test" />
                    </manifest>
                """.trimIndent())
            }
            val unsigned = File(directory, "unsigned app.apk")
            runTool(listOf(File(tools, if (windows) "aapt.exe" else "aapt").path,
                "package", "-f", "-M", manifest.path, "-I", androidJar!!, "-F", unsigned.path))
            val keystore = File(directory, "test key.p12")
            val keytool = File(System.getProperty("java.home"), if (windows) "bin/keytool.exe" else "bin/keytool")
            runTool(listOf(keytool.path, "-genkeypair", "-alias", "regression", "-keyalg", "RSA",
                "-keysize", "2048", "-validity", "1", "-dname", "CN=Regression",
                "-storetype", "PKCS12", "-keystore", keystore.path,
                "-storepass:env", "APKSIGNER_TEST_PASSWORD", "-keypass:env", "APKSIGNER_TEST_PASSWORD"),
                mapOf("APKSIGNER_TEST_PASSWORD" to password))
            assertNull(ApkSigner.setupJavaHome(System.getProperty("java.home")))
            assertNull(ApkSigner.init(tools.path))
            assertEquals("io.example.signingtest", ApkSigner.getApkPackageName(unsigned.path))

            for (schemes in listOf(listOf(SignType.V1), listOf(SignType.V1, SignType.V2),
                listOf(SignType.V1, SignType.V2, SignType.V4))) {
                val signed = ApkSigner.alignAndSignApk(unsigned.path, keystore.path, "regression", password, password,
                    zipAlign = true, signVersions = schemes, onProgress = {})
                assertTrue(signed is CommandResult.Success<*>, (signed as? CommandResult.Error<*>)?.message.toString())
                val verification = ApkSigner.getApkSignInfo(signed.result.toString())
                val details = assertIs<CommandResult.Success<*>>(verification).result.toString()
                assertTrue(Regex("Verified using v1 scheme.*: true").containsMatchIn(details), details)
                assertTrue(Regex("Verified using v2 scheme.*: ${SignType.V2 in schemes}").containsMatchIn(details), details)
                assertTrue(Regex("Verified using v3 scheme.*: false").containsMatchIn(details), details)
                if (SignType.V4 in schemes) {
                    assertTrue(File("${signed.result}.idsig").length() > 0L, "V4 sidecar is missing")
                }
            }
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun runTool(arguments: List<String>, environment: Map<String, String> = emptyMap()) {
        val builder = ProcessBuilder(arguments).redirectErrorStream(true)
        builder.environment().putAll(environment)
        val process = builder.start()
        try {
            val output = process.inputStream.bufferedReader().use { it.readText() }
            assertEquals(0, process.waitFor(), output)
        } finally {
            process.destroy()
        }
    }
}
