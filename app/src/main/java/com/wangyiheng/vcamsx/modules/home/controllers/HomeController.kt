package com.wangyiheng.vcamsx.modules.home.controllers

import android.content.Context
import android.net.Uri
import java.io.File

class HomeController {



    private fun copyVideoToAppDir(context: Context, videoUri: Uri) {
        val inputStream = context.contentResolver.openInputStream(videoUri)
        val outputDir = context.getExternalFilesDir(null)!!.absolutePath
        val outputFile = File(outputDir, "copied_video.mp4")

        inputStream?.use { input ->
            outputFile.outputStream().use { fileOut ->
                input.copyTo(fileOut)
            }
        }
    }

}