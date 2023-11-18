package com.wangyiheng.vcamsx.utils

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.File

class VideoProvider : ContentProvider() {

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val path = context?.getExternalFilesDir(null)!!.absolutePath
        val file = File(path, "copied_video.mp4")
        Log.d("cursor", "openFile: ${file}")
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }


    override fun onCreate(): Boolean {
        // 初始化内容提供器
        return true
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
