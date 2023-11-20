package com.wangyiheng.vcamsx.modules.home.controllers

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.wangyiheng.vcamsx.data.models.VideoStatues
import com.wangyiheng.vcamsx.utils.InfoManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

class HomeController: KoinComponent {

    val context by inject<Context>()
    val isVideoEnabled  = mutableStateOf(false)
    val isVolumeEnabled = mutableStateOf(false)
    val infoManager by inject<InfoManager>()

    fun init(){
        getState()
    }

    fun copyVideoToAppDir(context: Context,videoUri: Uri) {
        val inputStream = context.contentResolver.openInputStream(videoUri)
        val outputDir = context.getExternalFilesDir(null)!!.absolutePath
        val outputFile = File(outputDir, "copied_video.mp4")

        inputStream?.use { input ->
            outputFile.outputStream().use { fileOut ->
                input.copyTo(fileOut)
            }
        }
    }

    fun saveState() {
        infoManager.removeVideoStatus()
        infoManager.saveVideoStatus(VideoStatues(isVideoEnabled.value,isVolumeEnabled.value))
    }

    fun getState(){
        infoManager.getVideoStatus()?.let {
            isVideoEnabled.value = it.isVideoEnable
            isVolumeEnabled.value = it.volume
        }
    }
}