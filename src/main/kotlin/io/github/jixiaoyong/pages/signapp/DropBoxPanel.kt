package io.github.jixiaoyong.pages.signapp

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.io.File

internal fun droppedFilePaths(transferable: Transferable): List<String> {
    if (!transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) return emptyList()
    return runCatching {
        (transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>)
            ?.filterIsInstance<File>()?.map { it.absolutePath }.orEmpty()
    }.getOrDefault(emptyList())
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun DropBoxPanel(
    modifier: Modifier = Modifier,
    onFileDrop: (List<String>) -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val currentOnFileDrop by rememberUpdatedState(onFileDrop)
    val target = remember {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val paths = droppedFilePaths(event.awtTransferable)
                if (paths.isEmpty()) return false
                currentOnFileDrop(paths)
                return true
            }
        }
    }
    Box(
        modifier = modifier.dragAndDropTarget(
            shouldStartDragAndDrop = { it.awtTransferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor) },
            target = target
        ),
        content = content
    )
}
