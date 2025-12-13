package com.flix.videos.ui.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.crop(horizontal: Dp = 0.dp, vertical: Dp = 0.dp) =
    this.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        fun Dp.toPxInt(): Int = this.toPx().toInt()
        layout(
            placeable.width - (horizontal * 2).toPxInt(),
            placeable.height - (vertical * 2).toPxInt()
        ) {
            placeable.placeRelative(-horizontal.toPx().toInt(), -vertical.toPx().toInt())
        }
    }

@Composable
fun Modifier.noRippleClickable(onClick:()-> Unit) = this.clickable(onClick = onClick, interactionSource = remember { MutableInteractionSource() }, indication = null)