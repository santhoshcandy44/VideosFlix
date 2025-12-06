package com.flix.videos.ui.app

import android.Manifest
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ModeEditOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.flix.videos.R
import com.flix.videos.models.VideoInfo
import com.flix.videos.ui.app.player.ACTION_BROADCAST_CONTROL
import com.flix.videos.ui.app.player.EXTRA_CONTROL_CLOSE
import com.flix.videos.ui.app.player.EXTRA_CONTROL_TYPE
import com.flix.videos.ui.app.player.PlayerActivity
import com.flix.videos.ui.utils.FormatterUtils.formatHumanReadableBytesSize
import com.flix.videos.ui.utils.FormatterUtils.formatTimeSeconds
import com.flix.videos.ui.utils.crop
import com.flix.videos.ui.utils.shortToast
import com.flix.videos.viewmodel.ReadMediaVideosViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosScreen(viewModel: ReadMediaVideosViewModel) {
    val videInfos by viewModel.videoInfos.collectAsState()
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painterResource(R.drawable.ic_nav), contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Text("Videos", style = MaterialTheme.typography.titleMedium)
            }
        })
        VideosListScreen(
            videInfos,
            viewModel,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosListScreen(
    videInfos: List<VideoInfo>,
    viewModel: ReadMediaVideosViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var selectedDetailItem by remember { mutableStateOf<VideoInfo?>(null) }
    var showMoreOptions by remember { mutableStateOf(false) }
    var showDetailsSheet by remember { mutableStateOf(false) }

    var showRenameVideo by remember { mutableStateOf(false) }

    val readMediaPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            shortToast(context, "Permission Granted")
        } else {
            shortToast(context, "Permission Denied")
        }
    }

    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO       // Android 13+
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE  // Android 12 and below
    }

    val hasReadMediaPermissionIsGranted: () -> Boolean = {
        ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(Unit) {
        if (hasReadMediaPermissionIsGranted()) {
            readMediaPermissionLauncher.launch(permission)
        }
    }

    val deleteMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedDetailItem?.let { item ->
                context.contentResolver.delete(item.uri, null, null)
            }
        }
    }

    var renameText by rememberSaveable { mutableStateOf("") }

    val renameMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedDetailItem?.let { item ->
                viewModel.renameVideo(item.uri, renameText)
            }
        }
    }

    LazyColumn(
        modifier = modifier
    ) {
        items(videInfos, key = { it.uri }) { videoInfo ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }) {
                            val intent = Intent(ACTION_BROADCAST_CONTROL).apply {
                                `package` = context.packageName
                                putExtra(EXTRA_CONTROL_TYPE, EXTRA_CONTROL_CLOSE)
                            }
                            context.sendBroadcast(intent)
                            context.startActivity(
                                Intent(context, PlayerActivity::class.java)
                                    .apply {
                                        data = videoInfo.uri
                                        putExtra("title", videoInfo.title)
                                        putExtra("video_width", videoInfo.width)
                                        putExtra("video_height", videoInfo.height)
                                        putExtra("total_duration", videoInfo.duration)
                                    })
                        }) {


                    videoInfo.thumbnail?.let {
                        Image(
                            it.asImageBitmap(),
                            modifier = Modifier
                                .width(160.dp)
                                .height(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    1.dp,
                                    Color.DarkGray,
                                    RoundedCornerShape(8.dp)
                                ),
                            contentDescription = null
                        )
                    } ?: run {
                        Spacer(
                            modifier = Modifier
                                .width(160.dp)
                                .height(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    1.dp,
                                    Color.DarkGray,
                                    RoundedCornerShape(8.dp)
                                )
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = videoInfo.title,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = SimpleDateFormat(
                                "dd MMM yyyy",
                                Locale.getDefault()
                            ).format(Date(videoInfo.dateAdded * 1000L)),
                            maxLines = 1,
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Box(modifier = Modifier.wrapContentSize()) {
                    IconButton(onClick = {
                        selectedDetailItem = videoInfo
                        showMoreOptions = true
                    }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }

                    selectedDetailItem?.let { item ->
                        DropdownMenu(
                            expanded = item == videoInfo && showMoreOptions,
                            onDismissRequest = {
                                showMoreOptions = false
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .crop(vertical = 8.dp),
                            offset = DpOffset((-8).dp, 0.dp),
                            containerColor = Color.DarkGray
                        ) {
                            MenuItem(
                                icon = Icons.Outlined.ModeEditOutline,
                                label = "Rename"
                            ) {
                                showMoreOptions = false
                                renameText = item.title
                                showRenameVideo = true
                            }

                            MenuItem(
                                icon = Icons.Outlined.Delete,
                                label = "Delete"
                            ) {
                                try {
                                    context.contentResolver.delete(item.uri, null, null)
                                } catch (e: RecoverableSecurityException) {
                                    val intentSender = e.userAction.actionIntent.intentSender
                                    deleteMediaLauncher.launch(
                                        IntentSenderRequest.Builder(intentSender).build()
                                    )
                                } finally {
                                    showMoreOptions = false
                                }
                            }

                            MenuItem(
                                icon = Icons.Outlined.ContentCut,
                                label = "Edit"
                            ) {
                                showMoreOptions = false
                                val editIntent = Intent(Intent.ACTION_EDIT).apply {
                                    setDataAndType(item.uri, "video/*")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    Intent.createChooser(
                                        editIntent,
                                        "Edit Video"
                                    )
                                )
                            }

                            MenuItem(
                                icon = Icons.Outlined.Share,
                                label = "Share"
                            ) {
                                showMoreOptions = false
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = item.mimeType
                                    putExtra(Intent.EXTRA_STREAM, item.uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    Intent.createChooser(
                                        shareIntent,
                                        "Share Video"
                                    )
                                )
                            }

                            MenuItem(
                                icon = Icons.Outlined.Info,
                                label = "Details"
                            ) {
                                showMoreOptions = false
                                showDetailsSheet = true
                            }
                        }
                    }
                }
            }
        }
    }

    selectedDetailItem?.takeIf { showRenameVideo }
        ?.let { item ->
            RenameVideoDialog(renameText, {
                renameText = it
            }, onDismiss = {
                showRenameVideo = false
            }) {
                try {
                    viewModel.renameVideo(item.uri, it)
                } catch (e: RecoverableSecurityException) {
                    renameMediaLauncher.launch(
                        IntentSenderRequest.Builder(
                            e.userAction.actionIntent.intentSender
                        ).build()
                    )
                } finally {
                    showRenameVideo = false
                }
            }
        }

    selectedDetailItem.takeIf { showDetailsSheet }?.let { detailItem ->
        ModalBottomSheet(
            onDismissRequest = {
                selectedDetailItem = null
                showDetailsSheet = false
            },
            dragHandle = null,
            modifier = Modifier
                .safeDrawingPadding()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Details",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.DarkGray)
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    InfoRow("Name", detailItem.title)
                    InfoRow("Duration", formatTimeSeconds(detailItem.duration / 1000f))
                    InfoRow("Resolution", "${detailItem.width} × ${detailItem.height} pixels")
                    InfoRow("Size", formatHumanReadableBytesSize(detailItem.size))
                    InfoRow("MimeType", detailItem.mimeType)
                    InfoRow("Path", detailItem.path)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            color = Color.LightGray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(2f)
        )
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        leadingIcon = {
            Icon(icon, contentDescription = label)
        },
        text = { Text(label) },
        onClick = onClick,
        colors = MenuDefaults.itemColors(
            textColor = Color.White,
            leadingIconColor = Color.White
        )
    )
}

@Composable
private fun RenameVideoDialog(
    renameText: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Rename Video",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column {
                TextField(
                    value = renameText,
                    onValueChange = onValueChange,
                    singleLine = true,
                    colors = TextFieldDefaults.colors(unfocusedIndicatorColor = Color.Magenta)
                )
            }
        },

        confirmButton = {
            TextButton(
                onClick = {
                    if (renameText.isNotBlank()) onRename(renameText.trim())
                }
            ) {
                Text("Rename", color = Color.Magenta)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Magenta)
            }
        }
    )
}
