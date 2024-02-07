package com.wangyiheng.vcamsx.data.models

data class VideoInfo(
    val videoId: Int = 0,
    val videoName: String = "vcamsx",
    val videoUrl: String ="",
    val videoType: String = "mp4"
)

data class VideoInfos(
    val videos: List<VideoInfo>
)