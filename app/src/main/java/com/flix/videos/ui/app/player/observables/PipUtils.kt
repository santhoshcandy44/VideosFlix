package com.flix.videos.ui.app.player.observables

import android.app.PendingIntent
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import androidx.activity.ComponentActivity
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.flix.videos.ui.app.player.ACTION_BROADCAST_CONTROL
import com.flix.videos.ui.app.player.EXTRA_CONTROL_TYPE
import com.flix.videos.ui.utils.findActivity


fun createPipAction(
    context:Context,
    @DrawableRes iconRes: Int,
    title: String,
    requestCode: Int,
    controlType: Int
): RemoteAction {
    val intent = Intent(ACTION_BROADCAST_CONTROL).apply {
        setPackage(context.packageName)
        putExtra(EXTRA_CONTROL_TYPE, controlType)
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    return RemoteAction(
        Icon.createWithResource(context, iconRes),
        title,
        title,
        pendingIntent
    )
}

@Composable
fun observeUserLeaveHint(onUserLeave: () -> Unit) {
    val context = LocalContext.current
    DisposableEffect(context) {
        val activity = context.findActivity() as ComponentActivity

        val onUserLeaveBehavior = Runnable {
            onUserLeave()
        }

        activity.addOnUserLeaveHintListener(
            onUserLeaveBehavior
        )
        onDispose {
            activity.removeOnUserLeaveHintListener(
                onUserLeaveBehavior
            )
        }
    }
}

@Composable
fun observePipRemoteActions(onReceive : (Intent?) -> Unit) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                onReceive(intent)
            }
        }
        ContextCompat.registerReceiver(
            context,
            broadcastReceiver,
            IntentFilter(ACTION_BROADCAST_CONTROL),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose {
            context.unregisterReceiver(broadcastReceiver)
        }
    }
}