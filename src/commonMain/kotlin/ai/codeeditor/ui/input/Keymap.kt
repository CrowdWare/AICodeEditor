package ai.codeeditor.ui.input

import ai.codeeditor.core.EditorController
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString

/**
 * Keymap handles keyboard input and translates to editor operations.
 * ASCII-only for now; IME support comes later.
 */
object Keymap {

    fun handleKeyEvent(
        event: KeyEvent, 
        controller: EditorController,
        clipboardManager: ClipboardManager? = null
    ): Boolean {
        if (event.type != KeyEventType.KeyDown) return false

        val shift = event.isShiftPressed
        val ctrl = event.isCtrlPressed
        val meta = event.isMetaPressed
        val alt = event.isAltPressed
        val cmdOrCtrl = ctrl || meta

        return when (event.key) {
            Key.Enter -> {
                controller.insertNewLine()
                true
            }
            Key.Backspace -> {
                controller.deleteBackward()
                true
            }
            Key.Delete -> {
                controller.deleteForward()
                true
            }
            Key.DirectionLeft -> {
                controller.moveLeft(shift)
                true
            }
            Key.DirectionRight -> {
                controller.moveRight(shift)
                true
            }
            Key.DirectionUp -> {
                controller.moveUp(shift)
                true
            }
            Key.DirectionDown -> {
                controller.moveDown(shift)
                true
            }
            Key.MoveHome -> {
                controller.moveLineStart(shift)
                true
            }
            Key.MoveEnd -> {
                controller.moveLineEnd(shift)
                true
            }
            Key.PageUp -> {
                controller.movePage(-controller.state.viewportLines, 20f, shift)
                true
            }
            Key.PageDown -> {
                controller.movePage(controller.state.viewportLines, 20f, shift)
                true
            }
            Key.Escape -> {
                controller.clearSelection()
                true
            }
            Key.Tab -> {
                controller.insertText("    ")
                true
            }
            else -> {
                // Handle clipboard and editing shortcuts FIRST
                if (cmdOrCtrl) {
                    when (event.key) {
                        Key.Z -> {
                            if (shift) {
                                // Cmd/Ctrl+Shift+Z = Redo
                                controller.redo()
                            } else {
                                // Cmd/Ctrl+Z = Undo
                                controller.undo()
                            }
                            return true
                        }
                        Key.Y -> {
                            // Cmd/Ctrl+Y = Redo (alternative)
                            controller.redo()
                            return true
                        }
                        Key.A -> {
                            controller.selectAll()
                            return true
                        }
                        Key.C -> {
                            if (clipboardManager != null) {
                                val selectedText = controller.getSelectedText()
                                if (selectedText.isNotEmpty()) {
                                    clipboardManager.setText(AnnotatedString(selectedText))
                                }
                            }
                            return true
                        }
                        Key.X -> {
                            if (clipboardManager != null) {
                                val selectedText = controller.getSelectedText()
                                if (selectedText.isNotEmpty()) {
                                    clipboardManager.setText(AnnotatedString(selectedText))
                                    controller.insertText("")
                                }
                            }
                            return true
                        }
                        Key.V -> {
                            if (clipboardManager != null) {
                                val text = clipboardManager.getText()?.text
                                if (!text.isNullOrEmpty()) {
                                    controller.insertText(text)
                                }
                            }
                            return true
                        }
                    }
                }
                
                // Handle printable characters (but not if it's a Cmd/Ctrl command)
                if (!cmdOrCtrl && event.utf16CodePoint != 0) {
                    val codePoint = event.utf16CodePoint
                    
                    // Filter out invalid/placeholder characters
                    // 0xFFFF = Invalid/no character (Option/Fn alone)
                    // 0xFFFD = Replacement character "�"
                    // 0xE000-0xF8FF = Private use area
                    if (codePoint == 0xFFFF || codePoint == 0xFFFD || codePoint in 0xE000..0xF8FF) {
                        return false
                    }
                    
                    val char = codePoint.toChar()
                    // Only insert if it's truly a printable character
                    // Ignore control characters but allow Alt/Option for special chars on Mac
                    if (char >= ' ' || char == '\t') {
                        controller.insertText(char.toString())
                        return true
                    }
                }
                false
            }
        }
    }
}
