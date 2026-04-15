package com.flix.videos.ui.app.player.service.player.managers

import android.content.Context
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import org.koin.core.annotation.Factory

@Factory
class MediaControllerManager(
   private val context: Context
) : ControllerProvider {
    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    override fun getController(
        sessionToken: SessionToken?,
        connectionHints: Bundle,
        onReady: ((MediaController, ControllerSource) -> Unit)?
    ) {
        if (sessionToken == null) throw IllegalArgumentException("session token can not be empty")

        controller?.let {
            onReady?.invoke(it, ControllerSource.ACTIVITY)
            return
        }

        if (controllerFuture == null) {
            controllerFuture =
                MediaController.Builder(
                    context,
                    sessionToken
                ).buildAsync()
        }

        controllerFuture!!.addListener(
            {
                controller = controllerFuture!!.get()
                onReady?.invoke(controller!!, ControllerSource.ACTIVITY)
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    fun releaseMediaController() {
        controller?.stop()
        controller?.clearMediaItems()
        controller?.release()
        controller = null
        controllerFuture = null
    }
}