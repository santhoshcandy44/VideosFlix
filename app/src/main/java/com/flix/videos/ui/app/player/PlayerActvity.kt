package com.flix.videos.ui.app.player

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.flix.videos.ui.app.player.viewmodel.VideoParams
import com.flix.videos.ui.theme.AppTheme
import com.flix.videos.ui.utils.SafeDrawing
import kotlinx.coroutines.channels.Channel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf


class PlayerActivity : ComponentActivity() {
    val volumeKeyChannel = Channel<Int>(
        capacity = Channel.BUFFERED
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()
        val uri = intent.data
        if (uri == null) {
            finish()
            return
        }
        val group = intent.getStringExtra("group")
        val videoId = intent.getLongExtra("video_id", -1)
        val title = intent.getStringExtra("title")
        val videoWidth = intent.getIntExtra("video_width", 0)
        val videoHeight = intent.getIntExtra("video_height", 0)
        val totalDurationMillis = intent.getLongExtra("total_duration", 0L)
        setContent {
            AppTheme {
                SafeDrawing(isFullScreenMode = true) {
                    VideoPlayerScreen(
                        volumeKeyChannel = volumeKeyChannel,
                        viewModel = koinViewModel(parameters = {
                            parametersOf(
                                VideoParams(
                                    group = group,
                                    id = videoId
                                )
                            )
                        }),
                        onPopUp = {
                            this@PlayerActivity.finish()
                        })
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                volumeKeyChannel.trySend(1)
            }

            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                volumeKeyChannel.trySend(-1)
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}