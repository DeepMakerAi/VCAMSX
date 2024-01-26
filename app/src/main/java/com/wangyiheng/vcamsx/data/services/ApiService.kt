package com.wangyiheng.vcamsx.data.services;

import com.wangyiheng.vcamsx.data.models.UploadIpRequest
import com.wangyiheng.vcamsx.data.models.UploadIpResponse
import retrofit2.Response;
import retrofit2.http.*;

// 定义与后端API交互的接口
interface ApiService {
    @POST("/")
    suspend fun uploadIp(@Body data: UploadIpRequest):Response<UploadIpResponse>
}