package com.wangyiheng.vcamsx

import android.app.Application
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.os.Handler
import android.util.Log
import android.view.Surface
import cn.dianbobo.dbb.util.HLog
import com.wangyiheng.vcamsx.utils.VideoPlayer.c1_camera_play
import com.wangyiheng.vcamsx.utils.VideoPlayer.ijkMediaPlayer
import com.wangyiheng.vcamsx.utils.VideoPlayer.ijkplay_play
import com.wangyiheng.vcamsx.utils.VideoPlayer.initializeTheStateAsWellAsThePlayer
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlinx.coroutines.*
import java.util.*


class MainHook : IXposedHookLoadPackage {
    val TAG = "vcamsx"

    var contextUnhook:XC_MethodHook.Unhook ? = null

    companion object {
        @Volatile
        var data_buffer = byteArrayOf(0)
        var context: Context? = null
        var c2_builder: CaptureRequest.Builder? = null
        var origin_preview_camera: Camera? = null
        var fake_SurfaceTexture: SurfaceTexture? = null
        var original_preview_Surface: Surface? = null
        var original_c1_preview_SurfaceTexture:SurfaceTexture? = null
        var isPlaying:Boolean = false
        var needRecreate: Boolean = false
        var c2VirtualSurfaceTexture: SurfaceTexture? = null
        var c2_reader_Surfcae: Surface? = null
    }

    private var c2_virtual_surface: Surface? = null
    private var c2_state_callback_class: Class<*>? = null
    private var c2_state_callback: CameraDevice.StateCallback? = null

    // Xposed模块中
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if(lpparam.packageName == "com.wangyiheng.vcamsx"){
            return
        }
        if(lpparam.processName.contains(":")) {
            Log.d(TAG,"当前进程："+lpparam.processName)
            return
        }

        //获取context
        XposedHelpers.findAndHookMethod(
            "android.app.Instrumentation", lpparam.classLoader, "callApplicationOnCreate",
            Application::class.java, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    param?.args?.firstOrNull()?.let { arg ->
                        if (arg is Application) {
                            val applicationContext = arg.applicationContext
                            if (context != applicationContext) {
                                try {
                                    context = applicationContext
                                    if (!isPlaying) {
                                        isPlaying = true
                                        ijkMediaPlayer ?: initializeTheStateAsWellAsThePlayer()
                                    }
                                } catch (ee: Exception) {
                                    HLog.d(TAG, "$ee")
                                }
                            }
                        }
                    }
                }
            }
        )

        // 支持bilibili摄像头替换
        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "setPreviewTexture",
            SurfaceTexture::class.java, object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun beforeHookedMethod(param: MethodHookParam) {
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
                        SurfaceTexture(10)
                    } else {
                        fake_SurfaceTexture!!.release()
                        SurfaceTexture(10)
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
            "android.hardware.camera2.CameraManager", lpparam.classLoader, "openCamera",
            String::class.java,
            CameraDevice.StateCallback::class.java,
            Handler::class.java, object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        if(param.args[1] == null){
                            return
                        }
                        if(param.args[1] == c2_state_callback){
                            return
                        }
                        c2_state_callback = param.args[1] as CameraDevice.StateCallback
                        c2_state_callback_class = param.args[1]?.javaClass
                        process_camera2_init(c2_state_callback_class as Class<Any>?,lpparam)
                    }catch (e:Exception){
                        HLog.d("android.hardware.camera2.CameraManager报错了", "openCamera")
                    }
                }
            })
    }

    private fun process_camera2_init(c2StateCallbackClass: Class<Any>?, lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedHelpers.findAndHookMethod(c2StateCallbackClass, "onOpened", CameraDevice::class.java, object : XC_MethodHook() {
            @Throws(Throwable::class)
            override fun beforeHookedMethod(param: MethodHookParam) {
                needRecreate = true
                createVirtualSurface()

                c2_reader_Surfcae = null
                original_preview_Surface = null

                if(lpparam.packageName != "com.ss.android.ugc.aweme" ){
                    XposedHelpers.findAndHookMethod(param.args[0].javaClass, "createCaptureSession", List::class.java, CameraCaptureSession.StateCallback::class.java, Handler::class.java, object : XC_MethodHook() {
                        @Throws(Throwable::class)
                        override fun beforeHookedMethod(paramd: MethodHookParam) {
                            if (paramd.args[0] != null) {
                                paramd.args[0] = listOf(c2_virtual_surface)
                            }
                        }
                    })
                }

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
                            if(original_preview_Surface != param.args[0] as Surface ){
                                original_preview_Surface = param.args[0] as Surface
                            }
                        }else{
                            if(c2_reader_Surfcae == null && lpparam.packageName != "com.ss.android.ugc.aweme"){
                                c2_reader_Surfcae = param.args[0] as Surface
                            }
                        }
                        if(lpparam.packageName != "com.ss.android.ugc.aweme"){
                            param.args[0] = c2_virtual_surface
                        }
                    }
                }
            })

        XposedHelpers.findAndHookMethod("android.hardware.camera2.CaptureRequest.Builder", lpparam.classLoader, "build",object :XC_MethodHook(){
            @Throws(Throwable::class)
            override fun beforeHookedMethod(param: MethodHookParam) {
                ijkplay_play()
            }
        })
    }

    private fun createVirtualSurface(): Surface? {
        if (needRecreate) {
            c2VirtualSurfaceTexture?.release()
            c2VirtualSurfaceTexture = null

            c2_virtual_surface?.release()
            c2_virtual_surface = null

            c2VirtualSurfaceTexture = SurfaceTexture(15)
            c2_virtual_surface = Surface(c2VirtualSurfaceTexture)
            needRecreate = false
        } else if (c2_virtual_surface == null) {
            needRecreate = true
            c2_virtual_surface = createVirtualSurface()
        }
        return c2_virtual_surface
    }
}

