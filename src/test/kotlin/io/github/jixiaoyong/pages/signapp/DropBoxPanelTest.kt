package io.github.jixiaoyong.pages.signapp

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DropBoxPanelTest {
    @Test fun `reads file paths with spaces from a file drop`() {
        val directory = File("/tmp/Android SDK/build-tools")
        val transferable = object : Transferable {
            override fun getTransferDataFlavors() = arrayOf(DataFlavor.javaFileListFlavor)
            override fun isDataFlavorSupported(flavor: DataFlavor) = flavor == DataFlavor.javaFileListFlavor
            override fun getTransferData(flavor: DataFlavor): Any = listOf(directory)
        }
        assertEquals(listOf(directory.absolutePath), droppedFilePaths(transferable))
    }

    @Test fun `ignores non-file drops`() {
        assertTrue(droppedFilePaths(StringSelection("not a file")).isEmpty())
    }
}
