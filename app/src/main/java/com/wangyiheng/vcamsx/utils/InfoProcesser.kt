package com.wangyiheng.vcamsx.utils

import com.wangyiheng.vcamsx.MainHook
import com.wangyiheng.vcamsx.data.models.VideoStatues

object InfoProcesser {
    var videoStatus: VideoStatues? = null
    var infoManager : InfoManager?= null


    fun initStatus(){
        infoManager = InfoManager(MainHook.context!!)
        videoStatus = infoManager!!.getVideoStatus()
    }
}