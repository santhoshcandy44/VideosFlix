package com.flix.videos.ui.app.player

import android.content.pm.ActivityInfo
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flix.videos.ui.utils.NoIndicationInteractionSource
import com.flix.videos.ui.utils.findActivity

@Composable
fun BoxScope.LockedButton(onClick:()-> Unit){
    IconButton(
        onClick = onClick,
        interactionSource = remember { NoIndicationInteractionSource() },
        modifier = Modifier
            .padding(16.dp)
            .align(Alignment.TopStart)
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = "Orientation rotate",
        )
    }
}