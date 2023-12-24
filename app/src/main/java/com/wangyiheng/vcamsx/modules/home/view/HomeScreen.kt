import android.Manifest
import android.content.Context
import android.net.Uri
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wangyiheng.vcamsx.components.SettingRow
import com.wangyiheng.vcamsx.modules.home.controllers.HomeController
import java.io.File


@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val homeController =  viewModel<HomeController>()
    val path = context.getExternalFilesDir(null)!!.absolutePath
    val file = File(path, "copied_video.mp4")

    LaunchedEffect(Unit){
        homeController.init()
    }

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

            VideoPlayerDialog(homeController, context, videoPath)


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
                    homeController.detailAlterShow.value = true
                }
            ) {
                Text("查看视频")
            }
            SettingRow(
                label = "视频开关",
                checkedState = homeController.isVideoEnabled,
                onCheckedChange = { homeController.saveState() },
                context = context
            )

            SettingRow(
                label = "音量开关",
                checkedState = homeController.isVolumeEnabled,
                onCheckedChange = { homeController.saveState() },
                context = context
            )

            SettingRow(
                label = if (homeController.codecType.value) "硬解码" else "软解码",
                checkedState = homeController.codecType,
                onCheckedChange = { homeController.saveState() },
                context = context
            )
        }
    }
}
@Composable
fun VideoPlayerDialog(homeController: HomeController, context: Context, videoPath: String) {
    if (homeController.detailAlterShow.value) {
        Dialog(onDismissRequest = {
            homeController.detailAlterShow.value = false
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
//                                    playVideo(context, holder, videoPath)
                                    homeController.playRTMPStream(context, holder, "rtmp://192.168.123.129:1935/live/test110")
                                }

                                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

                                }

                                override fun surfaceDestroyed(holder: SurfaceHolder) {
                                    // 这里释放播放器资源
                                    homeController.release()
                                }
                            })
                        }
                    }
                )
            }
        }
    }
}








@Preview
@Composable
fun PreviewMessageCard() {
    HomeScreen()
}
