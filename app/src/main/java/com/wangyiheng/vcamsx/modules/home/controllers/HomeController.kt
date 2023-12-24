package com.wangyiheng.vcamsx.modules.home.controllers

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.SurfaceHolder
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.wangyiheng.vcamsx.data.models.VideoStatues
import com.wangyiheng.vcamsx.utils.InfoManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import java.io.File

class HomeController: ViewModel(),KoinComponent {

    val isVideoEnabled  = mutableStateOf(false)
    val isVolumeEnabled = mutableStateOf(false)
    val videoPlayer = mutableStateOf(1)
    val codecType = mutableStateOf(true)
    val isLiveStreamingEnabled = mutableStateOf(false)

    val infoManager by inject<InfoManager>()
    var mediaPlayer: IjkMediaPlayer? = null
    private var retryCount = 0
    private val maxRetryCount = 5
    val isLiveStreamingDisplay =  mutableStateOf(false)
    val isVideoDisplay =  mutableStateOf(false)
    var liveURL = mutableStateOf("rtmp://ns8.indexforce.com/home/mystream")

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
        infoManager.saveVideoStatus(
            VideoStatues(
                isVideoEnabled.value,
                isVolumeEnabled.value,
                videoPlayer.value,
                codecType.value,
                isLiveStreamingEnabled.value
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
        }
    }


    fun playVideo(context: Context, holder: SurfaceHolder, videoPath: String) {
        mediaPlayer = IjkMediaPlayer().apply {
            setDataSource(videoPath)
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
                setOnPreparedListener { start() }
            } catch (e: Exception) {
                if (retryCount < maxRetryCount) {
                    retryCount++
//                    playRTMPStream(context, holder, rtmpUrl)
                }
            }
        }
    }

    fun release(){
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

//    private fun resetMediaPlayer() {
//        mediaPlayer?.stop()
//        mediaPlayer?.release()
//        mediaPlayer = IjkMediaPlayer()
//    }
}

