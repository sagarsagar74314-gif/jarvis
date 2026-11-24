package com.myra.assistant

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    private lateinit var characterAvatar: ImageView
    private lateinit var glowRingView: View
    private lateinit var waveBarView: View
    private lateinit var speakingStatusText: TextView
    private lateinit var subHintText: TextView
    private lateinit var stopSessionBtn: Button
    private lateinit var commandInput: EditText

    private var isSpeakingOrReacting = false
    private var characterAnimator: AnimatorSet? = null
    private var waveAnimator: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#000000"))
            isFillViewport = true
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 44, 36, 44)
        }

        // Top Greeting Header (30029.jpg style)
        val greetingTitle = TextView(this).apply {
            text = "Hello, JARVIS."
            textSize = 30f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
        }
        root.addView(greetingTitle)

        val subtitleView = TextView(this).apply {
            text = "What shall we do today?"
            textSize = 15f
            setTextColor(Color.parseColor("#B8B8B8"))
            setPadding(0, 6, 0, 28)
        }
        root.addView(subtitleView)

        // Center Portrait inside Orange Glowing Ring
        val portraitFrame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(540, 540).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = 26
            }
        }

        glowRingView = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(520, 520, Gravity.CENTER)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#080402"))
                setStroke(10, Color.parseColor("#FF5A1F"))
            }
        }
        portraitFrame.addView(glowRingView)

        characterAvatar = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(476, 476, Gravity.CENTER)
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#140A05"))
            }
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            val resId = resources.getIdentifier("jarvis_character", "drawable", packageName)
            if (resId != 0) {
                setImageResource(resId)
            }
            setOnClickListener {
                toggleSpeakingReaction(!isSpeakingOrReacting)
            }
        }
        portraitFrame.addView(characterAvatar)
        root.addView(portraitFrame)

        // Orange Sound Wave Bar below Portrait
        waveBarView = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(420, 14).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = 22
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 12f
                setColor(Color.parseColor("#FF5A1F"))
            }
        }
        root.addView(waveBarView)

        // Speaking Status & Tap Hint
        speakingStatusText = TextView(this).apply {
            text = "JARVIS is idle..."
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#FF5A1F"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 8)
        }
        root.addView(speakingStatusText)

        subHintText = TextView(this).apply {
            text = "Speak to interrupt · or tap the portrait"
            textSize = 13f
            setTextColor(Color.parseColor("#A6A6A6"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 28)
        }
        root.addView(subHintText)

        // Bottom Controls Row: Left Eye/Mode Button + Orange Stop Session Button + Right Mic Button
        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 18
            }
        }

        val leftModeBtn = Button(this).apply {
            text = "👁"
            textSize = 18f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#141414"))
            }
            layoutParams = LinearLayout.LayoutParams(116, 116).apply {
                rightMargin = 16
            }
            setOnClickListener {
                openOverlaySettings()
            }
        }

        stopSessionBtn = Button(this).apply {
            text = "■  Stop session"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 48f
                setColor(Color.parseColor("#FF5A1F"))
            }
            layoutParams = LinearLayout.LayoutParams(0, 116, 1f).apply {
                rightMargin = 16
            }
            setOnClickListener {
                toggleSpeakingReaction(false)
                speakingStatusText.text = "Session stopped"
            }
        }

        val rightMicBtn = Button(this).apply {
            text = "🎙"
            textSize = 18f
            setTextColor(Color.parseColor("#FF5A1F"))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#1A0D07"))
            }
            layoutParams = LinearLayout.LayoutParams(116, 116)
            setOnClickListener {
                val next = !isSpeakingOrReacting
                toggleSpeakingReaction(next)
                if (next) {
                    val cmd = commandInput.text.toString().trim().ifEmpty { "jarvis hello" }
                    dispatchCommand(cmd)
                }
            }
        }

        actionRow.addView(leftModeBtn)
        actionRow.addView(stopSessionBtn)
        actionRow.addView(rightMicBtn)
        root.addView(actionRow)

        // Optional Quick Text Command Input
        commandInput = EditText(this).apply {
            hint = "Type a command or message..."
            setHintTextColor(Color.parseColor("#777777"))
            setTextColor(Color.WHITE)
            textSize = 14f
            isSingleLine = true
            maxLines = 1
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 28f
                setColor(Color.parseColor("#111111"))
                setStroke(2, Color.parseColor("#2E2E2E"))
            }
            setPadding(24, 16, 24, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12
            }
        }
        root.addView(commandInput)

        val sendBtn = Button(this).apply {
            text = "Send & React"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 26f
                setColor(Color.parseColor("#1F110B"))
                setStroke(2, Color.parseColor("#FF5A1F"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
            setOnClickListener {
                val cmd = commandInput.text.toString().trim()
                if (cmd.isNotEmpty()) {
                    toggleSpeakingReaction(true)
                    speakingStatusText.text = "JARVIS is speaking..."
                    dispatchCommand(cmd)
                    commandInput.setText("")
                } else {
                    toggleSpeakingReaction(false)
                }
            }
        }
        root.addView(sendBtn)

        // Footer Info Line (30029.jpg style)
        val privacyNote = TextView(this).apply {
            text = "You control the mic. Screen sharing is always opt-in."
            textSize = 12f
            setTextColor(Color.parseColor("#8E8E8E"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 12)
        }
        root.addView(privacyNote)

        val speakerWakeNote = TextView(this).apply {
            text = "🎧 Phone speaker · Wake word off"
            textSize = 12f
            setTextColor(Color.parseColor("#B5B5B5"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 28)
        }
        root.addView(speakerWakeNote)

        // Setup & Gemini Key Section (scroll down to configure)
        val setupScroll = HorizontalScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 14
            }
        }

        val setupRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        setupRow.addView(makeSetupButton("1. Permissions") { askPermissions() })
        setupRow.addView(makeSetupButton("2. Overlay") { openOverlaySettings() })
        setupRow.addView(makeSetupButton("3. Accessibility") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        setupRow.addView(makeSetupButton("5. Start Service") {
            toggleSpeakingReaction(true)
            startJarvisService()
        })
        setupScroll.addView(setupRow)
        root.addView(setupScroll)

        val keyInput = EditText(this).apply {
            hint = "Gemini key यहाँ डालें"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            isSingleLine = true
            maxLines = 1
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 20f
                setColor(Color.parseColor("#111111"))
                setStroke(2, Color.parseColor("#2E2E2E"))
            }
            setPadding(24, 16, 24, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 10
            }
        }
        root.addView(keyInput)

        val saveKeyBtn = Button(this).apply {
            text = "4. Key सेव करें"
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 20f
                setColor(Color.parseColor("#171717"))
                setStroke(2, Color.parseColor("#FF5A1F"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                val key = keyInput.text.toString().trim()
                if (key.isEmpty()) {
                    Toast.makeText(this@MainActivity, "पहले key डालें", Toast.LENGTH_SHORT).show()
                } else {
                    getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .putString("gemini_key", key)
                        .apply()
                    keyInput.setText("")
                    Toast.makeText(this@MainActivity, "Key सेव हो गई", Toast.LENGTH_SHORT).show()
                }
            }
        }
        root.addView(saveKeyBtn)

        scrollView.addView(root)
        setContentView(scrollView)

        toggleSpeakingReaction(false)
    }

    private fun toggleSpeakingReaction(reacting: Boolean) {
        isSpeakingOrReacting = reacting
        characterAnimator?.cancel()
        waveAnimator?.cancel()

        if (reacting) {
            speakingStatusText.text = "JARVIS is speaking..."
            speakingStatusText.setTextColor(Color.parseColor("#FF5A1F"))

            val scaleX = ObjectAnimator.ofFloat(characterAvatar, View.SCALE_X, 0.96f, 1.07f).apply {
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                duration = 520
            }
            val scaleY = ObjectAnimator.ofFloat(characterAvatar, View.SCALE_Y, 0.96f, 1.07f).apply {
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                duration = 520
            }
            val tilt = ObjectAnimator.ofFloat(characterAvatar, View.ROTATION, -3.5f, 3.5f).apply {
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                duration = 680
            }

            characterAnimator = AnimatorSet().apply {
                interpolator = AccelerateDecelerateInterpolator()
                playTogether(scaleX, scaleY, tilt)
                start()
            }

            waveAnimator = ObjectAnimator.ofFloat(waveBarView, View.SCALE_X, 0.55f, 1.15f).apply {
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                duration = 320
                start()
            }
        } else {
            speakingStatusText.text = "JARVIS is idle..."
            speakingStatusText.setTextColor(Color.parseColor("#A86C52"))
            characterAvatar.scaleX = 1f
            characterAvatar.scaleY = 1f
            characterAvatar.rotation = 0f
            waveBarView.scaleX = 0.65f
        }
    }

    private fun dispatchCommand(cmd: String) {
        runCatching {
            val clazz = Class.forName("com.myra.assistant.CommandHandler")
            val ctor = clazz.getConstructor(Context::class.java)
            val instance = ctor.newInstance(this)
            val method = clazz.methods.firstOrNull {
                it.parameterCount == 1 && it.parameterTypes[0] == String::class.java
            }
            method?.invoke(instance, cmd)
        }.onFailure {
            Toast.makeText(this, "कमांड: $cmd", Toast.LENGTH_SHORT).show()
        }
    }

    private fun makeSetupButton(label: String, onTap: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 11f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 18f
                setColor(Color.parseColor("#141414"))
                setStroke(2, Color.parseColor("#333333"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                rightMargin = 10
            }
            setOnClickListener { onTap() }
        }
    }

    private fun askPermissions() {
        val perms = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        ActivityCompat.requestPermissions(this, perms.toTypedArray(), 101)
    }

    private fun openOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } else {
            Toast.makeText(this, "Overlay अनुमति पहले से चालू है", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startJarvisService() {
        runCatching {
            val serviceClass = Class.forName("com.myra.assistant.MyraService")
            val intent = Intent(this, serviceClass)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Toast.makeText(this, "JARVIS सेवा शुरू की गई", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, "JARVIS सेवा शुरू करने में समस्या", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        characterAnimator?.cancel()
        waveAnimator?.cancel()
        super.onDestroy()
    }
}

