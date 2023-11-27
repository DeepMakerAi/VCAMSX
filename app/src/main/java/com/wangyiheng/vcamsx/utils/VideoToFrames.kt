package com.wangyiheng.vcamsx.utils

import android.content.ContentValues.TAG
import android.graphics.ImageFormat
import android.media.*
import android.util.Log
import android.view.Surface
import com.wangyiheng.vcamsx.MainHook
import de.robv.android.xposed.XposedBridge
import java.io.IOException
import java.util.concurrent.LinkedBlockingQueue

class VideoToFrames : Runnable {

    private var stopDecode = false

    private var outputImageFormat: OutputImageFormat? = null
    private var videoFilePath: String? = null
    private var childThread: Thread? = null
    private var throwable: Throwable? = null // 定义 throwable 变量
    private val decodeColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
    private val play_surf: Surface? = null
    private val DEFAULT_TIMEOUT_US: Long = 10000
    private val callback: Callback? = null
    private val mQueue: LinkedBlockingQueue<ByteArray>? = null
    private val COLOR_FormatI420 = 1
    private val COLOR_FormatNV21 = 2
    private val VERBOSE = false
    fun stopDecode() {
        stopDecode = true
    }

    interface Callback {
        fun onFinishDecode()
        fun onDecodeFrame(index: Int)
    }

    @Throws(IOException::class)
    fun setSaveFrames(imageFormat: OutputImageFormat) {
        outputImageFormat = imageFormat
    }


    fun decode(videoFilePath: String) {
        this.videoFilePath = videoFilePath
        if (childThread == null) {
            childThread = Thread(this, "decode").apply {
                start()
            }
            throwable?.let { throw it }
        }
    }

    override fun run() {
        try {
            Log.d("vcamsxtoast","------开始解码------")
            videoFilePath?.let { videoDecode(it) }
        } catch (t: Throwable) {
            throwable = t
        }
    }

