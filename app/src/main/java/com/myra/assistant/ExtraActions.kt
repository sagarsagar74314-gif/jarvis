package com.myra.assistant

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class ExtraActions(
    private val context: Context,
    private val say: (String) -> Unit
) {

    private val main = Handler(Looper.getMainLooper())

    fun addGoogleAccount() {
        val intent = Intent(Settings.ACTION_ADD_ACCOUNT).apply {
            putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
            say("गूगल अकाउंट की स्क्रीन खोल रहा हूँ")
        } catch (e: Exception) {
            say("यह स्क्रीन नहीं खुल पाई")
        }
    }

    private fun lastLocation(): Location? {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return null

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var best: Location? = null
        for (provider in lm.getProviders(true)) {
            val loc = try {
                lm.getLastKnownLocation(provider)
            } catch (e: SecurityException) {
                null
            } ?: continue
            val current = best
            if (current == null || loc.time > current.time) best = loc
        }
        return best
    }

    private fun describe(code: Int): String {
        return when (code) {
            0 -> "आसमान साफ है"
            1, 2, 3 -> "बादल छाए हैं"
            45, 48 -> "कोहरा है"
            in 51..57 -> "बूंदाबांदी हो रही है"
            in 61..67 -> "बारिश हो रही है"
            in 71..77 -> "बर्फ गिर रही है"
            in 80..82 -> "बौछारें पड़ रही हैं"
            in 95..99 -> "आँधी-तूफान है"
            else -> "मौसम मिला-जुला है"
        }
    }

    fun weather() {
        val loc = lastLocation()
        if (loc == null) {
            say("पहले लोकेशन की इजाज़त दो और फोन की लोकेशन चालू करो")
            return
        }
        say("मौसम देख रहा हूँ")
        thread {
            val answer = try {
                val url = URL(
                    "https://api.open-meteo.com/v1/forecast?latitude=${loc.latitude}" +
                        "&longitude=${loc.longitude}" +
                        "&current=temperature_2m,weather_code&timezone=auto"
                )
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                val body = conn.inputStream.bufferedReader().readText()
                val current = JSONObject(body).getJSONObject("current")
                val temp = current.getDouble("temperature_2m").toInt()
                val code = current.getInt("weather_code")
                "अभी तापमान $temp डिग्री है और ${describe(code)}"
            } catch (e: Exception) {
                "मौसम नहीं मिल पाया, इंटरनेट देखो"
            }
            main.post { say(answer) }
        }
    }
}
