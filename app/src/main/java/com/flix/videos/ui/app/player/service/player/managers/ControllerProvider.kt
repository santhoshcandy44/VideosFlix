package com.flix.videos.ui.app.player.service

import android.os.Bundle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken


enum class ControllerSource {
    ACTIVITY,
    SERVICE
}


interface ControllerProvider {
    fun getController(sessionToken: SessionToken?, args: Bundle, onReady: ((MediaController, ControllerSource) -> Unit)?)
}
