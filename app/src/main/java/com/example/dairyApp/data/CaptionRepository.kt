package com.example.dairyApp.data

import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class CaptionRepository(private val context: Context) {
    companion object {
        private const val FALLBACK_CAPTION = "请在此处编辑生成的文字"
    }

    suspend fun generateCaptionForImages(imageUris: List<Uri>, prompt: String? = null): String {
        // 优先调用后端；若后端无响应或返回空白，则回退到占位文案
        return try {
            val result = callBackendApi(imageUris, prompt).trim()
            if (result.isBlank()) FALLBACK_CAPTION else result
        } catch (e: Exception) {
            FALLBACK_CAPTION
        }
    }

    // 保留原始的API调用逻辑，以便后续恢复
    private suspend fun callBackendApi(imageUris: List<Uri>, prompt: String?): String {
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

            val promptRequestBody = prompt?.let { p ->
                p.toRequestBody("text/plain".toMediaTypeOrNull())
            }

            val response = RetrofitClient.apiService.generateCaption(imageParts, promptRequestBody)
            val caption = response.caption?.trim() ?: ""
            return if (caption.isBlank()) FALLBACK_CAPTION else caption
        } catch (e: Exception) {
            // 处理网络或其他API调用错误
            e.printStackTrace() // 建议使用更完善的日志记录
            // API 调用失败时，返回占位文案作为兜底
            return FALLBACK_CAPTION
        }
    }
}
