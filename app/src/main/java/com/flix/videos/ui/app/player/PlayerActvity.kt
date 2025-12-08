package com.flix.videos.ui.app.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.OnPictureInPictureModeChangedProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.flix.videos.ui.theme.SuperBosTheme
import com.flix.videos.ui.utils.SafeDrawing
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

class PlayerActivity : ComponentActivity(), OnPictureInPictureModeChangedProvider {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()
        enterFullScreenMode(this)
        val uri = intent.data
        if (uri == null) {
            finish()
            return
        }
        val title = intent.getStringExtra("title")
        val videoWidth = intent.getIntExtra("video_width", 0)
        val videoHeight = intent.getIntExtra("video_height", 0)
        val totalDurationMillis = intent.getLongExtra("total_duration", 0L)
        setContent {
            SuperBosTheme {
                SafeDrawing(isFullScreenMode = true) {
                    VideoPlayerScreen(
                        viewModel = koinViewModel(parameters = {
                            parametersOf(
                                uri.toString(),
                                title ?: "",
                                videoWidth,
                                videoHeight,
                                totalDurationMillis
                            )
                        }),
                        onPopUp = {
                            this@PlayerActivity.finish()
                        })
                }
            }
        }
    }
}