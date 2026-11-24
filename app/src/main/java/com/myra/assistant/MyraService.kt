package com.myra.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView

class MyraService : Service() {

    private var recognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null
    private var orb: TextView? = null
    private var awaiting = false
    private var running = true
    private lateinit var commands: CommandHandler

    private val wakeWords = listOf(
        "hey jarvis", "हे जार्विस", "हे जारविस", "हे जर्विस",
        "jarvis", "जार्विस", "जारविस", "जर्विस", "जारवीस", "जार्वीस"
    )

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        commands = CommandHandler(this)
        startAsForeground()
        muteBeep(true)
        startListening()
    }

    private fun muteBeep(mute: Boolean) {
        try {
            val audio = getSystemService(AUDIO_SERVICE) as AudioManager
            val flag = if (mute) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE
            audio.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, flag, 0)
            audio.adjustStreamVolume(AudioManager.STREAM_SYSTEM, flag, 0)
        } catch (e: Exception) {
        }
    }

    private fun startAsForeground() {
        val channelId = "jarvis_channel"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(channelId, "Jarvis", NotificationManager.IMPORTANCE_LOW)
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Jarvis चालू है")
            .setContentText("\"Jarvis\" बोलो")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(open)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(1, notification)
        }
    }

    private fun startListening() {
        if (!running) return
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: ""
                handleSpeech(text)
                restart(700)
            }
            override fun onError(error: Int) {
                val wait = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 1200L
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 3000L
                    else -> 2000L
                }
                restart(wait)
            }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        }
        recognizer?.startListening(intent)
    }

    private fun restart(delay: Long) {
        handler.removeCallbacks(restartTask)
        handler.postDelayed(restartTask, delay)
    }

    private val restartTask = Runnable { startListening() }

    private fun handleSpeech(text: String) {
        val lower = text.lowercase().trim()
        if (lower.isEmpty()) return

        if (awaiting) {
            awaiting = false
            runCommand(lower)
            return
        }

        val hit = wakeWords.firstOrNull { lower.contains(it) } ?: return
        showOrb()
        val rest = lower.substringAfter(hit).trim()
        if (rest.isEmpty()) {
            awaiting = true
            handler.postDelayed({
                awaiting = false
                hideOrb()
            }, 8000)
        } else {
            runCommand(rest)
        }
    }

    private fun runCommand(command: String) {
        commands.handle(command)
        handler.postDelayed({ hideOrb() }, 3000)
    }

    private fun showOrb() {
        if (orb != null) return
        if (!Settings.canDrawOverlays(this)) return
        val view = TextView(this).apply {
            text = "J"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#7C4DFF"))
            }
        }
        val params = WindowManager.LayoutParams(
            180, 180,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }
        windowManager?.addView(view, params)
        orb = view
    }

    private fun hideOrb() {
        orb?.let { windowManager?.removeView(it) }
        orb = null
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacksAndMessages(null)
        recognizer?.destroy()
        hideOrb()
        muteBeep(false)
        super.onDestroy()
    }
}
