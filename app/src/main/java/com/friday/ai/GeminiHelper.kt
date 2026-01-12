package com.friday.ai

import com.friday.ai.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class GeminiHelper {

    // 🔐 API key from BuildConfig (secure)
    private val API_KEY = BuildConfig.GEMINI_API_KEY

    private val client = OkHttpClient()

    fun getGeminiReply(
        userInput: String,
        callback: (String) -> Unit
    ) {
        try {
            val url =
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=$API_KEY"

            val json = JSONObject().apply {
                put(
                    "contents",
                    listOf(
                        mapOf(
                            "parts" to listOf(
                                mapOf("text" to userInput)
                            )
                        )
                    )
                )
            }

            val body = json.toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).enqueue(object : okhttp3.Callback {

                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                    callback("I'm offline right now.")
                }

                override fun onResponse(
                    call: okhttp3.Call,
                    response: okhttp3.Response
                ) {
                    val responseBody = response.body?.string() ?: run {
                        callback("Empty response from Gemini.")
                        return
                    }

                    try {
                        val reply =
                            JSONObject(responseBody)
                                .getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text")

                        callback(reply.trim())
                    } catch (_: Exception) {
                        callback("I couldn't process that.")
                    }
                }
            })
        } catch (_: Exception) {
            callback("Gemini error.")
        }
    }
}
