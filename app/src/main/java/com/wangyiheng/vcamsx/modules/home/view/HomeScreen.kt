import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wangyiheng.vcamsx.components.LivePlayerDialog
import com.wangyiheng.vcamsx.components.SettingRow
import com.wangyiheng.vcamsx.components.VideoPlayerDialog
import com.wangyiheng.vcamsx.modules.home.controllers.HomeController


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val homeController =  viewModel<HomeController>()
    LaunchedEffect(Unit){
        homeController.init()
    }

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

    Card(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
        val buttonModifier = Modifier
            .fillMaxWidth()

        Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextField(
                value = homeController.liveURL.value,
                onValueChange = { homeController.liveURL.value = it },
                label = { Text("RTMP链接：") }
            )

            Button(
                modifier = buttonModifier,
                onClick = {
                    homeController.saveState()
                }
            ) {
                Text("保存RTMP链接")
            }
            Button(
                modifier = buttonModifier,
                onClick = {
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)

                }
            ) {
                Text("选择视频")
            }

            Button(
                modifier = buttonModifier,
                onClick = {
                    homeController.isVideoDisplay.value = true
                }
            ) {
                Text("查看视频")
            }

            Button(
                modifier = buttonModifier,
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
                onCheckedChange = {
                    if(homeController.isH264HardwareDecoderSupport()){
                        homeController.saveState()
                    }else{
                        homeController.codecType.value = false
                        Toast.makeText(context, "不支持硬解码", Toast.LENGTH_SHORT).show()
                    }},
                context = context
            )
        }
        val annotatedString = AnnotatedString.Builder("本软件免费，点击前往软件下载页").apply {
            // 添加点击事件的范围
            addStringAnnotation(
                tag = "URL",
                annotation = "https://github.com/iiheng/VCAMSX/releases",
                start = 0,
                end = 12
            )
        }.toAnnotatedString()

        ClickableText(
            text = annotatedString,
            style = TextStyle(fontSize = 12.sp, textDecoration = TextDecoration.Underline)
        ) { offset ->
            annotatedString.getStringAnnotations("URL", offset, offset)
                .firstOrNull()?.let { annotation ->
                    // 在这里处理点击事件，比如打开一个浏览器
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                    context.startActivity(intent)
                }
        }

        LivePlayerDialog(homeController)
        VideoPlayerDialog(homeController)
    }
}


@Preview
@Composable
fun PreviewMessageCard() {
    HomeScreen()
}
