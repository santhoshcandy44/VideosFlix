package com.flix.videos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.flix.videos.ui.app.MainVideosScreen
import com.flix.videos.ui.app.PermissionWrapper
import com.flix.videos.ui.app.viewmodel.ReadMediaVideosViewModel
import com.flix.videos.ui.theme.AppTheme
import com.flix.videos.ui.utils.SafeDrawing
import org.koin.compose.viewmodel.koinViewModel

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()

        setContent {
            val safeDrawing = WindowInsets.safeDrawing
            val configuration = LocalConfiguration.current
            var initialSafeInsets by retain {
                mutableStateOf(safeDrawing)
            }

            LaunchedEffect(configuration) {
                initialSafeInsets = safeDrawing
            }

            AppTheme {
                SafeDrawing(windowInsets = initialSafeInsets) {
                    val viewModel: ReadMediaVideosViewModel = koinViewModel()
                    PermissionWrapper(onPermissionGranted = {
                        viewModel.fetchVideoInfos()
                    }) {
                        MainVideosScreen(viewModel)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun preView() {
    AppTheme {

    }
}