package com.wangyiheng.vcamsx.data.models

data class VideoStatues(
    val isVideoEnable:Boolean = false,
    val volume: Boolean = false,
    val videoPlayer:Int = 1,
    val codecType:Boolean = false,
    val isLiveStreamingEnabled:Boolean = false,
    val liveURL:String = "rtmp://ns8.indexforce.com/home/mystream"
)