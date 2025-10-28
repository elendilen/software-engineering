package com.example.dairyApp.data

import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class CaptionRepository(private val context: Context) {

    suspend fun generateCaptionForImages(imageUris: List<Uri>): String {
        // 后端可用时，使用以下真实API调用
        // return callBackendApi(imageUris)

        // 后端不可用时的临时方案：返回一个默认的可编辑标题
        return "请在此处编辑生成的文字"
    }

    // 保留原始的API调用逻辑，以便后续恢复
    private suspend fun callBackendApi(imageUris: List<Uri>): String {
        try {
            val imageParts = imageUris.map { uri ->
                // 将Uri转换为MultipartBody.Part
                val fileStream = context.contentResolver.openInputStream(uri)
                val requestBody = fileStream?.readBytes()?.toRequestBody(
                    "image/*".toMediaTypeOrNull()
                )
                fileStream?.close()
                // "images" 是后端期望的字段名
                MultipartBody.Part.createFormData("images", "image.jpg", requestBody!!)
            }

            val response = RetrofitClient.apiService.generateCaption(imageParts)
            return response.caption
        } catch (e: Exception) {
            // 处理网络或其他API调用错误
            e.printStackTrace() // 建议使用更完善的日志记录
            // API调用失败时，也可以返回默认标题或抛出自定义异常
            return "API调用失败，请手动编辑"
        }
    }
}
