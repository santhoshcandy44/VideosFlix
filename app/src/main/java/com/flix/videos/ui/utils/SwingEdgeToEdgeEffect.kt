package com.flix.videos.ui.utils

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
fun SwingEdgeToEdgeEffect(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(remember {
                object : NestedScrollConnection {
                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        if (available.y < 0 && consumed.y <= 0) {
                            scope.launch {
                                offsetY.snapTo(offsetY.value + available.y)
                            }
                        }

                        if (available.y > 0 && consumed.y <= 0) {
                            scope.launch {
                                offsetY.snapTo(offsetY.value + available.y)
                            }
                        }
                        if(consumed.y.absoluteValue > 0){
                            scope.launch {
                                offsetY.snapTo(0f)
                            }
                        }
                        Log.e("Scrolling","${consumed.y}")
                        return super.onPostScroll(consumed, available, source)
                    }

                    override suspend fun onPostFling(
                        consumed: Velocity,
                        available: Velocity
                    ): Velocity {
                        offsetY.animateTo(0f, tween(200))
                        return super.onPostFling(consumed, available)
                    }
                }
            })
            .offset {
                IntOffset(
                    x = 0,
                    y = offsetY.value.roundToInt()
                )
            }
            .then(modifier)
    ) {
        content()
    }
}