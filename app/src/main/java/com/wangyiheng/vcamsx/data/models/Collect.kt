package com.wangyiheng.vcamsx.data.models

data class UploadIpRequest(
    val ip: String // 确保字段名与服务器期望的匹配
)

data class UploadIpResponse(
    val result: Result
)

data class Result(
    val isSuccess: Boolean,
    val ipcount: Int
)