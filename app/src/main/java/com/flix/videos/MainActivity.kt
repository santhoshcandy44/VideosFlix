package com.flix.videos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.flix.videos.ui.theme.SuperBosTheme
import com.flix.videos.ui.app.MainVideosScreen
import com.flix.videos.ui.utils.SafeDrawing

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()
        setContent {
            SuperBosTheme {
                SafeDrawing{
                    MainVideosScreen()
                }
            }
        }
    }
}
