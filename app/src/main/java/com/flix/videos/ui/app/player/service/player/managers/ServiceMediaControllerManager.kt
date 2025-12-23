package com.flix.videos.ui.app.player.service.player.managers

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.flix.videos.ui.app.player.service.player.PipPlayerService
import com.google.common.util.concurrent.ListenableFuture
import org.koin.core.annotation.Single

@Single
class ServiceMediaControllerManager(
    private val applicationContext: Context
) : ControllerProvider {
    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null


    @Synchronized
    override fun getController(
        sessionToken: SessionToken?,
        args: Bundle,
        onReady: ((MediaController, ControllerSource) -> Unit)?
    ) {
        controller?.let {
            onReady?.invoke(it, ControllerSource.SERVICE)
            return
        }

        controllerFuture = controllerFuture ?: MediaController.Builder(
            applicationContext,
            SessionToken(
                applicationContext,
                ComponentName(
                    applicationContext,
                    PipPlayerService::class.java
                )
            )
        ) .setConnectionHints(args)
            .buildAsync()

        controllerFuture?.let { future ->
            future.addListener(
                {
                    controller = future.get()
                    onReady?.invoke(controller!!, ControllerSource.SERVICE)
                },
                ContextCompat.getMainExecutor(applicationContext)
            )
        }
    }

    fun releaseMediaController() {
        controller?.stop()
        controller?.clearMediaItems()
        controller?.release()
        controller = null
        controllerFuture = null
    }
}