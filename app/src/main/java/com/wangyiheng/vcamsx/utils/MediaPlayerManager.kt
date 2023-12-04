package com.wangyiheng.vcamsx.utils

import android.util.Log
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import java.util.*

object MediaPlayerManager {
    private const val MAX_PLAYER_COUNT = 5 // 最大播放器数量
    private val playerQueue = LinkedList<IjkMediaPlayer>()

    init {
        // 初始化播放器队列
        repeat(MAX_PLAYER_COUNT) {
            val mediaPlayer = IjkMediaPlayer()
            mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 0)


            playerQueue.add(mediaPlayer)
        }
    }

    private var currentPlayingPlayer: IjkMediaPlayer? = null

    fun acquirePlayer(): IjkMediaPlayer {
        // 释放之前的播放器对象
        Log.d("dbb",playerQueue.toString())
        currentPlayingPlayer?.let {
            releasePlayer(it)
        }


        return if (playerQueue.isNotEmpty()) {
            currentPlayingPlayer = playerQueue.poll() // 获取可用的播放器对象并设置为当前播放器
            currentPlayingPlayer!!
        } else {
            currentPlayingPlayer = IjkMediaPlayer() // 如果队列为空，创建一个新的播放器对象并设置为当前播放器
            currentPlayingPlayer!!
        }
    }

    private fun releasePlayer(player: IjkMediaPlayer?) {
        player?.apply {
            reset()
            playerQueue.offer(this) // 重置播放器并放回队列中
        }
    }
}
