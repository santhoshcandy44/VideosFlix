package com.flix.videos.ui.app.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forward10
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.flix.videos.ui.app.player.ExoplayerSeekDirection.SEEK_BACKWARD
import com.flix.videos.ui.app.player.ExoplayerSeekDirection.SEEK_FORWARD
import com.flix.videos.ui.app.player.common.isLandscape

object ExoplayerSeekDirection {
    const val SEEK_NONE = 0
    const val SEEK_FORWARD = 1
    const val SEEK_BACKWARD = -1
}

@Composable
fun BoxScope.TapToSeekController(
    seekDirection:Int
){
    if (seekDirection == SEEK_BACKWARD) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.3f)
                .then(if(isLandscape()) Modifier.fillMaxHeight() else Modifier.height(300.dp))
                .drawBehind {
                    val radius = size.height / 2f
                    drawArc(
                        color = Color.Black.copy(0.7f),
                        startAngle = 90f,
                        sweepAngle = -180f,
                        useCenter = true,
                        topLeft = Offset( - radius, 0f),
                        size = Size(radius * 2, size.height)
                    )
                }
                .align(Alignment.CenterStart)
        ) {
            Column(
                modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.Center),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Outlined.Replay10,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
                Text("-10s")
            }
        }
    } else if (seekDirection == SEEK_FORWARD) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.3f)
                .then(if(isLandscape()) Modifier.fillMaxHeight() else Modifier.height(300.dp))
                .drawBehind {
                    val radius = size.height / 2f
                    drawArc(
                        color = Color.Black.copy(0.7f),
                        startAngle = 90f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset(size.width - radius, 0f),
                        size = Size(radius * 2, size.height)
                    )
                }
                .align(Alignment.CenterEnd)
        ) {
            Column(
                modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.Center),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Outlined.Forward10,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
                Text("+10s")
            }
        }
    }
}

fun HalfCircleLeftShape() = GenericShape { size, _ ->
    val width = size.width
    val height = size.height
    val radius = height / 2f

    // ARC bounds: making a perfect half circle
    val rect = Rect(
        left = width - radius * 2f,
        top = 0f,
        right = width,
        bottom = height
    )

    // Start at top-right
    moveTo(width, 0f)
    // Draw top → arc → bottom
    arcTo(
        rect = rect,
        startAngleDegrees = -90f,
        sweepAngleDegrees = -180f,
        forceMoveTo = false
    )
    // Back to bottom-right
    lineTo(width, height)

    close()
}
