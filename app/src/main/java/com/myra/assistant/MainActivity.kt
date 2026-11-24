package com.Jarvis.assistant

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
import android.view.inputmethod.InputMethodManager
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

    private lateinit var commandHandler: CommandHandler
    private lateinit var characterAvatar: ImageView
    private lateinit var statusText: TextView
    private lateinit var chatBox: EditText
    private lateinit var micButton: Button

    private var isActiveReaction = false
    private var reactionAnimator: AnimatorSet? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        commandHandler = CommandHandler(this)

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#050811"))
            isFillViewport = true
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 36, 28, 36)
        }

        // Top Header: JARVIS
        val titleView = TextView(this).apply {
            text = "JARVIS"
            textSize = 28f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#38F2FF"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 12)
        }
        root.addView(titleView)

        // Character Reaction Status
        statusText = TextView(this).apply {
            text = "JARVIS रिएक्शन: OFF (शांत)"
            textSize = 13f
            setTextColor(Color.parseColor("#9FB3C8"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 18)
        }
        root.addView(statusText)

        // Middle Stage: Left Tabs (MEMORY, SKILLS, CHARACTER, SETTING) + Center Girl Character
        val middleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 24
            }
        }

        val leftTabsColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.42f)
        }

        val tabs = listOf(
            Triple("MEMORY", "#00E5FF", "MEMORY मोड सक्रिय"),
            Triple("SKILLS", "#00E676", "SKILLS मोड सक्रिय"),
            Triple("CHARACTER", "#FF2A55", "CHARACTER रिएक्शन मोड"),
            Triple("SETTING", "#E0E8FF", "SETTING मोड सक्रिय")
        )

        for ((label, hexColor, msg) in tabs) {
            val tabBtn = Button(this).apply {
                text = label
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 18f
                    setColor(Color.parseColor("#0D1526"))
                    setStroke(3, Color.parseColor(hexColor))
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 14
                }
                setOnClickListener {
                    statusText.text = "JARVIS: $msg"
                    if (label == "CHARACTER") {
                        toggleCharacterReaction(!isActiveReaction)
                    } else if (label == "SETTING") {
                        openOverlaySettings()
                    }
                }
            }
            leftTabsColumn.addView(tabBtn)
        }

        // Center Character Container with Glowing Ring
        val avatarContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 420, 0.58f).apply {
                leftMargin = 14
            }
        }

        val glowRing = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(370, 370, Gravity.CENTER)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#0A1324"))
                setStroke(8, Color.parseColor("#00E5FF"))
            }
        }
        avatarContainer.addView(glowRing)

        characterAvatar = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(340, 340, Gravity.CENTER)
            scaleType = ImageView.ScaleType.CENTER_CROP
            val resId = resources.getIdentifier("jarvis_character", "drawable", packageName)
            if (resId != 0) {
                setImageResource(resId)
            }
            setOnClickListener {
                toggleCharacterReaction(!isActiveReaction)
            }
        }
        avatarContainer.addView(characterAvatar)

        middleRow.addView(leftTabsColumn)
        middleRow.addView(avatarContainer)
        root.addView(middleRow)

        // Bottom Bar: Single Main Mic + Chat Box + Keyboard Icon + Send Icon
        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 24
            }
        }

        micButton = Button(this).apply {
            text = "MIC"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#00E5FF"))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#102238"))
                setStroke(4, Color.parseColor("#00E5FF"))
            }
            layoutParams = LinearLayout.LayoutParams(120, 120).apply {
                rightMargin = 10
            }
            setOnClickListener {
                val nextState = !isActiveReaction
                toggleCharacterReaction(nextState)
                if (nextState) {
                    val spokenCmd = chatBox.text.toString().trim().ifEmpty { "jarvis status" }
                    statusText.text = "JARVIS बात कर रही है: $spokenCmd"
                    commandHandler.handle(spokenCmd)
                } else {
                    statusText.text = "JARVIS रिएक्शन: OFF"
                }
            }
        }

        chatBox = EditText(this).apply {
            hint = "यहाँ लिखें..."
            setHintTextColor(Color.parseColor("#7D8DA6"))
            setTextColor(Color.WHITE)
            textSize = 14f
            singleLine = true
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 32f
                setColor(Color.parseColor("#101829"))
                setStroke(3, Color.parseColor("#2A3F63"))
            }
            setPadding(24, 18, 24, 18)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = 8
            }
        }

        val keyboardIconBtn = Button(this).apply {
            text = "KB"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#131D33"))
                setStroke(3, Color.parseColor("#3A4F75"))
            }
            layoutParams = LinearLayout.LayoutParams(105, 105).apply {
                rightMargin = 8
            }
            setOnClickListener {
                chatBox.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(chatBox, InputMethodManager.SHOW_IMPLICIT)
                statusText.text = "कीबोर्ड खोला गया"
            }
        }

        val sendIconBtn = Button(this).apply {
            text = "SEND"
            textSize = 10f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#FF2A55"))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#24101A"))
                setStroke(3, Color.parseColor("#FF2A55"))
            }
            layoutParams = LinearLayout.LayoutParams(105, 105)
            setOnClickListener {
                val cmd = chatBox.text.toString().trim()
                if (cmd.isNotEmpty()) {
                    toggleCharacterReaction(true)
                    statusText.text = "JARVIS रिएक्शन: $cmd"
                    commandHandler.handle(cmd)
                    chatBox.setText("")
                } else {
                    toggleCharacterReaction(false)
                    statusText.text = "JARVIS रिएक्शन: OFF"
                }
            }
        }

        bottomBar.addView(micButton)
        bottomBar.addView(chatBox)
        bottomBar.addView(keyboardIconBtn)
        bottomBar.addView(sendIconBtn)
        root.addView(bottomBar)

        // Compact Control & Gemini Key Row (ताकि सेटअप और की सेव करने वाला काम भी चलता रहे)
        val setupRow = HorizontalScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 14
            }
        }

        val setupButtonsBox = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        setupButtonsBox.addView(makeSmallActionButton("1. परमिशन") { askPermissions() })
        setupButtonsBox.addView(makeSmallActionButton("2. ओवरले") { openOverlaySettings() })
        setupButtonsBox.addView(makeSmallActionButton("3. एक्सेसिबिलिटी") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        setupButtonsBox.addView(makeSmallActionButton("5. JARVIS शुरू") {
            toggleCharacterReaction(true)
            startJarvis()
        })

        setupRow.addView(setupButtonsBox)
        root.addView(setupRow)

        val keyBox = EditText(this).apply {
            hint = "Gemini key यहाँ डालें"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            isSingleLine = true
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 20f
                setColor(Color.parseColor("#101829"))
                setStroke(2, Color.parseColor("#2A3F63"))
            }
            setPadding(24, 16, 24, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 10
            }
        }
        root.addView(keyBox)

        val saveKeyBtn = Button(this).apply {
            text = "4. Key सेव करें"
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 20f
                setColor(Color.parseColor("#16243B"))
                setStroke(2, Color.parseColor("#00E5FF"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                val key = keyBox.text.toString().trim()
                if (key.isEmpty()) {
                    Toast.makeText(this@MainActivity, "पहले key डालें", Toast.LENGTH_SHORT).show()
                } else {
                    getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .putString("gemini_key", key)
                        .apply()
                    keyBox.setText("")
                    Toast.makeText(this@MainActivity, "Key सेव हो गई", Toast.LENGTH_SHORT).show()
                }
            }
        }
        root.addView(saveKeyBtn)

        scrollView.addView(root)
        setContentView(scrollView)

        // शुरू में रिएक्शन OFF रखें
        toggleCharacterReaction(false)
    }

    private fun toggleCharacterReaction(active: Boolean) {
        isActiveReaction = active
        reactionAnimator?.cancel()

        if (active) {
            statusText.text = "JARVIS: बात करते हुए रिएक्शन ACTIVE"
            statusText.setTextColor(Color.parseColor("#00FFA3"))

            val scaleX = ObjectAnimator.ofFloat(characterAvatar, View.SCALE_X, 0.95f, 1.08f).apply {
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                duration = 550
            }
            val scaleY = ObjectAnimator.ofFloat(characterAvatar, View.SCALE_Y, 0.95f, 1.08f).apply {
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                duration = 550
            }
            val tilt = ObjectAnimator.ofFloat(characterAvatar, View.ROTATION, -4f, 4f).apply {
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                duration = 700
            }

            reactionAnimator = AnimatorSet().apply {
                interpolator = AccelerateDecelerateInterpolator()
                playTogether(scaleX, scaleY, tilt)
                start()
            }
        } else {
            statusText.text = "JARVIS रिएक्शन: OFF (बात बंद / शांत)"
            statusText.setTextColor(Color.parseColor("#9FB3C8"))
            characterAvatar.scaleX = 1f
            characterAvatar.scaleY = 1f
            characterAvatar.rotation = 0f
        }
    }

    private fun makeSmallActionButton(label: String, onTap: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 11f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 18f
                setColor(Color.parseColor("#11192B"))
                setStroke(2, Color.parseColor("#354B73"))
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

    private fun startJarvis() {
        val intent = Intent(this, MyraService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "JARVIS सेवा शुरू की गई", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        reactionAnimator?.cancel()
        super.onDestroy()
    }
}
