package com.example.dairyApp.data

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @Multipart
    @POST("v1/generate-caption") // 你的API端点
    suspend fun generateCaption(
        @Part images: List<MultipartBody.Part>,
        @Part("prompt") prompt: RequestBody?
    ): CaptionResponse // 定义一个响应数据类
}

data class CaptionResponse(val caption: String)