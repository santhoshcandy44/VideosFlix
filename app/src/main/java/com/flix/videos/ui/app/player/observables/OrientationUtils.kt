package com.flix.videos.ui.app.player.observables

import android.content.res.Configuration
import android.view.OrientationEventListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun rememberDeviceOrientationFlow(): StateFlow<Int> {
    val context = LocalContext.current

    val orientationFlow = remember { MutableStateFlow(Configuration.ORIENTATION_UNDEFINED) }

    DisposableEffect(Unit) {
        val listener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return

                val newOrientation = when {
                    orientation in 60..120 || orientation in 240..300 ->
                        Configuration.ORIENTATION_LANDSCAPE

                    orientation in 300..360 || orientation in 0..30 || orientation in 150..210 ->
                        Configuration.ORIENTATION_PORTRAIT

                    else ->
                        Configuration.ORIENTATION_UNDEFINED
                }

                if (orientationFlow.value != newOrientation) {
                    orientationFlow.value = newOrientation
                }
            }
        }

        listener.enable()

        onDispose {
            listener.disable()
        }
    }

    return orientationFlow
}