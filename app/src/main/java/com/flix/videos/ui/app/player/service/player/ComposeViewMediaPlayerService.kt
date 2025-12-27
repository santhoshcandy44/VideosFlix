package com.flix.videos.ui.app.player.service.player

import android.content.Intent
import android.os.IBinder
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ServiceLifecycleDispatcher
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.flix.videos.ui.app.player.viewmodel.VideoParams

abstract class ComposeViewMediaPlayerService : MediaPlayerService(), LifecycleOwner, ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val dispatcher = ServiceLifecycleDispatcher(this)

    private val vm = ViewModelStore()
    private var savedStateRegistryController: SavedStateRegistryController = SavedStateRegistryController.create(this)


    override fun onCreate() {
        super.onCreate()
        dispatcher.onServicePreSuperOnCreate()
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onStart(intent: Intent?, startId: Int) {
        dispatcher.onServicePreSuperOnStart()
        super.onStart(intent, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        dispatcher.onServicePreSuperOnBind()
        return super.onBind(intent)
    }

    override val viewModelStore: ViewModelStore = vm

    override val savedStateRegistry: SavedStateRegistry =
        savedStateRegistryController.savedStateRegistry

    override val lifecycle: Lifecycle
        get() = dispatcher.lifecycle
            @Composable
    abstract fun Content(videoParams: VideoParams)

    override fun onDestroy() {
        dispatcher.onServicePreSuperOnDestroy()
        viewModelStore.clear()
        super.onDestroy()
    }
}

