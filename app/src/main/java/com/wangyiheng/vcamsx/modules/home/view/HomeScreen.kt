import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.FileChannel


@Composable
fun HomeScreen() {
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    val selectVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        videoUri = uri
        uri?.let { copyVideoToAppDir(context, it) }
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

    Column {
        Button(onClick = {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }) {
            Text("Select Video")
        }
    }
}

private fun copyVideoToAppDir(context: Context, videoUri: Uri) {
    Log.d("copyVideoToAppDir", "copyVideoToAppDir: ${videoUri}")
    val inputStream = context.contentResolver.openInputStream(videoUri)
    val outputDir = context.getExternalFilesDir(null)!!.absolutePath
    val outputFile = File(outputDir, "copied_video.mp4")

    inputStream?.use { input ->
        outputFile.outputStream().use { fileOut ->
            input.copyTo(fileOut)
        }
    }
}
