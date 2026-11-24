package com.myra.assistant

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    private lateinit var statusText: TextView
    private lateinit var hintText: TextView
    private lateinit var weatherTemp: TextView
    private lateinit var weatherInfo: TextView
    private lateinit var avatarFrame: FrameLayout
    private lateinit var ring: View
    private var ringAnim: ObjectAnimator? = null

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun rounded(fill: Int, stroke: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(1), stroke)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dark = Color.parseColor("#2B1B17")

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(16), dp(40), dp(16), dp(24))
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    Color.parseColor("#F7B9A8"),
                    Color.parseColor("#FBE0D8"),
                    Color.parseColor("#F7C4B6")
                )
            )
        }

        // ऊपर: नाम, नमस्ते और सेटिंग
        val top = FrameLayout(this)
        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        titleCol.addView(TextView(this).apply {
            text = "Jarvis"
            textSize = 34f
            setTextColor(dark)
            setTypeface(typeface, android.graphics.Typeface.BOLD_ITALIC)
            gravity = Gravity.CENTER
        })
        titleCol.addView(TextView(this).apply {
            text = greeting()
            textSize = 14f
            setTextColor(dark)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
        })
        top.addView(titleCol, FrameLayout.LayoutParams(MATCH, WRAP))
        top.addView(TextView(this).apply {
            text = "⚙️"
            textSize = 20f
            gravity = Gravity.CENTER
            background = rounded(Color.parseColor("#66FFFFFF"), Color.parseColor("#55000000"), 14)
            setOnClickListener { openSettingsDialog() }
        }, FrameLayout.LayoutParams(dp(44), dp(44), Gravity.END))
        root.addView(top, LinearLayout.LayoutParams(MATCH, WRAP))

        // बीच का बड़ा गोला
        val outer = dp(260)
        val inner = dp(232)
        avatarFrame = FrameLayout(this)
        ring = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.TRANSPARENT)
                setStroke(dp(3), Color.parseColor("#C62828"))
            }
        }
        avatarFrame.addView(ring, FrameLayout.LayoutParams(outer, outer, Gravity.CENTER))
        val image = ImageView(this).apply {
            setImageResource(R.drawable.avatar)
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
        }
        avatarFrame.addView(image, FrameLayout.LayoutParams(inner, inner, Gravity.CENTER))
        avatarFrame.setOnClickListener { toggleJarvis() }
        root.addView(avatarFrame, LinearLayout.LayoutParams(outer, outer).apply {
            topMargin = dp(20)
        })

        // माइक की स्थिति
        statusText = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.parseColor("#1B5E20"))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(10), dp(24), dp(10))
            background = rounded(Color.parseColor("#88FFFFFF"), Color.parseColor("#55000000"), 22)
        }
        root.addView(statusText, LinearLayout.LayoutParams(WRAP, WRAP).apply {
            topMargin = dp(16)
        })

        hintText = TextView(this).apply {
            textSize = 11f
            setTextColor(dark)
            gravity = Gravity.CENTER
        }
        root.addView(hintText, LinearLayout.LayoutParams(WRAP, WRAP).apply {
            topMargin = dp(6)
        })

        // मौसम का कार्ड
        val weatherCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(14), dp(20), dp(14))
            background = rounded(Color.parseColor("#88FFFFFF"), Color.parseColor("#55000000"), 22)
            setOnClickListener { loadWeather() }
        }
        weatherTemp = TextView(this).apply {
            text = "--°C"
            textSize = 30f
            setTextColor(dark)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        weatherInfo = TextView(this).apply {
            text = "मौसम देखने के लिए दबाओ"
            textSize = 13f
            setTextColor(dark)
            setPadding(dp(16), 0, 0, 0)
        }
        weatherCard.addView(weatherTemp)
        weatherCard.addView(weatherInfo)
        root.addView(weatherCard, LinearLayout.LayoutParams(MATCH, WRAP).apply {
            topMargin = dp(18)
        })

        // तीन छोटे कार्ड
        val cards = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        cards.addView(smallCard("🔐", "परमिशन") { askPermissions() })
        cards.addView(smallCard("🫧", "ओवरले") { openOverlaySettings() })
        cards.addView(smallCard("♿", "एक्सेसिबिलिटी") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        root.addView(cards, LinearLayout.LayoutParams(MATCH, WRAP).apply {
            topMargin = dp(14)
        })

        // नीचे की पट्टी
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = rounded(Color.parseColor("#99FFFFFF"), Color.parseColor("#55000000"), 32)
        }
        bar.addView(TextView(this).apply {
            text = "🏠\nHome"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(dark)
        }, LinearLayout.LayoutParams(0, WRAP, 1f))
        bar.addView(TextView(this).apply {
            text = "🎤"
            textSize = 28f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#D50000"))
            }
            setOnClickListener { toggleJarvis() }
        }, LinearLayout.LayoutParams(dp(68), dp(68)))
        bar.addView(TextView(this).apply {
            text = "⚙️\nSettings"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(dark)
            setOnClickListener { openSettingsDialog() }
        }, LinearLayout.LayoutParams(0, WRAP, 1f))
        root.addView(bar, LinearLayout.LayoutParams(MATCH, WRAP).apply {
            topMargin = dp(18)
        })

        setContentView(ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        })

        startFloating()
    }

    override fun onResume() {
        super.onResume()
        refreshState()
        loadWeather()
    }

    private fun greeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val word = when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
        return "$word, Sagar"
    }

    private fun smallCard(emoji: String, label: String, onTap: () -> Unit): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(12), dp(6), dp(12))
            background = rounded(Color.parseColor("#88FFFFFF"), Color.parseColor("#55000000"), 18)
            setOnClickListener { onTap() }
        }
        card.addView(TextView(this).apply {
            text = emoji
            textSize = 22f
            gravity = Gravity.CENTER
        })
        card.addView(TextView(this).apply {
            text = label
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#2B1B17"))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        card.layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f).apply {
            setMargins(dp(4), 0, dp(4), 0)
        }
        return card
    }

    // इमेज का तैरना और साँस जैसा धड़कना
    private fun startFloating() {
        ObjectAnimator.ofPropertyValuesHolder(
            avatarFrame,
            PropertyValuesHolder.ofFloat("translationY", -dp(8).toFloat(), dp(8).toFloat()),
            PropertyValuesHolder.ofFloat("scaleX", 1f, 1.04f),
            PropertyValuesHolder.ofFloat("scaleY", 1f, 1.04f),
            PropertyValuesHolder.ofFloat("rotation", -1.5f, 1.5f)
        ).apply {
            duration = 2400
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            start()
        }
    }

    @Suppress("DEPRECATION")
    private fun isServiceRunning(): Boolean {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return am.getRunningServices(100).any {
            it.service.className == MyraService::class.java.name
        }
    }

    private fun refreshState() {
        val running = isServiceRunning()
        if (running) {
            statusText.text = "● MIC ON • LISTENING..."
            hintText.text = "\"Jarvis\" बोलो • गोले पर दबाकर बंद करो"
            if (ringAnim == null) {
                ringAnim = ObjectAnimator.ofPropertyValuesHolder(
                    ring,
                    PropertyValuesHolder.ofFloat("scaleX", 1f, 1.1f),
                    PropertyValuesHolder.ofFloat("scaleY", 1f, 1.1f),
                    PropertyValuesHolder.ofFloat("alpha", 1f, 0.35f)
                ).apply {
                    duration = 1100
                    repeatCount = ValueAnimator.INFINITE
                    repeatMode = ValueAnimator.REVERSE
                    start()
                }
            }
        } else {
            statusText.text = "● MIC OFF"
            hintText.text = "गोले पर दबाकर Jarvis चालू करो"
            ringAnim?.cancel()
            ringAnim = null
            ring.scaleX = 1f
            ring.scaleY = 1f
            ring.alpha = 1f
        }
    }

    private fun toggleJarvis() {
        if (isServiceRunning()) {
            stopService(Intent(this, MyraService::class.java))
            Toast.makeText(this, "Jarvis बंद हो गया", Toast.LENGTH_SHORT).show()
        } else {
            val micGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (!micGranted) {
                Toast.makeText(this, "पहले परमिशन वाले कार्ड से माइक की इजाज़त दो", Toast.LENGTH_LONG).show()
                return
            }
            try {
                ContextCompat.startForegroundService(this, Intent(this, MyraService::class.java))
                Toast.makeText(this, "Jarvis शुरू हो गया", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    "दिक्कत: " + e.javaClass.simpleName + " " + (e.message ?: ""),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        handler.postDelayed({ refreshState() }, 600)
    }

    private fun openSettingsDialog() {
        val box = EditText(this).apply {
            hint = "Gemini key यहाँ डालो"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
        }
        AlertDialog.Builder(this)
            .setTitle("Gemini key")
            .setView(box)
            .setPositiveButton("सेव करो") { _, _ ->
                val key = box.text.toString().trim()
                if (key.isEmpty()) {
                    Toast.makeText(this, "पहले key डालो", Toast.LENGTH_SHORT).show()
                } else {
                    getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
                        .edit().putString("gemini_key", key).apply()
                    Toast.makeText(this, "Key सेव हो गई", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("रद्द", null)
            .show()
    }

    private fun askPermissions() {
        val list = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= 33) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        ActivityCompat.requestPermissions(this, list.toTypedArray(), 1)
    }

    private fun openOverlaySettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun describe(code: Int): String {
        return when (code) {
            0 -> "साफ़ आसमान"
            1, 2, 3 -> "बादल छाए हैं"
            45, 48 -> "कोहरा"
            in 51..57 -> "बूंदाबांदी"
            in 61..67 -> "बारिश"
            in 71..77 -> "बर्फ़बारी"
            in 80..82 -> "बौछारें"
            in 95..99 -> "आँधी-तूफान"
            else -> "मिला-जुला मौसम"
        }
    }

    private fun loadWeather() {
        val fine = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) {
            weatherInfo.text = "मौसम के लिए लोकेशन की इजाज़त दो"
            return
        }
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
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
        val here = best
        if (here == null) {
            weatherInfo.text = "लोकेशन नहीं मिली, फोन की लोकेशन चालू करो"
            return
        }
        thread {
            var temp = "--°C"
            var info: String
            try {
                val url = URL(
                    "https://api.open-meteo.com/v1/forecast?latitude=${here.latitude}" +
                        "&longitude=${here.longitude}" +
                        "&current=temperature_2m,weather_code,relative_humidity_2m&timezone=auto"
                )
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                val body = conn.inputStream.bufferedReader().readText()
                val current = JSONObject(body).getJSONObject("current")
                temp = current.getDouble("temperature_2m").toInt().toString() + "°C"
                val humidity = current.getInt("relative_humidity_2m")
                info = describe(current.getInt("weather_code")) + " • नमी $humidity%"
            } catch (e: Exception) {
                info = "मौसम नहीं मिला, इंटरनेट देखो"
            }
            runOnUiThread {
                weatherTemp.text = temp
                weatherInfo.text = info
            }
        }
    }
}
