package com.flix.videos.ui.app.player.service.player.managers

import android.os.Bundle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken

enum class ControllerSource {
    ACTIVITY,
    SERVICE
}

interface ControllerProvider {
    fun getController(sessionToken: SessionToken?, connectionHints: Bundle, onReady: ((MediaController, ControllerSource) -> Unit)?)
}
