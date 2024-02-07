package com.wangyiheng.vcamsx.utils

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import java.io.FileOutputStream

class Lib {
    fun extractFramesFromVideo(videoPath: String): List<Bitmap> {
        val retriever = MediaMetadataRetriever()
        val frameList = mutableListOf<Bitmap>()
        try {
            retriever.setDataSource(videoPath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLong()
            val frameRate = 10000000

            for (time in 0..duration step frameRate.toLong()) {
                val bitmap = retriever.getFrameAtTime(time, MediaMetadataRetriever.OPTION_CLOSEST)
                bitmap?.let { frameList.add(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            retriever.release()
        }
        return frameList
    }

    fun compressAndSaveBitmap(bitmap: Bitmap, outputPath: String) {
        try {
            FileOutputStream(outputPath).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}