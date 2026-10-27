package io.github.jixiaoyong.pages.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContains
import androidx.compose.ui.graphics.Color
import io.github.jixiaoyong.i18n.ZhStrings
import io.github.jixiaoyong.i18n.EnStrings

class SettingsLinksTest {
    @Test fun `latest version link points to the maintained fork`() {
        assertEquals("https://github.com/hbdzp/ApkSigner", PROJECT_WEBSITE)
    }

    @Test fun `both languages expose separate clickable fork and upstream links`() {
        for (strings in listOf(ZhStrings, EnStrings)) {
            val intro = buildSettingsIntro(strings, "1.3.0", Color.Blue)
            assertContains(intro.text, "1.3.0")
            for (url in listOf(PROJECT_WEBSITE, UPSTREAM_WEBSITE)) {
                val position = intro.text.indexOf(url)
                assertEquals(url, intro.getStringAnnotations("URL", position, position + url.length).single().item)
            }
        }
    }
}
