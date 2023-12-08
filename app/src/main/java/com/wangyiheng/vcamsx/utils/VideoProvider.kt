package com.wangyiheng.vcamsx.utils

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.wangyiheng.vcamsx.R
import java.io.File

class VideoProvider : ContentProvider() {

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val filePath = extractContent(uri.toString())
        val file: File

        if (filePath == "vcamsx.mp4") {
            Log.d("vcamsx", filePath)

            // 获取外部文件目录
            val externalFilesDir = context?.getExternalFilesDir(null)?.absolutePath ?: return null

            // 创建一个指向 "dbb.mp4" 的文件对象
            val vcamsxFile = File(externalFilesDir, "dbb.mp4")

            // 检查文件是否存在，如果不存在，则从资源中复制
            if (!vcamsxFile.exists()) {
                context?.resources?.openRawResource(R.raw.vcamsx).use { inputStream ->
                    vcamsxFile.outputStream().use { fileOutputStream ->
                        inputStream?.copyTo(fileOutputStream)
                    }
                }
            }

            file = vcamsxFile
        }else{
            val path = context?.getExternalFilesDir(null)!!.absolutePath
            file = File(path, "copied_video.mp4")
        }

        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }


    override fun onCreate(): Boolean {
        // 初始化内容提供器
        return true
    }

    fun extractContent(url: String): String? {
        val prefix = "com.wangyiheng.vcamsx.videoprovider/"
        val index = url.indexOf(prefix)

        return if (index != -1) {
            url.substring(index + prefix.length)
        } else {
            null
        }
    }
    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor {
        // 创建MatrixCursor
        val cursor = MatrixCursor(arrayOf("_id", "display_name", "size", "date_modified","file"))
        val path = context?.getExternalFilesDir(null)!!.absolutePath
        val file = File(path, "advancedModeMovies/654e1835b70883406c4640c3/caibi_60.mp4")
        // 获取视频文件夹路径
        cursor.addRow(arrayOf(0, file.name, file.length(), file.lastModified(),file))
        Log.d("cursor", "query: ${file}")

        return cursor
    }

    // 其他方法根据需要实现，这里为了简单起见，我们留空
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
