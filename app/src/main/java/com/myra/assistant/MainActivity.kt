package com.J.A.R.V.I.S.assistant

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var characterAvatar: ImageView
    private lateinit var waveBar: View
    private lateinit var micLiveBadge: TextView
    private lateinit var speakingStatus: TextView
    private lateinit var speakHintText: TextView
    private lateinit var responseCardText: TextView
    private lateinit var quickChatInput: EditText
    private lateinit var quickChatRow: LinearLayout
    private lateinit var setupDrawerSection: LinearLayout

    private var isHindiMode = true
    private var isReacting = false
    private var avatarAnimator: AnimatorSet? = null
    private var waveAnimator: ObjectAnimator? = null

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()

            if (spoken.isNotEmpty()) {
                setCharacterReaction(true)
                responseCardText.text = "आपने कहा: \"$spoken\" — JARVIS रिएक्शन चालू है।"
                dispatchCommand(spoken)
            } else {
                setCharacterReaction(false)
            }
        } else {
            setCharacterReaction(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val outerContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#000000"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 34, 32, 28)
        }

        // Top Bar: JARVIS + हिन्दी pill
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 4
            }
        }

        val titleLeft = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val jarvisTitle = TextView(this).apply {
            text = "JARVIS"
            textSize = 30f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
        }

        val subPrompt = TextView(this).apply {
            text = "What shall we do today?"
            textSize = 14f
            setTextColor(Color.parseColor("#B5B5B5"))
        }

        titleLeft.addView(jarvisTitle)
        titleLeft.addView(subPrompt)

        val hindiBadge = Button(this).apply {
            text = "हिन्दी"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 36f
                setColor(Color.parseColor("#181818"))
                setStroke(2, Color.parseColor("#323232"))
            }
            setOnClickListener {
                isHindiMode = !isHindiMode
                text = if (isHindiMode) "हिन्दी" else "EN"
                speakHintText.text = if (isHindiMode) {
                    "Speak in हिन्दी (Hindi) · tap mic or portrait when done"
                } else {
                    "Speak in English · tap mic or portrait when done"
                }
            }
        }

        topBar.addView(titleLeft)
        topBar.addView(hindiBadge)
        content.addView(topBar)

        // MIC LIVE Pill Badge
        micLiveBadge = TextView(this).apply {
            text = "🎙  MIC LIVE"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#FF7A3D"))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 28f
                setColor(Color.parseColor("#241107"))
                setStroke(2, Color.parseColor("#4D220D"))
            }
            setPadding(22, 8, 22, 8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 8
                bottomMargin = 14
            }
            setOnClickListener {
                setCharacterReaction(!isReacting)
            }
        }
        content.addView(micLiveBadge)

        // Center Girl Character inside Orange Glowing Ring
        val portraitHolder = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(500, 500).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = 18
            }
        }

        val outerRing = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(484, 484, Gravity.CENTER)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#0A0502"))
                setStroke(9, Color.parseColor("#F56A18"))
            }
        }
        portraitHolder.addView(outerRing)

        characterAvatar = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(446, 446, Gravity.CENTER)
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#180C05"))
            }
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            loadCharacterImage(this)
            setOnClickListener {
                setCharacterReaction(!isReacting)
            }
        }
        portraitHolder.addView(characterAvatar)
        content.addView(portraitHolder)

        // Orange Waveform Line
        waveBar = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(380, 12).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = 16
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 12f
                setColor(Color.parseColor("#F56A18"))
            }
        }
        content.addView(waveBar)

        // Speaking / Reaction Status & Hint
        speakingStatus = TextView(this).apply {
            text = "JARVIS is speaking..."
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#F56A18"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 6)
        }
        content.addView(speakingStatus)

        speakHintText = TextView(this).apply {
            text = "Speak in हिन्दी (Hindi) · tap mic or portrait when done"
            textSize = 12f
            setTextColor(Color.parseColor("#A6A6A6"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }
        content.addView(speakHintText)

        // Dark Card Box for Live Response
        val messageCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 28f
                setColor(Color.parseColor("#121212"))
                setStroke(2, Color.parseColor("#242424"))
            }
            setPadding(24, 18, 24, 18)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
        }

        responseCardText = TextView(this).apply {
            text = "JARVIS तैयार है। बात करते समय रिएक्शन चालू रहेगा और बात बंद होने पर ऑफ हो जाएगा।"
            textSize = 14f
            setTextColor(Color.parseColor("#EAEAEA"))
            gravity = Gravity.CENTER
        }
        messageCard.addView(responseCardText)
        content.addView(messageCard)

        // Controls Row: Eye Button + Orange Stop Session Pill + Right Orange Mic Button
        val controlsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 14
            }
        }

        val eyeBtn = Button(this).apply {
            text = "👁"
            textSize = 17f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#161616"))
            }
            layoutParams = LinearLayout.LayoutParams(112, 112).apply {
                rightMargin = 14
            }
            setOnClickListener {
                openOverlaySettings()
            }
        }

        val stopSessionBtn = Button(this).apply {
            text = "■  Stop session"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 46f
                setColor(Color.parseColor("#F56A18"))
            }
            layoutParams = LinearLayout.LayoutParams(0, 112, 1f).apply {
                rightMargin = 14
            }
            setOnClickListener {
                setCharacterReaction(false)
                responseCardText.text = "सेशन बंद कर दिया गया है। रिएक्शन OFF है।"
            }
        }

        val micBtn = Button(this).apply {
            text = "🎙"
            textSize = 17f
            setTextColor(Color.parseColor("#F56A18"))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#241107"))
                setStroke(2, Color.parseColor("#4D220D"))
            }
            layoutParams = LinearLayout.LayoutParams(112, 112)
            setOnClickListener {
                launchSpeechInput()
            }
        }

        controlsRow.addView(eyeBtn)
        controlsRow.addView(stopSessionBtn)
        controlsRow.addView(micBtn)
        content.addView(controlsRow)

        // Optional Quick Text Input Row (toggled by "Type")
        quickChatRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12
            }
        }

        quickChatInput = EditText(this).apply {
            hint = "यहाँ लिखें..."
            setHintTextColor(Color.parseColor("#7A7A7A"))
            setTextColor(Color.WHITE)
            textSize = 14f
            isSingleLine = true
            maxLines = 1
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 26f
                setColor(Color.parseColor("#141414"))
                setStroke(2, Color.parseColor("#2E2E2E"))
            }
            setPadding(22, 14, 22, 14)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = 10
            }
        }

        val sendQuickBtn = Button(this).apply {
            text = "Send"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 26f
                setColor(Color.parseColor("#F56A18"))
            }
            setOnClickListener {
                val msg = quickChatInput.text.toString().trim()
                if (msg.isNotEmpty()) {
                    setCharacterReaction(true)
                    responseCardText.text = "JARVIS: $msg"
                    dispatchCommand(msg)
                    quickChatInput.setText("")
                } else {
                    setCharacterReaction(false)
                }
            }
        }

        quickChatRow.addView(quickChatInput)
        quickChatRow.addView(sendQuickBtn)
        content.addView(quickChatRow)

        // Privacy Note & Action Strip: Test voice · Hey JARVIS · Ready · Type
        val privacyText = TextView(this).apply {
            text = "You control the mic. Screen sharing is always opt-in."
            textSize = 11f
            setTextColor(Color.parseColor("#8A8A8A"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 10)
        }
        content.addView(privacyText)

        val stripRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 18
            }
        }

        val testVoiceTv = TextView(this).apply {
            text = "🎧 Test voice"
            textSize = 12f
            setTextColor(Color.parseColor("#C8C8C8"))
            setPadding(12, 8, 12, 8)
            setOnClickListener {
                setCharacterReaction(true)
                responseCardText.text = "हाँ जी बॉस, JARVIS की आवाज़ और रिएक्शन चालू है।"
            }
        }

        val wakeReadyTv = TextView(this).apply {
            text = "·  Hey JARVIS · Ready  ·"
            textSize = 12f
            setTextColor(Color.parseColor("#9A9A9A"))
            setPadding(8, 8, 8, 8)
            setOnClickListener {
                setCharacterReaction(!isReacting)
            }
        }

        val typeTv = TextView(this).apply {
            text = "⌨ Type"
            textSize = 12f
            setTextColor(Color.parseColor("#C8C8C8"))
            setPadding(12, 8, 12, 8)
            setOnClickListener {
                quickChatRow.visibility =
                    if (quickChatRow.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }
        }

        stripRow.addView(testVoiceTv)
        stripRow.addView(wakeReadyTv)
        stripRow.addView(typeTv)
        content.addView(stripRow)

        // Setup & Gemini Key Section
        setupDrawerSection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val setupScroll = HorizontalScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12
            }
        }

        val setupRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        setupRow.addView(makeActionChip("1. Permissions") { askPermissions() })
        setupRow.addView(makeActionChip("2. Overlay") { openOverlaySettings() })
        setupRow.addView(makeActionChip("3. Accessibility") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        setupRow.addView(makeActionChip("5. Start Service") {
            setCharacterReaction(true)
            startJarvisService()
        })
        setupScroll.addView(setupRow)
        setupDrawerSection.addView(setupScroll)

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
                setColor(Color.parseColor("#121212"))
                setStroke(2, Color.parseColor("#2A2A2A"))
            }
            setPadding(22, 14, 22, 14)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 10
            }
        }
        setupDrawerSection.addView(keyInput)

        val saveKeyBtn = Button(this).apply {
            text = "4. Key सेव करें"
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 20f
                setColor(Color.parseColor("#181818"))
                setStroke(2, Color.parseColor("#F56A18"))
            }
            setOnClickListener {
                val key = keyInput.text.toString().trim()
                if (key.isEmpty()) {
                    Toast.makeText(this@MainActivity, "पहले key डालें", Toast.LENGTH_SHORT).show()
                } else {
                    getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
      
