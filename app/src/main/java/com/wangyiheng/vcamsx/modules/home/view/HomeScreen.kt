import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.wangyiheng.vcamsx.modules.home.controllers.HomeController
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.FileChannel


var mediaPlayer: IjkMediaPlayer? = null

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val homeController = remember { HomeController() }
    val path = context.getExternalFilesDir(null)!!.absolutePath
    val file = File(path, "copied_video.mp4")
    val detailAlterShow = remember { mutableStateOf(false) }
    val ijkPlayer = remember {
        IjkMediaPlayer().apply {
            setDataSource(context, Uri.parse("$path/copied_video.mp4"))
            setOnPreparedListener { start() } // 准备完成后开始播放
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 0)
        }
    }
    LaunchedEffect(Unit){
        homeController.init()
    }
    val showDialog = remember { mutableStateOf(true) }

    val videoPath = context.getExternalFilesDir(null)!!.absolutePath + "/copied_video.mp4"


    val selectVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            homeController.copyVideoToAppDir(context,it)
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            if (isGranted) {
                selectVideoLauncher.launch("video/*")
            } else {
                // Handle permission denial
                Toast.makeText(context, "请打开设置允许读取文件夹权限", Toast.LENGTH_SHORT).show()
            }
        }
    )

    Box(
        modifier = Modifier.fillMaxSize().background(Color(255,255,255,1)),
        contentAlignment = Alignment.Center,

    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(150.dp)
        ) {
            // 使按钮宽度等于列的最大宽度

            VideoPlayerDialog(detailAlterShow, context, videoPath)


            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            ) {
                Text("选择视频")
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    detailAlterShow.value = true
                }
            ) {
                Text("查看视频")
            }
            Row(
                verticalAlignment = Alignment.CenterVertically, // 对齐文本和开关
                modifier = Modifier.fillMaxWidth() // 拉伸以匹配按钮宽度
            ) {
                Text("视频开关：", modifier = Modifier.weight(1f)) // 权重使文本占据大部分空间
                Switch(
                    checked = homeController.isVideoEnabled.value,
                    onCheckedChange = {
                        homeController.isVideoEnabled.value = it
                        homeController.saveState()
                        Toast.makeText(context, if (it) "视频打开" else "视频关闭", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically, // 对齐文本和开关
                modifier = Modifier.fillMaxWidth() // 拉伸以匹配按钮宽度
            ) {
                Text("音量开关：", modifier = Modifier.weight(1f)) // 权重使文本占据大部分空间
                Switch(
                    checked = homeController.isVolumeEnabled.value,
                    onCheckedChange = {
                        homeController.isVolumeEnabled.value = it
                        homeController.saveState()
                        Toast.makeText(context, if (it) "声音打开" else "声音关闭", Toast.LENGTH_SHORT).show()
                    }
                )
            }
//            Row(
//                verticalAlignment = Alignment.CenterVertically, // 对齐文本和开关
//                modifier = Modifier.fillMaxWidth() // 拉伸以匹配按钮宽度
//            ) {
//                Text("备选播放器:", modifier = Modifier.weight(1f)) // 权重使文本占据大部分空间
//                Switch(
//                    checked = (homeController.videoPlayer.value == 2),
//                    onCheckedChange = {
//                        homeController.videoPlayer.value = if(it) 2 else 1
//                        homeController.saveState()
//                        Toast.makeText(context, if (it) "备选播放器打开" else "备选播放器关闭", Toast.LENGTH_SHORT).show()
//                    }
//                )
//            }
            Row(
                verticalAlignment = Alignment.CenterVertically, // 对齐文本和开关
                modifier = Modifier.fillMaxWidth() // 拉伸以匹配按钮宽度
            ) {
                Text((if (homeController.codecType.value) "硬解码" else "软解码")+":", modifier = Modifier.weight(1f)) // 权重使文本占据大部分空间
                Switch(
                    checked = (homeController.codecType.value),
                    onCheckedChange = {
                        homeController.codecType.value = it
                        homeController.saveState()
                        Toast.makeText(context, if (it) "硬解码" else "软解码", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}
@Composable
fun VideoPlayerDialog(showDialog: MutableState<Boolean>, context: Context, videoPath: String) {
    if (showDialog.value) {
        Dialog(onDismissRequest = {
            showDialog.value = false
            mediaPlayer?.release()
        }) {
            Column(
                modifier = Modifier.size(width = 300.dp, height = 400.dp), // 设置Dialog的大小
                verticalArrangement = Arrangement.Center
            ) {
                AndroidView(
                    modifier = Modifier.weight(1f), // 让视频播放器填充除按钮以外的空间
                    factory = { ctx ->
                        SurfaceView(ctx).apply {
                            holder.addCallback(object : SurfaceHolder.Callback {
                                override fun surfaceCreated(holder: SurfaceHolder) {
                                    playVideo(context, holder, videoPath)
                                }

                                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

                                override fun surfaceDestroyed(holder: SurfaceHolder) {
                                    // 这里释放播放器资源
                                }
                            })
                        }
                    }
                )
                Button(
                    onClick = { showDialog.value = false }, // 点击按钮关闭Dialog
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("关闭")
                }
            }
        }
    }
}

// 其他代码保持不变


private fun playVideo(context: Context, holder: SurfaceHolder, videoPath: String) {
    mediaPlayer = IjkMediaPlayer().apply {
        setDataSource(videoPath)
        setDisplay(holder)
        prepareAsync()
        setOnPreparedListener { start() }
    }
}
@Preview
@Composable
fun PreviewMessageCard() {
    HomeScreen()
}
