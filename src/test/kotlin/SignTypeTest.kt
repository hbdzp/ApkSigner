import io.github.jixiaoyong.beans.SignType
import java.net.URLClassLoader
import kotlin.test.*

class SignTypeTest {
    @Test fun `scheme lists are valid when a scheme is initialized first`() {
        val urls = arrayOf(SignType::class.java.protectionDomain.codeSource.location,
            Unit::class.java.protectionDomain.codeSource.location)
        URLClassLoader(urls, ClassLoader.getPlatformClassLoader()).use { loader ->
            Class.forName("io.github.jixiaoyong.beans.SignType\$V1", true, loader)
            val type = loader.loadClass("io.github.jixiaoyong.beans.SignType")
            val companion = type.getField("Companion").get(null)
            val all = companion.javaClass.getMethod("getALL_SIGN_TYPES").invoke(companion) as List<*>
            assertEquals(4, all.size)
            assertTrue(all.all { it != null }, "Scheme initialization must not insert null into the list")
        }
    }
}
