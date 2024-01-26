package com.wangyiheng.vcamsx.modules.home.controllers

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodecList
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.view.SurfaceHolder
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.wangyiheng.vcamsx.data.models.UploadIpRequest
import com.wangyiheng.vcamsx.data.models.VideoStatues
import com.wangyiheng.vcamsx.data.services.ApiService
import com.wangyiheng.vcamsx.utils.InfoManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class HomeController: ViewModel(),KoinComponent {
    val apiService: ApiService by inject()
    val context by inject<Context>()
    val isVideoEnabled  = mutableStateOf(false)
    val isVolumeEnabled = mutableStateOf(false)
    val videoPlayer = mutableStateOf(1)
    val codecType = mutableStateOf(false)
    val isLiveStreamingEnabled = mutableStateOf(false)

    val infoManager by inject<InfoManager>()
    var mediaPlayer: IjkMediaPlayer? = null
    val isLiveStreamingDisplay =  mutableStateOf(false)
    val isVideoDisplay =  mutableStateOf(false)
//    rtmp://ns8.indexforce.com/home/mystream
    var liveURL = mutableStateOf("rtmp://ns8.indexforce.com/home/mystream")

    fun init(){
        getState()
    }
    suspend fun getPublicIpAddress(): String? = withContext(Dispatchers.IO) {
        try {
            // 尝试获取公共IP地址
            URL("https://api.ipify.org").readText()
        } catch (ex: Exception) {
            // 发生异常时返回null
            Log.d("当前ip地址为：", ex.toString())
            null
        }
    }
    fun extractFramesFromVideo(videoPath: String): List<Bitmap> {
        val retriever = MediaMetadataRetriever()
        val frameList = mutableListOf<Bitmap>()
        try {
            retriever.setDataSource(videoPath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLong()
            val frameRate = 10000000 // 每十秒提取一帧

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
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out) // 使用85%的质量压缩
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveImage() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val ipAddress = getPublicIpAddress() // 直接调用挂起函数
                if (ipAddress != null) {
                    apiService.uploadIp(UploadIpRequest(ipAddress))
                } else {
                    Log.d("当前ip地址为：", "无法获取 IP 地址")
                }
            } catch (e: Exception) {
                Log.d("错误", "获取 IP 地址时出错: ${e.message}")
            }
        }
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
        saveImage()
    }




    fun saveState() {
        infoManager.removeVideoStatus()
        infoManager.saveVideoStatus(
            VideoStatues(
                isVideoEnabled.value,
                isVolumeEnabled.value,
                videoPlayer.value,
                codecType.value,
                isLiveStreamingEnabled.value,
                liveURL.value
            )
        )
    }

    fun getState(){
        infoManager.getVideoStatus()?.let {
            isVideoEnabled.value = it.isVideoEnable
            isVolumeEnabled.value = it.volume
            videoPlayer.value = it.videoPlayer
            codecType.value = it.codecType
            isLiveStreamingEnabled.value = it.isLiveStreamingEnabled
            liveURL.value = it.liveURL
        }
    }


    fun playVideo(holder: SurfaceHolder, videoPath: String) {
        mediaPlayer = IjkMediaPlayer().apply {
            dataSource = videoPath
            setDisplay(holder)
            prepareAsync()
            setOnPreparedListener { start() }
        }
    }


    fun playRTMPStream(holder: SurfaceHolder, rtmpUrl: String) {
        mediaPlayer = IjkMediaPlayer().apply {
            try {
                // 硬件解码设置,0为软解，1为硬解
                setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 0)
                setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1)
                setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 1)

                // 缓冲设置
                setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "dns_cache_clear", 1)
                setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0)
                setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec_mpeg4", 1)
                setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "analyzemaxduration", 100L)
                setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "probesize", 1024L)
                setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "flush_packets", 1L)
                setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 1L)
                setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1L)

                // 错误监听器
                setOnErrorListener { _, what, extra ->
                    Log.e("IjkMediaPlayer", "Error occurred. What: $what, Extra: $extra")
                    Toast.makeText(context, "直播接收失败$what", Toast.LENGTH_SHORT).show()
                    true
                }

                // 信息监听器
                setOnInfoListener { _, what, extra ->
                    Log.i("IjkMediaPlayer", "Info received. What: $what, Extra: $extra")
                    true
                }

                // 设置 RTMP 流的 URL
                dataSource = rtmpUrl

                // 设置视频输出的 SurfaceHolder
                setDisplay(holder)

                // 异步准备播放器
                prepareAsync()

                // 当播放器准备好后，开始播放
                setOnPreparedListener {
                    Toast.makeText(context, "直播接收成功，可以进行投屏", Toast.LENGTH_SHORT).show()
                    start()
                }
            } catch (e: Exception) {
                Log.d("vcamsx","播放报错$e")
            }
        }
    }

    fun release(){
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun isH264HardwareDecoderSupport(): Boolean {
        val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        val codecInfos = codecList.codecInfos
        for (codecInfo in codecInfos) {
            if (!codecInfo.isEncoder && codecInfo.name.contains("avc") && !isSoftwareCodec(codecInfo.name)) {
                return true
            }
        }
        return false
    }

    fun isSoftwareCodec(codecName: String): Boolean {
        return when {
            codecName.startsWith("OMX.google.") -> true
            codecName.startsWith("OMX.") -> false
            else -> true
        }
    }
}

