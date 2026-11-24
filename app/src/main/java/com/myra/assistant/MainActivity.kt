package com.myra.assistant

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(48, 96, 48, 48)
        }

        root.addView(TextView(this).apply {
            text = "Jarvis"
            textSize = 30f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 48)
        })

        root.addView(makeButton("1. परमिशन दो") { askPermissions() })
        root.addView(makeButton("2. ओवरले चालू करो") { openOverlaySettings() })
        root.addView(makeButton("3. एक्सेसिबिलिटी चालू करो") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })

        val keyBox = EditText(this).apply {
            hint = "Gemini key यहाँ डालो"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
        }
        root.addView(keyBox)

        root.addView(makeButton("4. Key सेव करो") {
            val key = keyBox.text.toString().trim()
            if (key.isEmpty()) {
                Toast.makeText(this, "पहले key डालो", Toast.LENGTH_SHORT).show()
            } else {
                getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
                    .edit().putString("gemini_key", key).apply()
                keyBox.setText("")
                Toast.makeText(this, "Key सेव हो गई", Toast.LENGTH_SHORT).show()
            }
        })

        root.addView(makeButton("5. Jarvis शुरू करो") { startJarvis() })

        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun makeButton(label: String, onTap: () -> Unit): Button {
        return Button(this).apply {
            text = label
            setOnClickListener { onTap() }
        }
    }

    private fun askPermissions() {
        val list = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS,
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

    private fun startJarvis() {
        ContextCompat.startForegroundService(this, Intent(this, MyraService::class.java))
    }
}
