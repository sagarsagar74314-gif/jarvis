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
    private val prefs = context.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
    private val base = "https://generativelanguage.googleapis.com/v1beta/"

    private val persona = "तुम Jarvis हो, सागर का निजी वॉइस असिस्टेंट। " +
        "हमेशा हिंदी में, 2 से 3 छोटे वाक्यों में जवाब दो। " +
        "पैसे कमाने के बारे में पूछा जाए तो सच्चे और सुरक्षित तरीके बताओ, " +
        "कोई कमाई की गारंटी मत दो।"

    fun ask(question: String, onAnswer: (String) -> Unit) {
        val key = prefs.getString("gemini_key", "") ?: ""
        if (key.isEmpty()) {
            onAnswer("पहले ऐप में Gemini key सेव करो")
            return
        }
        thread {
            val answer = try {
                respond(question, key)
            } catch (e: Exception) {
                "इंटरनेट या key में दिक्कत है"
            }
            main.post { onAnswer(answer) }
        }
    }

    private fun respond(question: String, key: String): String {
        var model = prefs.getString("gemini_model", "") ?: ""
        if (model.isEmpty()) {
            model = pickModel(key) ?: return "कोई Gemini मॉडल नहीं मिला, key जाँचो"
            prefs.edit().putString("gemini_model", model).apply()
        }

        var result = call(question, key, model)

        // मॉडल अगर हट गया हो तो दोबारा चुनकर एक बार फिर कोशिश करो
        if (result.first == 404) {
            prefs.edit().remove("gemini_model").apply()
            val fresh = pickModel(key) ?: return "कोई Gemini मॉडल नहीं मिला, key जाँचो"
            prefs.edit().putString("gemini_model", fresh).apply()
            model = fresh
            result = call(question, key, model)
        }

        if (result.first != 200) {
            return "AI से जवाब नहीं मिला, कोड ${result.first}"
        }

        val parts = JSONObject(result.second)
            .getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
        val builder = StringBuilder()
        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            if (part.optBoolean("thought", false)) continue
            builder.append(part.optString("text", ""))
        }
        val text = builder.toString().trim()
        return if (text.isEmpty()) "AI ने कोई जवाब नहीं दिया" else text
    }

    private fun call(question: String, key: String, model: String): Pair<Int, String> {
        val url = URL(base + "models/" + model + ":generateContent")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("x-goog-api-key", key)
        conn.connectTimeout = 15000
        conn.readTimeout = 40000
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
        val stream = if (code == 200) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.readText() ?: ""
        return Pair(code, text)
    }

    private fun pickModel(key: String): String? {
        val conn = URL(base + "models?pageSize=200").openConnection() as HttpURLConnection
        conn.setRequestProperty("x-goog-api-key", key)
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        if (conn.responseCode != 200) return null

        val body = conn.inputStream.bufferedReader().readText()
        val models = JSONObject(body).getJSONArray("models")
        val skip = listOf(
            "tts", "image", "embedding", "live", "audio", "robotics",
            "computer", "vision", "native", "imagen", "veo", "transcribe"
        )
        val names = mutableListOf<String>()
        for (i in 0 until models.length()) {
            val m = models.getJSONObject(i)
            val name = m.getString("name").removePrefix("models/")
            val methods = m.optJSONArray("supportedGenerationMethods") ?: continue
            var ok = false
            for (j in 0 until methods.length()) {
                if (methods.getString(j) == "generateContent") ok = true
            }
            if (!ok) continue
            if (!name.startsWith("gemini")) continue
            if (skip.any { name.contains(it) }) continue
            names.add(name)
        }

        return names.firstOrNull { it.contains("flash-lite") && !it.contains("preview") }
            ?: names.firstOrNull { it.contains("flash") && !it.contains("preview") }
            ?: names.firstOrNull { it.contains("flash") }
            ?: names.firstOrNull()
    }
}
