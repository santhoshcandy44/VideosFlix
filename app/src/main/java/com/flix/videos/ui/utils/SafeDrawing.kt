package com.flix.videos.ui.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsEndWidth
import androidx.compose.foundation.layout.windowInsetsStartWidth
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun SafeDrawing(
    statusBarColor: Color = MaterialTheme.colorScheme.background,
    navigationBarColor: Color = MaterialTheme.colorScheme.background,
    isFullScreenMode: Boolean = false,
    isNavigationBarContrastEnforced: Boolean=false,
    content: @Composable () -> Unit,
) {
    val window = LocalContext.current.findActivity().window
    LaunchedEffect(window) {
        window.setNavigationBarContrastEnforced(isNavigationBarContrastEnforced)
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .then(
                    if (!isFullScreenMode) Modifier.safeDrawingPadding() else Modifier
                ).imePadding()
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }

        if(!isFullScreenMode){
            Spacer(
                modifier = Modifier
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(statusBarColor)
            )

            Spacer(
                modifier = Modifier
                    .windowInsetsBottomHeight(WindowInsets.navigationBars)
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(navigationBarColor)
            )

            Spacer(
                modifier = Modifier
                    .windowInsetsStartWidth(WindowInsets.navigationBars)
                    .fillMaxHeight()
                    .align(Alignment.TopStart)
                    .background(navigationBarColor)
            )

            Spacer(
                modifier = Modifier
                    .windowInsetsEndWidth(WindowInsets.navigationBars)
                    .fillMaxHeight()
                    .align(Alignment.TopEnd)
                    .background(navigationBarColor)
            )
        }
    }
}