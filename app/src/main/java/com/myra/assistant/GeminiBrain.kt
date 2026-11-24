package com.myra.assistant

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class GeminiBrain(private val context: Context) {

    private val main = Handler(Looper.getMainLooper())

    private val persona = "तुम Jarvis हो, सागर का निजी वॉइस असिस्टेंट। " +
        "हमेशा हिंदी में, 2 से 3 छोटे वाक्यों में जवाब दो। " +
        "पैसे कमाने के बारे में पूछा जाए तो सच्चे और सुरक्षित तरीके बताओ, " +
        "कोई कमाई की गारंटी मत दो।"

    fun ask(question: String, onAnswer: (String) -> Unit) {
        val key = context.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
            .getString("gemini_key", "") ?: ""
        if (key.isEmpty()) {
            onAnswer("पहले ऐप में Gemini key सेव करो")
            return
        }
        thread {
            val answer = try {
                call(question, key)
            } catch (e: Exception) {
                "इंटरनेट या key में दिक्कत है"
            }
            main.post { onAnswer(answer) }
        }
    }

    private fun call(question: String, key: String): String {
        val url = URL(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
        )
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("x-goog-api-key", key)
        conn.connectTimeout = 15000
        conn.readTimeout = 30000
        conn.doOutput = true

        val body = JSONObject().apply {
            put("system_instruction", JSONObject().put(
                "parts", JSONArray().put(JSONObject().put("text", persona))
            ))
            put("contents", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", question)))
            }))
        }
        conn.outputStream.use { it.write(body.toString().toByteArray()) }

        val code = conn.responseCode
        if (code != 200) {
            return "AI से जवाब नहीं मिला, कोड $code"
        }
        val response = conn.inputStream.bufferedReader().readText()
        val parts = JSONObject(response)
            .getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
        return parts.getJSONObject(0).getString("text").trim()
    }
}
