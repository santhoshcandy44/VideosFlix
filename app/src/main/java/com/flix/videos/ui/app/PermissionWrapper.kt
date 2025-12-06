package com.flix.videos.ui.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.flix.videos.ui.utils.findActivity
import com.flix.videos.ui.utils.shortToast

@Composable
fun PermissionWrapper(
    onPermissionGranted:()->Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    var showPermissionRationaleDialog by remember { mutableStateOf(false) }
    var showPermissionAllowDialog by remember { mutableStateOf(false) }

    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO       // Android 13+
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE  // Android 12 and below
    }

    val shouldShowRequestPermissionRationale: () -> Boolean = {
        ActivityCompat.shouldShowRequestPermissionRationale(
            context.findActivity(),
            permission
        )
    }

    val readMediaPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            shortToast(context, "Permission Granted")
        } else {
            if (shouldShowRequestPermissionRationale()) {
                showPermissionRationaleDialog = true
            } else {
                showPermissionAllowDialog = true
            }
        }
    }

    val hasReadMediaPermissionIsGranted: () -> Boolean = {
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    val permissionRequest: () -> Unit = {
        if (shouldShowRequestPermissionRationale())
            showPermissionRationaleDialog = true
        else
            readMediaPermissionLauncher.launch(permission)
    }

    LaunchedEffect(Unit) {
        if (!hasReadMediaPermissionIsGranted()) {
            permissionRequest()
        }
    }

    content()

    if (showPermissionAllowDialog) {
        AllowPermissionDialog(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+
                "To continue, please allow “Photos and Videos” permission. " +
                        "This allows the app to access your media files securely."
            } else {
                // Android 12 and below
                "To continue, please allow “Storage” permission. " +
                        "This allows the app to read videos on your device."
            },
            onAllowPermissionClick = {
                if (hasReadMediaPermissionIsGranted())
                    onPermissionGranted()
                else
                    permissionRequest()
            },
            onClose = {
                context.findActivity().finish()
            }
        )
    }

    if (showPermissionRationaleDialog) {
        PermissionRationaleDialog(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+
                "To continue, please enable “Photos and Videos” permission in Settings. " +
                        "This allows the app to access your media files securely."
            } else {
                // Android 12 and below
                "To continue, please enable “Storage” permission in Settings. " +
                        "This allows the app to read videos on your device."
            },
            onPermissionResultCheck = {
                if (hasReadMediaPermissionIsGranted()) {
                    showPermissionRationaleDialog = false
                    onPermissionGranted()
                }
            },
            onClose = {
                context.findActivity().finish()
            }
        )
    }
}

@Composable
private fun AllowPermissionDialog(
    message: String,
    onAllowPermissionClick: () -> Unit,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        BackHandler(onBack = onClose)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(enabled = false) {},
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Permission Required",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = onAllowPermissionClick
                        ) {
                            Text("Allow Permission")
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun PermissionRationaleDialog(
    message: String,
    onPermissionResultCheck: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    val settingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            onPermissionResultCheck()
        }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        BackHandler(onBack = onClose)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(enabled = false) {},
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Permission Required",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                settingsLauncher.launch(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null)
                                    )
                                )
                            }
                        ) {
                            Text("Open Settings")
                        }
                    }
                }
            }
        }
    }
}