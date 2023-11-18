package cn.dianbobo.dbb.util

import de.robv.android.xposed.XposedBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

object HLog {
    fun d(logtype:String?="虚拟摄像头", msg: String) {
        XposedBridge.log("$logtype:$msg")
    }
}