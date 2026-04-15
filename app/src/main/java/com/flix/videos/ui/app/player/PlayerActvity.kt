package com.flix.videos.ui.app.player

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.media3.common.util.UnstableApi
import com.flix.videos.ui.app.player.service.player.managers.MediaControllerManager
import com.flix.videos.ui.app.player.viewmodel.VideoParams
import com.flix.videos.ui.theme.AppTheme
import com.flix.videos.ui.utils.SafeDrawing
import kotlinx.coroutines.channels.Channel
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@UnstableApi
class PlayerActivity : ComponentActivity() {
    private val volumeKeyChannel = Channel<Int>(
        capacity = Channel.BUFFERED
    )

    private val mediaControllerManager: MediaControllerManager by inject {
        parametersOf(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()

        val uri = intent.data
        if (uri == null) {
            finish()
            return
        }

        val configuration = resources.configuration
        when (configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                exitFullScreenMode(this)
            }
            Configuration.ORIENTATION_LANDSCAPE -> {
                enterFullScreenMode(this)
            }
        }

        val group = intent.getStringExtra("group")
        val videoId = intent.getLongExtra("video_id", -1)

        setContent {
            AppTheme {
                SafeDrawing(isFullScreenMode = true) {
                    Surface(color = Color.Black) {
                        VideoPlayerScreen(
                            volumeKeyChannel = volumeKeyChannel,
                            viewModel = koinViewModel(parameters = {
                                parametersOf(
                                    false,
                                    VideoParams(
                                        group = group,
                                        id = videoId
                                    ),
                                    mediaControllerManager,
                                    null
                                )
                            }),
                            onPopUp = {
                                this@PlayerActivity.finish()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (isInPictureInPictureMode) {
            moveTaskToBack(false)
            mediaControllerManager.releaseMediaController()
            finish()
            startActivity(
                Intent(this, PlayerActivity::class.java).apply {
                    data = intent.data
                    replaceExtras(intent)
                }
            )
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        when(newConfig.orientation){
            Configuration.ORIENTATION_PORTRAIT ->{
                exitFullScreenMode(this)
            }

            Configuration.ORIENTATION_LANDSCAPE ->{
                enterFullScreenMode(this)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        exitFullScreenMode(this)
    }
}