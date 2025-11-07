package ai.codeeditor.desktop

import ai.codeeditor.core.EditorController
import ai.codeeditor.core.SimpleTextBuffer
import ai.codeeditor.ui.EditorView
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "AICodeEditor - Canvas Based Editor",
        state = rememberWindowState(width = 1200.dp, height = 800.dp)
    ) {
        var refreshKey by remember { mutableStateOf(0) }
        
        val buffer = remember {
            SimpleTextBuffer(
                """
                // AICodeEditor - Canvas + Paragraph Rendering
                // No BasicTextField - Full Control
                
                fun main() {
                    println("Hello from AICodeEditor!")
                    val editor = "Compose Multiplatform"
                    val rendering = "Canvas + Skia Paragraph"
                    
                    // Features:
                    // - Direct text rendering with Paragraph
                    // - Custom cursor and selection
                    // - Viewport-based line rendering
                    // - Full keyboard control
                    // - Line numbers
                    
                    for (i in 1..10) {
                        println("Line ${'$'}i")
                    }
                }
                
                class TextBuffer {
                    private val content = StringBuilder()
                    private val lines = LineIndex()
                    
                    fun insert(offset: Int, text: String) {
                        content.insert(offset, text)
                        lines.update(offset, text)
                    }
                }
                """.trimIndent()
            )
        }
        
        val controller = remember(buffer) {
            EditorController(
                buffer = buffer,
                onStateChange = { refreshKey++ }
            )
        }
        
        key(refreshKey) {
            EditorView(controller = controller)
        }
    }
}
