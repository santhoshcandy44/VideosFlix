package com.flix.videos.ui.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp

@Composable
fun SwingEdgeToEdgeEffect(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    var offsetY by remember { mutableStateOf(0f) }
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
                            offsetY += available.y
                        }

                        if (available.y > 0 && consumed.y <= 0) {
                            offsetY += available.y
                        }
                        return super.onPostScroll(consumed, available, source)
                    }

                    override suspend fun onPostFling(
                        consumed: Velocity,
                        available: Velocity
                    ): Velocity {
                        offsetY = 0f
                        return super.onPostFling(consumed, available)
                    }
                }
            })
            .offset(0.dp, with(density) { offsetY.toDp() })
            .then(modifier)
    ) {
        content()
    }
}