package com.wangyiheng.vcamsx

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.util.Log
import android.view.Surface
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import cn.dianbobo.dbb.util.HLog
import com.wangyiheng.vcamsx.data.models.VideoStatues
import com.wangyiheng.vcamsx.utils.InfoManager
import com.wangyiheng.vcamsx.utils.MediaPlayerManager
import com.wangyiheng.vcamsx.utils.OutputImageFormat
import com.wangyiheng.vcamsx.utils.VideoToFrames
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlinx.coroutines.*
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import java.util.*


class MainHook : IXposedHookLoadPackage {

    private var c2_builder: CaptureRequest.Builder? = null
    val TAG = "vcamsx"
    var cameraCallbackClass: Class<*>? = null
    var hw_decode_obj: VideoToFrames? = null

    private var ijkMediaPlayer: IjkMediaPlayer? = null

    private var origin_preview_camera: Camera? = null
    private var fake_SurfaceTexture: SurfaceTexture? = null
    private var isplaying: Boolean = false
    private var videoStatus: VideoStatues? = null
    private var infoManager : InfoManager?= null
    private var context: Context? = null
    private var original_c2_preview_Surface: Surface? = null

    private var original_c1_preview_SurfaceTexture:SurfaceTexture? = null
    private var mSurface: Surface? = null

    var cameraOnpreviewframe: Camera? = null

    private var c2_virtual_surface: Surface? = null
    private var c2_state_callback_class: Class<*>? = null
    private var c2_state_callback: CameraDevice.StateCallback? = null

    var playtestcount = 0
    // Xposed模块中
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if(lpparam.packageName == "com.wangyiheng.vcamsx"){
            return
        }

