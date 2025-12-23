package com.flix.videos.ui.app.player.service.overlay


import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.flix.videos.WindowLayoutState
import com.flix.videos.ui.app.player.VideoPlayerScreen
import com.flix.videos.ui.app.player.common.dpToPx
import com.flix.videos.ui.app.player.viewmodel.VideoParams
import com.flix.videos.ui.app.player.viewmodel.VideoPlayerViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

class PipPlayerService : ComposeViewMediaPlayerService() {

    private val windowLayoutState = MutableStateFlow(WindowLayoutState.NONE)
    private var windowManager: WindowManager? = null
    private lateinit var layoutParams: WindowManager.LayoutParams
    private lateinit var overlayView: ComposeView

    companion object X {
        @Volatile
        var isRunning = false
    }

    override fun onCreate() {
        isRunning = true
        super.onCreate()
        lifecycleScope.launch {
            videoId.collectLatest { videoId ->
                if (videoId != null) {
                    layoutParams = WindowManager.LayoutParams().apply {
                        width = WindowManager.LayoutParams.WRAP_CONTENT
                        height = WindowManager.LayoutParams.WRAP_CONTENT
                        type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

                        format = PixelFormat.TRANSLUCENT
                        gravity = Gravity.TOP or Gravity.START
                        x = 0
                        y = 0
                    }

                    windowManager = applicationContext.getSystemService(WINDOW_SERVICE)
                            as WindowManager

                    overlayView = ComposeView(this@PipPlayerService).apply {
                        setViewTreeLifecycleOwner(this@PipPlayerService)
                        setViewTreeViewModelStoreOwner(this@PipPlayerService)
                        setViewTreeSavedStateRegistryOwner(this@PipPlayerService)
                        setContent {
                            Content(videoId)
                        }
                    }

                    windowManager!!.addView(overlayView, layoutParams)

                    windowLayoutState.value =
                        getOverlayInsetsAndBounds(this@PipPlayerService)

                    val windowLayoutState = windowLayoutState.value
                    val fullWidthPx = windowLayoutState.widthPx
                    val fullHeightPx = windowLayoutState.heightPx

                    val maxX = fullWidthPx - windowLayoutState.insets.right
                    val maxY = fullHeightPx - windowLayoutState.insets.bottom

                    layoutParams.x = maxX
                    layoutParams.y = maxY
                    disableLayoutMovements(layoutParams)

                    overlayView.post {
                        windowManager!!.updateViewLayout(overlayView, layoutParams)
                    }
                }
            }
        }
    }

    @Composable
    override fun Content(videoId: Long) {
        val windowLayoutState by windowLayoutState.collectAsState()
        val viewModel = koinViewModel<VideoPlayerViewModel>(
            parameters = {
                parametersOf(
                    true,
                    VideoParams(
                        group = null,
                        id = videoId
                    ),
                    null,
                    serviceMediaControllerManager
                )
            }
        )

        VideoPlayerScreen(
            viewModel,
            isOverlayWindow = true,
            windowLayoutState = windowLayoutState,
            attachDragBehavior = { dragView, windowLayoutStateProvider, onClick ->
                attachDragBehavior(dragView, windowLayoutStateProvider, onClick)
            },
            updateWindowSize = { videoWidth, videoHeight, isRestPosition ->
                updateWindowSize(videoWidth, videoHeight, isRestPosition)
            }
        )
    }

    fun disableLayoutMovements(layoutParams: WindowManager.LayoutParams) {
        val className = "android.view.WindowManager\$LayoutParams"

        try {
            val layoutParamsClass = Class.forName(className)

            val privateFlagsField = layoutParamsClass.getField("privateFlags")
            val noAnimField = layoutParamsClass.getField("PRIVATE_FLAG_NO_MOVE_ANIMATION")

            val privateFlagsValue = privateFlagsField.getInt(layoutParams)
            val noAnimFlag = noAnimField.getInt(layoutParams)

            privateFlagsField.setInt(
                layoutParams,
                privateFlagsValue or noAnimFlag
            )

        } catch (e: ClassNotFoundException) {
        } catch (e: Exception) {
        }
    }

    fun updateWindowSize(videoWidth: Int, videoHeight: Int, isRestPosition: Boolean) {
        if (videoWidth < 0 && videoHeight < 0) return

        val windowLayoutState = windowLayoutState.value
        val fullWidthPx = windowLayoutState.widthPx
        val fullHeightPx = windowLayoutState.heightPx

        val (width, height) = calculateMiniWindowSize(
            windowLayoutState,
            videoWidth,
            videoHeight
        )

        val oldX = layoutParams.x
        val oldY = layoutParams.y

        layoutParams.width = width
        layoutParams.height = height

        val margin = dpToPx(8)

        if (isRestPosition) {
            val maxX =
                fullWidthPx - width - windowLayoutState.insets.right - margin

            val maxY =
                fullHeightPx - height - windowLayoutState.insets.bottom - margin

            layoutParams.x = maxX
            layoutParams.y = maxY
        } else {
            val maxX =
                fullWidthPx - width - windowLayoutState.insets.right - margin

            val maxY = fullHeightPx - height - windowLayoutState.insets.bottom - margin

            layoutParams.x = minOf(oldX, maxX)
            layoutParams.y = minOf(oldY, maxY)
        }

        overlayView.post {
            windowManager!!.updateViewLayout(overlayView, layoutParams)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachDragBehavior(
        dragView: View,
        windowLayoutStateProvider: () -> WindowLayoutState,
        onClick: () -> Unit
    ) {
        var startX = 0
        var startY = 0
        var touchStartX = 0f
        var touchStartY = 0f
        var isDragging = false

        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop

        dragView.setOnTouchListener { _, event ->

            val windowLayoutState = windowLayoutStateProvider()
            val maxWidth = windowLayoutState.widthPx
            val maxHeight = windowLayoutState.heightPx

            val params = layoutParams

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    isDragging = false

                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchStartX).toInt()
                    val dy = (event.rawY - touchStartY).toInt()

                    if (kotlin.math.abs(dx) > touchSlop || kotlin.math.abs(dy) > touchSlop) {
                        isDragging = true

                        val dragViewWidth = dragView.width
                        val dragViewHeight = dragView.height

                        val margin = dpToPx(8)

                        val availableDx =
                            maxWidth - dragViewWidth - windowLayoutState.insets.right - margin
                        val availableDY =
                            maxHeight - dragViewHeight - windowLayoutState.insets.bottom - margin

                        params.x = (startX + dx).coerceIn(
                            windowLayoutState.insets.left + margin,
                            maxOf(availableDx, availableDx)
                        )
                        params.y = (startY + dy).coerceIn(
                            windowLayoutState.insets.top + margin,
                            maxOf(availableDY, windowLayoutState.insets.top + margin)
                        )

                        overlayView.post {
                            windowManager!!.updateViewLayout(overlayView, layoutParams)
                        }
                    }
                    true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    if (!isDragging) {
                        onClick()
                    }
                    true
                }

                else -> false
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        windowLayoutState.value = getOverlayInsetsAndBounds(this)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        isRunning = false
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        isRunning = false
        windowManager?.removeView(overlayView)
        Log.e("Player", "onDestroy: PipPlayerService")
        super.onDestroy()
    }
}
