package cn.dianbobo.dbb.util

import android.content.Context
import android.util.Log
import de.robv.android.xposed.XposedBridge
import java.io.*
import java.util.*
import com.bigkoo.pickerview.view.WheelTime.dateFormat
object HLog {
    var lastTransitionTime: Long = 0 // 初始化为0
    val logBuffer = mutableListOf<String>()
    val MAX_LOG_ENTRIES = 5
    fun d(logtype:String?="虚拟摄像头", msg: String) {
        XposedBridge.log("$logtype:$msg")
    }
    fun localeLog(context: Context,msg:String) {
        val currentTimeMillis = System.currentTimeMillis()
        val formattedDate = dateFormat.format(Date(currentTimeMillis))

        val timeInterval = if (lastTransitionTime != 0L) {
            (currentTimeMillis - lastTransitionTime)  // 将毫秒转换为秒
        } else {
            0L
        }
        // 更新上次切换时间
        lastTransitionTime = currentTimeMillis
        val logMessage = "时间：$formattedDate\n$msg \n日志间隔时间：${timeInterval}毫秒"
        Log.d("dbb",logMessage)

        // 将日志消息添加到缓冲区
        logBuffer.add(logMessage)

        // 如果缓冲区中的日志条目达到二十条，则保存到文件并清空缓冲区
        if (logBuffer.size >= MAX_LOG_ENTRIES) {
            saveLogsToFile(context)
        }
    }
    private fun saveLogsToFile(context: Context) {
        val logFileDir = context.getExternalFilesDir(null)!!.absolutePath
        val logFilePath = File(logFileDir, "log.txt")

        try {
            // 将缓冲区中的日志消息写入文件
            logBuffer.forEach { logMessage ->
                logFilePath.appendText(logMessage + "\n\n")
            }
            // 清空缓冲区
            logBuffer.clear()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}