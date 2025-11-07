package ai.codeeditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun MouseDebugView() {
    var clickCount by remember { mutableStateOf(0) }
    var lastClickPos by remember { mutableStateOf("None") }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.DarkGray)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    clickCount++
                    lastClickPos = "(${offset.x.toInt()}, ${offset.y.toInt()})"
                }
            }
    ) {
        Text(
            text = "Mouse Debug\nClicks: $clickCount\nLast: $lastClickPos",
            color = Color.White
        )
    }
}
