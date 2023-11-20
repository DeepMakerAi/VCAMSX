package com.wangyiheng.vcamsx.utils

import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer

class VideoPlayer {
    var dataSourceFactory: DefaultDataSource.Factory? = null
    var player_exoplayer: ExoPlayer? = null
}