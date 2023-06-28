package com.ryanharter.drawingtest

import android.graphics.RectF
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ryanharter.drawingtest.ui.theme.DrawingTestTheme
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DrawingTestTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    var drawing by remember { mutableStateOf(Drawing()) }
                    DrawingCanvas(
                        value = drawing,
                        onValueChanged = { drawing = it },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}


@Immutable
data class Drawing(val strokes: List<List<Offset>> = emptyList()) {
    val size: Size by lazy {
        Size(
            width = strokes.flatten().maxOf { it.x } - strokes.flatten().minOf { it.x },
            height = strokes.flatten().maxOf { it.y } - strokes.flatten().minOf { it.y },
        )
    }

    val bounds: RectF by lazy {
        RectF().apply {
            strokes.flatten().forEach { offset ->
                left = minOf(left, offset.x)
                top = minOf(top, offset.y)
                right = maxOf(right, offset.x)
                bottom = maxOf(bottom, offset.y)
            }
        }
    }
}

@Preview
@Composable
fun TestPreview() {
    DrawingTestTheme {
        Surface {
            var drawing by remember { mutableStateOf(Drawing()) }
            DrawingCanvas(
                value = drawing,
                onValueChanged = { drawing = it },
                modifier = Modifier.size(400.dp, 260.dp),
            )
        }
    }
}

@Composable
fun DrawingCanvas(
    value: Drawing,
    onValueChanged: (Drawing) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentStroke = remember { mutableStateListOf<Offset>() }

    //////////////
    // I assume the issue is in here!
    //////////////
    val internalDrawing by remember {
        derivedStateOf {
            Drawing(
                strokes = mutableListOf<List<Offset>>().apply {
                    addAll(value.strokes)
                    add(currentStroke)
                }
            )
        }
    }

    BoxWithConstraints(modifier.background(Color.Red.copy(alpha = 0.2f))) {
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else constraints.minWidth
        val height = if (constraints.hasBoundedHeight) constraints.maxHeight else constraints.minHeight
        Drawing(
            internalDrawing,
            contentSize = Size(width.toFloat(), height.toFloat()),
            modifier = Modifier
                .size(width.dp, height.dp)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            when (event.type) {
                                PointerEventType.Press,
                                PointerEventType.Move,
                                -> {
                                    event.changes.forEach { e ->
                                        val (x, y) = e.position
                                        val adjOffset = Offset(x, size.height - y)
                                        currentStroke.add(adjOffset)
                                        e.consume()
                                    }
                                }

                                PointerEventType.Release -> {
                                    val drawing = Drawing(
                                        strokes = mutableListOf<List<Offset>>().apply {
                                            addAll(value.strokes)
                                            add(currentStroke)
                                        }
                                    )
                                    currentStroke.clear()

                                    onValueChanged(drawing)
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                    }
                },
        )
    }
}

@Composable fun Drawing(
    drawing: Drawing,
    modifier: Modifier = Modifier,
    contentSize: Size = drawing.size,
) {
    Box(
        modifier = modifier
            .drawWithCache {
                val path = Path()
                drawing.strokes.forEach { line ->
                    var first = true
                    for (point in line) {
                        // TODO map the points to the canvas space
                        val offset = Offset(
                            (point.x / contentSize.width) * size.width,
                            size.height - (point.y / contentSize.height) * size.height,
                        )

                        if (first) {
                            first = false
                            path.moveTo(offset.x, offset.y)
                        } else {
                            path.lineTo(offset.x, offset.y)
                        }
                    }
                }

                onDrawBehind {
                    // Debug background
                    drawRect(
                        Color.Cyan,
                        alpha = 0.4f,
                        size = Size(size.width, size.height)
                    )

                    for (y in 0 until size.height.roundToInt() step 10) {
                        drawLine(Color.Cyan, start = Offset(0f, y.toFloat()), end = Offset(size.width, y.toFloat()))
                    }
                    for (x in 0 until size.width.roundToInt() step 10) {
                        drawLine(Color.Cyan, start = Offset(x.toFloat(), 0f), end = Offset(x.toFloat(), size.height))
                    }

                    drawPath(path, Color.Black, style = Stroke(width = 4f))
                }
            },
    )
}