    private fun videoDecode(videoFilePath: String) {
        var extractor: MediaExtractor? = null
        var decoder: MediaCodec? = null

        try {
            extractor = MediaExtractor().apply {
                setDataSource(videoFilePath)
            }
            val trackIndex = selectTrack(extractor)
            if (trackIndex < 0) {
                XposedBridge.log("&#8203;``【oaicite:5】``&#8203;&#8203;``【oaicite:4】``&#8203;No video track found in $videoFilePath")
            }
            extractor.selectTrack(trackIndex)
            val mediaFormat = extractor.getTrackFormat(trackIndex)
            val mime = mediaFormat.getString(MediaFormat.KEY_MIME)
            decoder = MediaCodec.createDecoderByType(mime!!)
            showSupportedColorFormat(decoder.codecInfo.getCapabilitiesForType(mime))
            if (isColorFormatSupported(decodeColorFormat, decoder.codecInfo.getCapabilitiesForType(mime))) {
                mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, decodeColorFormat)
                XposedBridge.log("&#8203;``【oaicite:3】``&#8203;&#8203;``【oaicite:2】``&#8203;set decode color format to type $decodeColorFormat")
            } else {
                Log.i(TAG, "unable to set decode color format, color format type $decodeColorFormat not supported")
                XposedBridge.log("&#8203;``【oaicite:1】``&#8203;&#8203;``【oaicite:0】``&#8203;unable to set decode color format, color format type $decodeColorFormat not supported")
            }
            decodeFramesToImage(decoder, extractor, mediaFormat)
            decoder.stop()
            while (!stopDecode) {
                extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                decodeFramesToImage(decoder, extractor, mediaFormat)
                decoder.stop()
            }
        } catch (e: Exception) {
            // Handle exceptions
        } finally {
            if(decoder != null) {
                decoder.stop()
                decoder.release()
                decoder = null
            }
            if(extractor != null) {
                extractor.release()
                extractor = null
            }
        }
    }
    private fun selectTrack(extractor: MediaExtractor): Int {
        val numTracks = extractor.trackCount
        for (i in 0 until numTracks) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime!!.startsWith("video/")) {
                return i
            }
        }
        return -1
    }

    private fun showSupportedColorFormat(caps: MediaCodecInfo.CodecCapabilities) {
        print("supported color format: ")
        for (c in caps.colorFormats) {
            print("$c\t")
        }
        println()
    }

    fun isColorFormatSupported(colorFormat: Int, caps: MediaCodecInfo.CodecCapabilities): Boolean {
        return caps.colorFormats.any { it == colorFormat }
    }

    private fun decodeFramesToImage(decoder: MediaCodec, extractor: MediaExtractor, mediaFormat: MediaFormat) {
        var isFirst = false
        var startWhen: Long = 0
        val info = MediaCodec.BufferInfo()
        decoder.configure(mediaFormat, play_surf, null, 0)
        var sawInputEOS = false
        var sawOutputEOS = false
        decoder.start()
        var outputFrameCount = 0

        while (!sawOutputEOS && !stopDecode) {
            if (!sawInputEOS) {
                val inputBufferId = decoder.dequeueInputBuffer(DEFAULT_TIMEOUT_US)
                if (inputBufferId >= 0) {
                    val inputBuffer = decoder.getInputBuffer(inputBufferId)
                    val sampleSize = extractor.readSampleData(inputBuffer!!, 0)
                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(inputBufferId, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        sawInputEOS = true
                    } else {
                        val presentationTimeUs = extractor.sampleTime
                        decoder.queueInputBuffer(inputBufferId, 0, sampleSize, presentationTimeUs, 0)
                        extractor.advance()
                    }
                }
            }

            val outputBufferId = decoder.dequeueOutputBuffer(info, DEFAULT_TIMEOUT_US)
            if (outputBufferId >= 0) {
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    sawOutputEOS = true
                }
                val doRender = info.size != 0
                if (doRender) {
                    outputFrameCount++
                    callback?.onDecodeFrame(outputFrameCount)

                    if (!isFirst) {
                        startWhen = System.currentTimeMillis()
                        isFirst = true
                    }
                    if (play_surf == null) {
                        val image = decoder.getOutputImage(outputBufferId)
                        val buffer = image!!.planes[0].buffer
                        val arr = ByteArray(buffer.remaining())
                        buffer.get(arr)
                        mQueue?.put(arr)

                        if (outputImageFormat != null) {
                            MainHook.data_buffer = getDataFromImage(image, COLOR_FormatNV21)
                        }
                        image.close()
                    }

                    val sleepTime = info.presentationTimeUs / 1000 - (System.currentTimeMillis() - startWhen)
                    if (sleepTime > 0) {
                        try {
                            Thread.sleep(sleepTime)
                        } catch (e: InterruptedException) {
                            XposedBridge.log("&#8203;``【oaicite:1】``&#8203;" + e.toString())
                            XposedBridge.log("&#8203;``【oaicite:0】``&#8203;线程延迟出错")
                        }
                    }
                    decoder.releaseOutputBuffer(outputBufferId, true)
                }
            }
        }
        callback?.onFinishDecode()
    }
    private fun getDataFromImage(image: Image, colorFormat: Int): ByteArray {
        if (colorFormat != COLOR_FormatI420 && colorFormat != COLOR_FormatNV21) {
            throw IllegalArgumentException("only support COLOR_FormatI420 and COLOR_FormatNV21")
        }
        if (!isImageFormatSupported(image)) {
            throw RuntimeException("can't convert Image to byte array, format ${image.format}")
        }

        val crop = image.cropRect
        val format = image.format
        val width = crop.width()
        val height = crop.height()
        val planes = image.planes
        val data = ByteArray(width * height * ImageFormat.getBitsPerPixel(format) / 8)
        val rowData = ByteArray(planes[0].rowStride)
        if (VERBOSE) Log.v(TAG, "get data from ${planes.size} planes")

        var channelOffset = 0
        var outputStride = 1
        for (i in planes.indices) {
            when (i) {
                0 -> {
                    channelOffset = 0
                    outputStride = 1
                }
                1 -> {
                    channelOffset = if (colorFormat == COLOR_FormatI420) width * height else width * height + 1
                    outputStride = 2
                }
                2 -> {
                    channelOffset = if (colorFormat == COLOR_FormatI420) (width * height * 1.25).toInt() else width * height
                    outputStride = 2
                }
            }
            val buffer = planes[i].buffer
            val rowStride = planes[i].rowStride
            val pixelStride = planes[i].pixelStride
            if (VERBOSE) {
                Log.v(TAG, "pixelStride $pixelStride")
                Log.v(TAG, "rowStride $rowStride")
                Log.v(TAG, "width $width")
                Log.v(TAG, "height $height")
                Log.v(TAG, "buffer size ${buffer.remaining()}")
            }

            val shift = if (i == 0) 0 else 1
            val w = width shr shift
            val h = height shr shift
            buffer.position(rowStride * (crop.top shr shift) + pixelStride * (crop.left shr shift))
            for (row in 0 until h) {
                val length: Int
                if (pixelStride == 1 && outputStride == 1) {
                    length = w
                    buffer.get(data, channelOffset, length)
                    channelOffset += length
                } else {
                    length = (w - 1) * pixelStride + 1
                    buffer.get(rowData, 0, length)
                    for (col in 0 until w) {
                        data[channelOffset] = rowData[col * pixelStride]
                        channelOffset += outputStride
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length)
                }
            }
            if (VERBOSE) Log.v(TAG, "Finished reading data from plane $i")
        }
        return data
    }

    private fun isImageFormatSupported(image: Image): Boolean {
        val format = image.format
        return when (format) {
            ImageFormat.YUV_420_888, ImageFormat.NV21, ImageFormat.YV12 -> true
            else -> false
        }
    }
}


enum class OutputImageFormat(val friendlyName: String) {
    I420("I420"),
    NV21("NV21"),
    JPEG("JPEG");

    override fun toString() = friendlyName
}


