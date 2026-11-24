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

    private lateinit var avatar: ImageView
    private lateinit var wave: View
    private lateinit var badge: TextView
    private lateinit var statusTv: TextView
    private lateinit var hintTv: TextView
    private lateinit var cardTv: TextView
    private lateinit var chatEt: EditText
    private lateinit var chatRow: LinearLayout
    private lateinit var setupBox: LinearLayout

    private var hindi = true
    private var reacting = false
    private var animSet: AnimatorSet? = null
    private var waveAnim: ObjectAnimator? = null

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        val txt = if (res.resultCode == Activity.RESULT_OK) {
            res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.trim().orEmpty()
        } else ""
        if (txt.isNotEmpty()) {
            setReacting(true)
            cardTv.text = "आपने कहा: \"$txt\" — JARVIS रिएक्शन चालू है।"
            runCmd(txt)
        } else {
            setReacting(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootWrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#000000"))
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 34, 32, 24)
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleCol.addView(TextView(this).apply {
            text = "JARVIS"
            textSize = 30f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
        })
        titleCol.addView(TextView(this).apply {
            text = "What shall we do today?"
            textSize = 14f
            setTextColor(Color.parseColor("#B5B5B5"))
        })
        val langBtn = Button(this).apply {
            text = "हिन्दी"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = pill("#181818", "#323232", 36f, 2)
            setOnClickListener {
                hindi = !hindi
                text = if (hindi) "हिन्दी" else "EN"
                hintTv.text = if (hindi) "Speak in हिन्दी · tap mic or portrait when done"
                else "Speak in English · tap mic or portrait when done"
            }
        }
        top.addView(titleCol)
        top.addView(langBtn)
        col.addView(top)

        badge = TextView(this).apply {
            text = "MIC LIVE"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#FF7A3D"))
            background = pill("#241107", "#4D220D", 28f, 2)
            setPadding(22, 8, 22, 8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 10; bottomMargin = 14 }
            setOnClickListener { setReacting(!reacting) }
        }
        col.addView(badge)

        val frame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(490, 490).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = 16
            }
        }
        frame.addView(View(this).apply {
            layoutParams = FrameLayout.LayoutParams(476, 476, Gravity.CENTER)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#0A0502"))
                setStroke(9, Color.parseColor("#F56A18"))
            }
        })
        avatar = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(438, 438, Gravity.CENTER)
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#180C05"))
            }
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(v: View, out: Outline) = out.setOval(0, 0, v.width, v.height)
            }
            loadAvatar(this)
            setOnClickListener { setReacting(!reacting) }
        }
        frame.addView(avatar)
        col.addView(frame)

        wave = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(380, 12).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = 16
            }
            background = pill("#F56A18", "#F56A18", 12f, 0)
        }
        col.addView(wave)

        statusTv = TextView(this).apply {
            text = "JARVIS is speaking..."
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#F56A18"))
            gravity = Gravity.CENTER
        }
        col.addView(statusTv)

        hintTv = TextView(this).apply {
            text = "Speak in हिन्दी · tap mic or portrait when done"
            textSize = 12f
            setTextColor(Color.parseColor("#A6A6A6"))
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 14)
        }
        col.addView(hintTv)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = pill("#121212", "#242424", 28f, 2)
            setPadding(22, 16, 22, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 18 }
        }
        cardTv = TextView(this).apply {
            text = "JARVIS तैयार है। बात करते समय रिएक्शन चालू रहेगा और बंद होने पर ऑफ हो जाएगा।"
            textSize = 14f
            setTextColor(Color.parseColor("#EAEAEA"))
            gravity = Gravity.CENTER
        }
        card.addView(cardTv)
        col.addView(card)

        val ctrl = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 14 }
        }
        ctrl.addView(Button(this).apply {
            text = "EYE"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#161616"))
            }
            layoutParams = LinearLayout.LayoutParams(110, 110).apply { rightMargin = 12 }
            setOnClickListener { openOverlay() }
        })
        ctrl.addView(Button(this).apply {
            text = "Stop session"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = pill("#F56A18", "#F56A18", 46f, 0)
            layoutParams = LinearLayout.LayoutParams(0, 110, 1f).apply { rightMargin = 12 }
            setOnClickListener {
                setReacting(false)
                cardTv.text = "सेशन बंद। रिएक्शन OFF है।"
            }
        })
        ctrl.addView(Button(this).apply {
            text = "MIC"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#F56A18"))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#241107"))
                setStroke(2, Color.parseColor("#4D220D"))
            }
            layoutParams = LinearLayout.LayoutParams(110, 110)
            setOnClickListener { startMic() }
        })
        col.addView(ctrl)

        chatRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
        }
        chatEt = EditText(this).apply {
            hint = "यहाँ लिखें..."
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            isSingleLine = true
            maxLines = 1
            background = pill("#141414", "#2E2E2E", 24f, 2)
            setPadding(20, 12, 20, 12)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = 8
            }
        }
        val sendBtn = Button(this).apply {
            text = "Send"
            setTextColor(Color.WHITE)
            background = pill("#F56A18", "#F56A18", 24f, 0)
            setOnClickListener {
                val m = chatEt.text.toString().trim()
                if (m.isNotEmpty()) {
                    setReacting(true)
                    cardTv.text = "JARVIS: $m"
                    runCmd(m)
                    chatEt.setText("")
                } else setReacting(false)
            }
        }
        chatRow.addView(chatEt)
        chatRow.addView(sendBtn)
        col.addView(chatRow)

        val strip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }
        strip.addView(TextView(this).apply {
            text = "Test voice"
            textSize = 12f
            setTextColor(Color.parseColor("#C8C8C8"))
            setPadding(10, 6, 10, 6)
            setOnClickListener {
                setReacting(true)
                cardTv.text = "हाँ जी बॉस, JARVIS चालू है।"
            }
        })
        strip.addView(TextView(this).apply {
            text = "· Hey JARVIS · Ready ·"
            textSize = 12f
            setTextColor(Color.parseColor("#9A9A9A"))
            setPadding(8, 6, 8, 6)
            setOnClickListener { setReacting(!reacting) }
        })
        strip.addView(TextView(this).apply {
            text = "Type"
            textSize = 12f
            setTextColor(Color.parseColor("#C8C8C8"))
            setPadding(10, 6, 10, 6)
            setOnClickListener {
                chatRow.visibility = if (chatRow.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }
        })
        col.addView(strip)

        setupBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val hScroll = HorizontalScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 10 }
        }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(chip("1. Permissions") { askPerms() })
        row.addView(chip("2. Overlay") { openOverlay() })
        row.addView(chip("3. Accessibility") { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) })
        row.addView(chip("5. Start Service") { setReacting(true); startSvc() })
        hScroll.addView(row)
        setupBox.addView(hScroll)

        val keyEt = EditText(this).apply {
            hint = "Gemini key यहाँ डालें"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            isSingleLine = true
            maxLines = 1
            background = pill("#121212", "#2A2A2A", 20f, 2)
            setPadding(20, 12, 20, 12)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 8 }
        }
        setupBox.addView(keyEt)
        setupBox.addView(Button(this).apply {
            text = "4. Key सेव करें"
            setTextColor(Color.WHITE)
            background = pill("#181818", "#F56A18", 20f, 2)
            setOnClickListener {
                val k = keyEt.text.toString().trim()
                if (k.isEmpty()) Toast.makeText(this@MainActivity, "पहले key डालें", Toast.LENGTH_SHORT).show()
                else {
                    getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE).edit().putString("gemini_key", k).apply()
                    keyEt.setText("")
                    Toast.makeText(this@MainActivity, "Key सेव हो गई", Toast.LENGTH_SHORT).show()
                }
            }
        })
        col.addView(setupBox)

        scroll.addView(col)
        rootWrap.addView(scroll)

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#050505"))
            setPadding(10, 10, 10, 14)
        }
        listOf("JARVIS", "Chat", "Tools", "Memory", "Settings").forEachIndexed { idx, label ->
            nav.addView(Button(this).apply {
                text = label
                textSize = 10f
                setTextColor(if (idx == 0) Color.parseColor("#F56A18") else Color.parseColor("#A0A0A0"))
                background = pill(if (idx == 0) "#241207" else "#050505", "#050505", 20f, 0)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener {
                    when (label) {
                        "Chat" -> chatRow.visibility = View.VISIBLE
                        "Tools", "Settings" -> { setupBox.visibility = View.VISIBLE; if (label == "Settings") openOverlay() }
                        "Memory" -> {
                            val has = !getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
                                .getString("gemini_key", null).isNullOrEmpty()
                            cardTv.text = if (has) "मेमोरी: Key सेव है।" else "मेमोरी: Key सेव नहीं है।"
                        }
                        else -> chatRow.visibility = View.GONE
                    }
                }
            })
        }
        rootWrap.addView(nav)
        setContentView(rootWrap)
        setReacting(false)
    }

    private fun pill(bg: String, stroke: String, rad: Float, sw: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = rad
        setColor(Color.parseColor(bg))
        if (sw > 0) setStroke(sw, Color.parseColor(stroke))
    }

    private fun chip(label: String, act: () -> Unit) = Button(this).apply {
        text = label
        textSize = 11f
        setTextColor(Color.WHITE)
        background = pill("#141414", "#303030", 18f, 2)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { rightMargin = 8 }
        setOnClickListener { act() }
    }

    private fun loadAvatar(iv: ImageView) {
        listOf("jarvis_character", "myra_character", "character", "avatar", "girl", "myra", "jarvis").forEach { n ->
            val id = resources.getIdentifier(n, "drawable", packageName)
            if (id != 0) { iv.setImageResource(id); return }
        }
        listOf("jarvis_character.png", "jarvis_character.jpg", "myra_character.png").forEach { f ->
            runCatching {
                assets.open(f).use { s ->
                    BitmapFactory.decodeStream(s)?.let { iv.setImageBitmap(it); return }
                }
            }
        }
        val sz = 438
        val bmp = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawCircle(sz / 2f, sz / 2f, sz / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 0f, sz.toFloat(), Color.parseColor("#2B1206"), Color.parseColor("#0C0502"), Shader.TileMode.CLAMP)
        })
        c.drawCircle(sz / 2f, sz * 0.43f, 115f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#DDD5EC") })
        c.drawCircle(sz / 2f, sz * 0.46f, 80f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F6C9AB") })
        c.drawRoundRect(92f, 270f, 346f, 430f, 70f, 70f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F56A18") })
        iv.setImageDrawable(BitmapDrawable(resources, bmp))
    }

    private fun startMic() {
        setReacting(true)
        statusTv.text = "Listening..."
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (hindi) "hi-IN" else Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "JARVIS से बोलें...")
        }
        runCatching { speechLauncher.launch(i) }.onFailure {
            Toast.makeText(this, "वॉयस इनपुट उपलब्ध नहीं है", Toast.LENGTH_SHORT).show()
            setReacting(false)
        }
    }

    private fun setReacting(active: Boolean) {
        reacting = active
        animSet?.cancel()
        waveAnim?.cancel()
        if (active) {
            statusTv.text = "JARVIS is speaking..."
            statusTv.setTextColor(Color.parseColor("#F56A18"))
            badge.text = "MIC LIVE"
            val sx = ObjectAnimator.ofFloat(avatar, View.SCALE_X, 0.96f, 1.07f).apply { repeatCount = ValueAnimator.INFINITE; repeatMode = ValueAnimator.REVERSE; duration = 500 }
            val sy = ObjectAnimator.ofFloat(avatar, View.SCALE_Y, 0.96f, 1.07f).apply { repeatCount = ValueAnimator.INFINITE; repeatMode = ValueAnimator.REVERSE; duration = 500 }
            val rt = ObjectAnimator.ofFloat(avatar, View.ROTATION, -3.5f, 3.5f).apply { repeatCount = Va
