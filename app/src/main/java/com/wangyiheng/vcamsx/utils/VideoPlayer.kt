package com.wangyiheng.vcamsx.utils

import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.view.Surface
import android.widget.Toast
import com.wangyiheng.vcamsx.MainHook
import com.wangyiheng.vcamsx.MainHook.Companion.c2_reader_Surfcae
import com.wangyiheng.vcamsx.MainHook.Companion.context
import com.wangyiheng.vcamsx.MainHook.Companion.original_c1_preview_SurfaceTexture
import com.wangyiheng.vcamsx.MainHook.Companion.original_preview_Surface
import com.wangyiheng.vcamsx.utils.InfoProcesser.videoStatus
import tv.danmaku.ijk.media.player.IjkMediaPlayer

object VideoPlayer {
    var ijkMediaPlayer: IjkMediaPlayer? = null
    var c3_player: MediaPlayer? = null
    var copyReaderSurface:Surface? = null
    // 公共配置方法
    private fun configureMediaPlayer(mediaPlayer: IjkMediaPlayer) {
        mediaPlayer.apply {
            // 公共的错误监听器
            setOnErrorListener { _, what, extra ->
                Log.e("IjkMediaPlayer", "Error occurred. What: $what, Extra: $extra")
                Toast.makeText(context, "播放错误: $what", Toast.LENGTH_SHORT).show()
                true
            }

            // 公共的信息监听器
            setOnInfoListener { _, what, extra ->
                true
            }
        }
    }

    // RTMP流播放器初始化
    fun initRTMPStreamPlayer() {
        ijkMediaPlayer = IjkMediaPlayer().apply {
            // 硬件解码设置
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

            // 应用公共配置
            configureMediaPlayer(this)

            // 设置 RTMP 流的 URL
            dataSource = videoStatus!!.liveURL

            // 异步准备播放器
            prepareAsync()

            // 准备好后的操作
            setOnPreparedListener {
                Log.d("IjkMediaPlayer", "RTMP Stream prepared. Starting playback.")
                original_preview_Surface?.let { setSurface(it) }
                Toast.makeText(context, "直播接收成功", Toast.LENGTH_SHORT).show()
                start()
            }
        }
    }

    // 视频播放器初始化
    fun initVideoPlayer() {
        ijkMediaPlayer = IjkMediaPlayer().apply {
            setVolume(0F, 0F) // 静音播放

            // 设置解码方式
            val codecType = videoStatus?.codecType
            val mediaCodecOption = if (codecType == true) 1L else 0L
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", mediaCodecOption)

            // 其他播放器配置
            setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probsize", 4096)
            setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "max-buffer-size", 8192)
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1)
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 0)
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "min-frames", 2)

            // 应用公共配置
            configureMediaPlayer(this)

            // 准备好后的操作
            setOnPreparedListener {
                isLooping = true
                original_preview_Surface?.let { setSurface(it) }
                Toast.makeText(context, "视频开始播放了", Toast.LENGTH_SHORT).show()
                start()
            }

            // 错误处理
            setOnErrorListener { _, what, extra ->
                Log.d("vcamsx","报错了$what")
                true // 已处理错误
            }

            // 设置数据源
            val videoUrl = "content://com.wangyiheng.vcamsx.videoprovider"
            setDataSource(context, Uri.parse(videoUrl))
            // 准备播放器
            prepareAsync()
        }
    }

    fun initializeTheStateAsWellAsThePlayer(){
        InfoProcesser.initStatus()

        if(ijkMediaPlayer == null){
            if(videoStatus?.isLiveStreamingEnabled == true){
                initRTMPStreamPlayer()
            }else if(videoStatus?.isVideoEnable == true){
                initVideoPlayer()
            }
        }
    }


    private fun handleMediaPlayer(surface: Surface) {
        try {
            Log.d("vcamsx","开始投屏")
            InfoProcesser.initStatus()
            videoStatus?.let { status ->
                val volume = if (status.isVideoEnable && status.volume) 1F else 0F
                ijkMediaPlayer?.apply {
                    setVolume(volume, volume)
                    if (status.isVideoEnable || status.isLiveStreamingEnabled) {
                        setSurface(surface)
                    }
                }
            }
        } catch (e: Exception) {
            // 这里可以添加更详细的异常处理或日志记录
            e.printStackTrace()
        }
    }


    fun ijkplay_play() {
        original_preview_Surface?.let { surface ->
            handleMediaPlayer(surface)
        }

        Log.d("vcamsx", c2_reader_Surfcae.toString())
        c2_reader_Surfcae?.let { surface ->
            c2_reader_play(surface)
        }

    }

    fun c1_camera_play() {
        if (original_c1_preview_SurfaceTexture != null && videoStatus?.isVideoEnable == true) {
            original_preview_Surface = Surface(original_c1_preview_SurfaceTexture)
            if(original_preview_Surface!!.isValid == true){
                handleMediaPlayer(original_preview_Surface!!)
            }
        }

        c2_reader_Surfcae?.let { surface ->
            c2_reader_play(surface)
        }

    }

    fun c2_reader_play(c2_reader_Surfcae:Surface){

        if(c2_reader_Surfcae == copyReaderSurface){
            return
        }
        copyReaderSurface = c2_reader_Surfcae

        if (c3_player == null) {
            c3_player = MediaPlayer()
        } else {
            c3_player!!.release()
            c3_player = MediaPlayer()
        }
        c3_player!!.setVolume(0f, 0f)
        c3_player!!.setSurface(c2_reader_Surfcae)

        c3_player!!.setLooping(true)


        try {
            c3_player!!.setOnPreparedListener {
                c3_player!!.start()
            }
            val videoUrl = "content://com.wangyiheng.vcamsx.videoprovider"
            val videoPathUri = Uri.parse(videoUrl)
            c3_player!!.setVolume(0f, 0f)
            c3_player!!.setDataSource(context, videoPathUri)
            c3_player!!.prepare()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

}