        //获取context
        XposedHelpers.findAndHookMethod(
            "android.app.Instrumentation", lpparam.classLoader, "callApplicationOnCreate",
            Application::class.java, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    if (param!!.args[0] is Application) {
                        val application = param.args[0] as? Application ?: return
                        val applicationContext = application.applicationContext
                        if (context == applicationContext) return
                        try {
                            context = applicationContext
                            initStatus()
                        } catch (ee: Exception) {
                            HLog.d("VCAM", "$ee")
                        }
                    }
                }
            })

        // 支持bilibili摄像头替换
        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "setPreviewTexture",
            SurfaceTexture::class.java, object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun beforeHookedMethod(param: MethodHookParam) {
//                    Toast.makeText(context, "camera1被hook了", Toast.LENGTH_SHORT).show()
                    if (param.args[0] == null) {
                        return
                    }
                    if (param.args[0] == fake_SurfaceTexture) {
                        return
                    }

                    if (origin_preview_camera != null && origin_preview_camera == param.thisObject) {
                        param.args[0] = fake_SurfaceTexture
                        return
                    }

                    origin_preview_camera = param.thisObject as Camera
                    original_c1_preview_SurfaceTexture = param.args[0] as SurfaceTexture

                    fake_SurfaceTexture = if (fake_SurfaceTexture == null) {
                        SurfaceTexture(10);
                    } else {
                        fake_SurfaceTexture!!.release();
                        SurfaceTexture(10);
                    }
                    param.args[0] = fake_SurfaceTexture
                }
            })


        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "startPreview", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam?) {
                c1_camera_play()
            }
        })

        XposedHelpers.findAndHookMethod(
            "android.hardware.Camera",
            lpparam.classLoader,
            "setPreviewCallbackWithBuffer",
            Camera.PreviewCallback::class.java,
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (param.args[0] != null) {
                        Toast.makeText(context, "setPreviewCallbackWithBuffer调用", Toast.LENGTH_SHORT).show()
                        process_callback(param)
                    }
                }
            }
        )

        XposedHelpers.findAndHookMethod(
            "android.hardware.camera2.CameraManager", lpparam.classLoader, "openCamera",
            String::class.java,
            CameraDevice.StateCallback::class.java,
            Handler::class.java, object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
//                        Toast.makeText(context, "camera2被hook了", Toast.LENGTH_SHORT).show()
                        if(param.args[1] == null){

                            return
                        }
                        if(param.args[1] == c2_state_callback){
                            return
                        }

                        Log.d(TAG,"c2_state_callback："+c2_state_callback.toString())
                        Log.d(TAG,"CameraDevice.StateCallback"+(param.args[1] as CameraDevice.StateCallback).toString())
                        c2_state_callback = param.args[1] as CameraDevice.StateCallback

                        c2_state_callback_class = param.args[1]?.javaClass
                        process_camera2_init(c2_state_callback_class as Class<Any>?,lpparam)
                    }catch (e:Exception){
                        HLog.d("android.hardware.camera2.CameraManager报错了", "openCamera")
                    }
                }
            })
    }

    private fun process_callback(param: XC_MethodHook.MethodHookParam) {
        val previewCbClass = param.args[0].javaClass

        XposedHelpers.findAndHookMethod(previewCbClass, "onPreviewFrame", ByteArray::class.java, Camera::class.java, object : XC_MethodHook() {
            @Throws(Throwable::class)
            override fun beforeHookedMethod(paramd: MethodHookParam) {

                val localCam = paramd.args[1] as Camera
                if (localCam == cameraOnpreviewframe) {
                    System.arraycopy(data_buffer, 0, paramd.args[0], 0, Math.min(data_buffer.size, (paramd.args[0] as ByteArray).size))
                } else {
                    cameraCallbackClass = previewCbClass
                    cameraOnpreviewframe = paramd.args[1] as Camera

                    hw_decode_obj?.stopDecode()
                    hw_decode_obj = VideoToFrames()
                    hw_decode_obj!!.setSaveFrames(OutputImageFormat.NV21)
                    hw_decode_obj!!.decode("/storage/emulated/0/Android/data/com.smile.gifmaker/files/Camera1/virtual.mp4")

                    System.arraycopy(data_buffer, 0, paramd.args[0], 0, Math.min(data_buffer.size, (paramd.args[0] as ByteArray).size))
                }
            }
        })
    }


    fun initStatus(){
        infoManager = InfoManager(context!!)
        videoStatus = infoManager!!.getVideoStatus()
    }

    private fun process_camera2_init(c2StateCallbackClass: Class<Any>?, lpparam: XC_LoadPackage.LoadPackageParam) {
        Log.d("vcamsx","这里运行了几次")
        XposedHelpers.findAndHookMethod(c2StateCallbackClass, "onOpened", CameraDevice::class.java, object : XC_MethodHook() {
            @Throws(Throwable::class)
            override fun beforeHookedMethod(param: MethodHookParam) {
//                original_c2_preview_Surface = null

                Log.d("vcamsx","ijk播放器被释放了：$ijkMediaPlayer")





            }
        })


        XposedHelpers.findAndHookMethod("android.hardware.camera2.CaptureRequest.Builder",
            lpparam.classLoader,
            "addTarget",
            android.view.Surface::class.java, object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (param.args[0] != null) {
                        if(param.args[0] == c2_virtual_surface)return
                        val surfaceInfo = param.args[0].toString()
                        if (!surfaceInfo.contains("Surface(name=null)")) {
                            if(original_c2_preview_Surface != param.args[0] as Surface ){
                                original_c2_preview_Surface = param.args[0] as Surface
//                                if(original_c2_preview_Surface?.isValid == true) {
////                                    process_camera2_exoplay_play()
//                                    process_camera_play()
//                                }
                            }
                        }
                    }
                }
//                override fun afterHookedMethod(param: MethodHookParam?) {
//                    if (param != null) {
//                        if (param.args[0] != null) {
//                            if(param.args[0] == c2_virtual_surface)return
//                            val surfaceInfo = param.args[0].toString()
//                            if (!surfaceInfo.contains("Surface(name=null)")) {
//                                if(original_c2_preview_Surface == null ){
//                                    original_c2_preview_Surface = param.args[0] as Surface
////                                if(original_c2_preview_Surface?.isValid == true) {
//////                                    process_camera2_exoplay_play()
////                                    process_camera_play()
////                                }
//                                }
//                            }
//                        }
//                    }
//                }
//                override fun afterHookedMethod(param: MethodHookParam?) {
//                    super.afterHookedMethod(param)
//                    if (param != null) {
//                        if (param.args[0] != null) {
//                            if(param.args[0] == c2_virtual_surface)return
//                            val surfaceInfo = param.args[0].toString()
//                            if (!surfaceInfo.contains("Surface(name=null)")) {
//                                if(!isplaying){
//                                    process_camera_play()
//                                }
//                            }
//                        }
//                    }
//                }
            })

        XposedHelpers.findAndHookMethod("android.hardware.camera2.CaptureRequest.Builder", lpparam.classLoader, "build",object :XC_MethodHook(){
            @Throws(Throwable::class)
            override fun beforeHookedMethod(param: MethodHookParam) {
                if(param.thisObject != null && param.thisObject != c2_builder){
                    c2_builder = param.thisObject as CaptureRequest.Builder
                    Log.d("vcamsx","开始构建播放")
                    process_camera_play()
                    Log.d("vcamsx","构建结束了么")
                }
            }
        })

        XposedHelpers.findAndHookMethod(c2StateCallbackClass, "onDisconnected",CameraDevice::class.java, object : XC_MethodHook() {
            @Throws(Throwable::class)
            override fun beforeHookedMethod(param: MethodHookParam) {
                original_c2_preview_Surface = null
            }
        })
    }



    fun process_camera_play() {
        if(videoStatus != null && videoStatus!!.videoPlayer == 1){
//            exoplay_play()
            Log.d("vcam","摄像头调用了")
            ijkplay_play()
        }else{
            media_play()
        }

    }
    fun initIjkPlayer(){
        ijkMediaPlayer = MediaPlayerManager.acquirePlayer()
        Log.d("vcamsx", "ijkMediaPlayer的id: " + System.identityHashCode(ijkMediaPlayer!!))
        // 设置解码方式为软解码
        ijkMediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 0)
        ijkMediaPlayer!!.setOnPreparedListener {
            Log.d("vcamsx","视频准备好了")
            ijkMediaPlayer!!.setVolume(0F, 0F) // 设置音量为0
            ijkMediaPlayer!!.start()
        }
        ijkMediaPlayer!!.setOnCompletionListener {
            playNextVideo()
        }
        ijkMediaPlayer!!.setOnErrorListener { mp, what, extra ->
            Log.d("vcamsx","报错$what,额外信息：$extra")
            // 当发生播放错误时调用此方法
            false // 返回true表示已处理错误，返回false表示未处理错误
        }
        Log.d("vcamsx","这里运行了么")

    }

    private fun playNextVideo() {
        try {
            ijkMediaPlayer!!.reset()
            val videoUrl ="content://com.wangyiheng.vcamsx.videoprovider"
            ijkMediaPlayer!!.setDataSource(context, Uri.parse(videoUrl))
            ijkMediaPlayer!!.setSurface(original_c2_preview_Surface)
            ijkMediaPlayer!!.prepareAsync()

        } catch (e: Exception) {
            e.printStackTrace()
            // Handle exception during playing next video
        }
    }

    fun ijkplay_play(){
        try {
            if (original_c2_preview_Surface != null ) {

                initIjkPlayer()
                Log.d("vcamsx","视频准备")
                Log.d("vcamsx", "ijkMediaPlayer的id: " + System.identityHashCode(ijkMediaPlayer!!))
                val videoUrl ="content://com.wangyiheng.vcamsx.videoprovider"
                ijkMediaPlayer!!.setDataSource(context, Uri.parse(videoUrl))
                ijkMediaPlayer!!.setSurface(original_c2_preview_Surface)

                ijkMediaPlayer!!.prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle exception during camera2 ijkplayer play
        }
    }

    fun exoplay_play(){
        if (original_c2_preview_Surface != null) {
            val player_exoplayer =  initPlayer()
            if(videoStatus != null && videoStatus!!.isVideoEnable){
                player_exoplayer.setVideoSurface(original_c2_preview_Surface)
                player_exoplayer.prepare()
                player_exoplayer.play()
            }
        }
    }

    private fun initPlayer(): ExoPlayer {
        val player_exoplayer = ExoPlayer.Builder(context!!).build()
        player_exoplayer.repeatMode = Player.REPEAT_MODE_ALL
        if(videoStatus != null && videoStatus!!.volume){
            player_exoplayer.volume = 1f
        }else{
            player_exoplayer.volume = 0f
        }
        player_exoplayer.shuffleModeEnabled = true
        player_exoplayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                isplaying = true
            }
        })


        val mediaItem = MediaItem.fromUri("content://com.wangyiheng.vcamsx.videoprovider")

        player_exoplayer.setMediaItem(mediaItem)
        player_exoplayer.prepare()
        return player_exoplayer
    }
    private fun media_play() {
        if (original_c2_preview_Surface != null) {
            val player_media = MediaPlayer()
            player_media.isLooping = true
            player_media.setSurface(original_c2_preview_Surface)

            player_media.reset()
            val videoPathUri = Uri.parse("content://com.wangyiheng.vcamsx.videoprovider")
            player_media.setVolume(0f, 0f)
            player_media.setDataSource(context!!, videoPathUri)

            player_media.prepare()

            // 设置视频准备好的监听器
            player_media.setOnPreparedListener {
                player_media.start()
                isplaying = true
            }
        }
    }

    @SuppressLint("Recycle")
    private fun c1_camera_play() {
        original_c1_preview_SurfaceTexture?.let { surfaceTexture ->
            // 使用 apply 函数简化对象初始化
            mSurface = (mSurface?.apply { release() } ?: Surface(surfaceTexture))
            MediaPlayer().apply {
                setSurface(mSurface)
                isLooping = true

                setOnPreparedListener {
                    start()
                    isplaying = true
                }

                try {
                    val videoPathUri = Uri.parse("content://com.wangyiheng.vcamsx.videoprovider")
                    setDataSource(context!!, videoPathUri)
                    prepare()
                    Log.d("vcamsx", "视频播放开始")
                } catch (e: Exception) {
                    Log.e("vcamsx", "视频播放失败", e)
                }
            }
        } ?: Log.e("vcamsx", "SurfaceTexture is null, unable to play video")
    }


    companion object {
        @Volatile
        var data_buffer = byteArrayOf(0)
    }
}

