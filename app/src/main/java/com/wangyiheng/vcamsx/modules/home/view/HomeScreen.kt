import android.Manifest
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wangyiheng.vcamsx.components.LivePlayerDialog
import com.wangyiheng.vcamsx.components.SettingRow
import com.wangyiheng.vcamsx.components.VideoPlayerDialog
import com.wangyiheng.vcamsx.modules.home.controllers.HomeController
import java.io.File


@OptIn(ExperimentalMaterial3Api::class)
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
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    // 在 Android 9 (Pie) 及以下版本，请求 READ_EXTERNAL_STORAGE 权限
                    Toast.makeText(context, "请打开设置允许读取文件夹权限", Toast.LENGTH_SHORT).show()
                } else {
                    // 在 Android 10 及以上版本，直接访问视频文件，无需请求权限
                    selectVideoLauncher.launch("video/*")
                }
            }
        }
    )

    Box(
        modifier = Modifier.fillMaxSize().background(Color(255,255,255,1)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
//            modifier = Modifier.width(150.dp)
        ) {
            // 使按钮宽度等于列的最大宽度
            Column {
                TextField(
                    value = homeController.liveURL.value,
                    onValueChange = { homeController.liveURL.value = it },
                    label = { Text("RTMP链接：") }
                )

                // 可选：显示输入的链接
                Text(homeController.liveURL.value)
            }

            LivePlayerDialog(homeController)
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
                    homeController.isVideoDisplay.value = true
                }
            ) {
                Text("查看视频")
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    homeController.isLiveStreamingDisplay.value = true
                }
            ) {
                Text("查看直播推流")
            }

            SettingRow(
                label = "视频开关",
                checkedState = homeController.isVideoEnabled,
                onCheckedChange = { homeController.saveState() },
                context = context
            )

            SettingRow(
                label = "直播推流开关",
                checkedState = homeController.isLiveStreamingEnabled,
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


@Preview
@Composable
fun PreviewMessageCard() {
    HomeScreen()
}
