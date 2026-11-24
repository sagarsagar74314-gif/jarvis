package com.myra.assistant

import android.Manifest
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CommandHandler(private val context: Context) {

    private var torchOn = false
    private var tts: TextToSpeech? = null
    private val brain = GeminiBrain(context)
    private val phone = PhoneActions(context) { say(it) }

    private val hindiNames = mapOf(
        "यूट्यूब" to "youtube",
        "व्हाट्सएप" to "whatsapp",
        "व्हाट्सऐप" to "whatsapp",
        "कैमरा" to "camera",
        "सेटिंग" to "settings",
        "क्रोम" to "chrome",
        "गैलरी" to "gallery",
        "फेसबुक" to "facebook",
        "इंस्टाग्राम" to "instagram",
        "जीमेल" to "gmail",
        "मैप" to "maps",
        "स्नैपचैट" to "snapchat",
        "प्ले स्टोर" to "play store",
        "कैलकुलेटर" to "calculator",
        "घड़ी" to "clock",
        "अमेज़न" to "amazon",
        "फ्लिपकार्ट" to "flipkart"
    )

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("hi", "IN")
            }
        }
    }

    private fun say(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis")
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }

    private fun launch(intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            say("यह काम नहीं हो पाया")
        }
    }

    fun handle(raw: String) {
        val text = raw.lowercase().trim()
        val short = text.length < 30
        if (phone.handle(text)) return

        when {
            short && has(text, "फ्लैशलाइट", "फ्लैश", "टॉर्च", "torch", "flashlight") -> toggleTorch(text)

            short && has(text, "समय", "टाइम", "time") -> {
                val now = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                say("अभी समय है $now")
            }

            short && has(text, "तारीख", "तारिख", "date", "आज कौन सा दिन") -> {
                val now = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("hi", "IN")).format(Date())
                say("आज $now है")
            }

            has(text, "बैटरी", "battery") -> batteryLevel()

            has(text, "कैलेंडर", "कैलेण्डर", "calendar") -> openCalendar()

            has(text, "अलार्म", "alarm") -> setAlarm(text)

            has(text, "कॉल", "call", "फोन करो", "फोन लगाओ") -> makeCall(text)

            has(text, "गाना", "गाने", "song", "चलाओ", "बजाओ") -> playSong(text)

            has(text, "डिलीट", "uninstall", "हटाओ", "अनइंस्टॉल") -> deleteApp(text)

            has(text, "शॉपिंग", "shopping", "खरीदो", "खरीदना", "ऑर्डर") -> shop(text)

            has(text, "वाईफाई", "वाई-फाई", "wifi", "wi-fi") -> openWifi()

            has(text, "ब्लूटूथ", "bluetooth") -> {
                launch(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                say("ब्लूटूथ की सेटिंग खोल रहा हूँ")
            }

            has(text, "ब्राइटनेस", "brightness", "स्क्रीन की रोशनी") -> {
                launch(Intent(Settings.ACTION_DISPLAY_SETTINGS))
                say("डिस्प्ले की सेटिंग खोल रहा हूँ")
            }

            has(text, "आवाज़ बढ़ाओ", "आवाज़ कम", "वॉल्यूम", "volume") -> {
                launch(Intent(Settings.ACTION_SOUND_SETTINGS))
                say("साउंड की सेटिंग खोल रहा हूँ")
            }

            short && has(text, "सेटिंग", "settings") -> {
                launch(Intent(Settings.ACTION_SETTINGS))
                say("सेटिंग खोल रहा हूँ")
            }

            has(text, "मौसम", "weather") -> webSearch("आज का मौसम")

            has(text, "सर्च", "search", "खोजो", "ढूंढो", "ढूँढो") -> {
                val query = strip(text, listOf("सर्च", "search", "करो", "खोजो", "ढूंढो", "ढूँढो", "गूगल", "google"))
                webSearch(query)
            }

            short && has(text, "खोल", "open", "ओपन", "चालू करो") -> openApp(text)

            else -> {
                // पहले देखो कि सिर्फ ऐप का नाम तो नहीं बोला, वरना AI से पूछो
                if (!(short && openApp(text, silent = true))) {
                    say("सोच रहा हूँ...")
                    brain.ask(raw) { answer -> say(answer) }
                }
            }
        }
    }

    private fun has(text: String, vararg words: String): Boolean {
        return words.any { text.contains(it) }
    }

    private fun strip(text: String, words: List<String>): String {
        var result = text
        words.forEach { result = result.replace(it, " ") }
        return result.trim().replace(Regex("\\s+"), " ")
    }

    private fun toggleTorch(text: String) {
        try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = cm.cameraIdList.first()
            torchOn = !has(text, "बंद", "off")
            cm.setTorchMode(id, torchOn)
            say(if (torchOn) "टॉर्च चालू" else "टॉर्च बंद")
        } catch (e: Exception) {
            say("टॉर्च नहीं चल पाई")
        }
    }

    private fun batteryLevel() {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        say("बैटरी $level प्रतिशत है")
    }

    private fun openWifi() {
        val intent = if (Build.VERSION.SDK_INT >= 29) {
            Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS)
        }
        launch(intent)
        say("वाईफाई का पैनल खोल रहा हूँ")
    }

    private fun openCalendar() {
        val uri = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()
        launch(Intent(Intent.ACTION_VIEW, uri))
        say("कैलेंडर खोल रहा हूँ")
    }

    private fun shop(text: String) {
        val query = strip(
            text,
            listOf("शॉपिंग", "shopping", "खरीदो", "खरीदना", "ऑर्डर", "करो", "करना", "मुझे", "अमेज़न", "पर")
        )
        val url = if (query.isEmpty()) {
            "https://www.amazon.in"
        } else {
            "https://www.amazon.in/s?k=" + Uri.encode(query)
        }
        launch(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        say("शॉपिंग खोल रहा हूँ")
    }

    private fun setAlarm(text: String) {
        val match = Regex("(\\d{1,2})(?:[:.](\\d{2}))?").find(text)
        if (match == null) {
            say("किस समय का अलार्म लगाऊँ?")
            return
        }
        var hour = match.groupValues[1].toInt()
        val minute = match.groupValues[2].ifEmpty { "0" }.toInt()
        val isPm = has(text, "शाम", "रात", "दोपहर", "pm")
        if (isPm && hour < 12) hour += 12
        if (hour > 23 || minute > 59) {
            say("समय सही नहीं लगा")
            return
        }
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, "Jarvis")
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        }
        launch(intent)
        say("अलार्म लग गया")
    }

    private fun findContactNumber(name: String): String? {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return null
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null, null
        )
        cursor?.use {
            while (it.moveToNext()) {
                val contact = it.getString(0)?.lowercase() ?: continue
                if (contact.length >= 2 && (contact.contains(name) || name.contains(contact))) {
                    return it.getString(1)
                }
            }
        }
        return null
    }

    private fun makeCall(text: String) {
        var number = text.filter { it.isDigit() }
        if (number.length < 5) {
            val name = strip(
                text,
                listOf("कॉल", "call", "फोन", "करो", "को", "लगाओ", "कर", "दो", "मिलाओ")
            )
            if (name.isEmpty()) {
                launch(Intent(Intent.ACTION_DIAL))
                say("किसे कॉल करना है?")
                return
            }
            val found = findContactNumber(name)
            if (found == null) {
                say("$name नाम का कॉन्टैक्ट नहीं मिला")
                return
            }
            number = found
            say("$name को कॉल कर रहा हूँ")
        }
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
        val action = if (granted) Intent.ACTION_CALL else Intent.ACTION_DIAL
        launch(Intent(action, Uri.parse("tel:$number")))
    }

    private fun playSong(text: String) {
        val query = strip(
            text,
            listOf("गाना", "गाने", "song", "चलाओ", "बजाओ", "चला दो", "बजा दो",
                "यूट्यूब", "youtube", "पर", "play", "प्ले")
        )
        val play = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
            putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
            putExtra(SearchManager.QUERY, query)
        }
        if (play.resolveActivity(context.packageManager) != null) {
            launch(play)
            say("गाना चला रहा हूँ")
        } else {
            val url = "https://www.youtube.com/results?search_query=" + Uri.encode("$query song")
            launch(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            say("गाना खोज रहा हूँ")
        }
    }

    private fun deleteApp(text: String) {
        val app = findApp(text)
        if (app == null) {
            say("कौन सा ऐप हटाना है?")
            return
        }
        // फोन खुद पूछेगा कि सच में हटाना है या नहीं
        launch(Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.second}")))
        say("${app.first} हटाने के लिए पुष्टि करो")
    }

    private fun webSearch(query: String) {
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, query)
        }
        launch(intent)
    }

    private fun openApp(text: String, silent: Boolean = false): Boolean {
        val app = findApp(text)
        if (app == null) {
            if (!silent) say("यह ऐप नहीं मिला")
            return false
        }
        val intent = context.packageManager.getLaunchIntentForPackage(app.second)
        if (intent == null) {
            if (!silent) say("यह ऐप नहीं खुल पाया")
            return false
        }
        launch(intent)
        say("${app.first} खोल रहा हूँ")
        return true
    }

    private fun findApp(text: String): Pair<String, String>? {
        var fixed = text
        hindiNames.forEach { (hindi, english) -> fixed = fixed.replace(hindi, english) }

        val pm = context.packageManager
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(main, 0)

        var best: Pair<String, String>? = null
        var bestLength = 0
        for (info in apps) {
            val label = info.loadLabel(pm).toString().lowercase()
            val pkg = info.activityInfo.packageName
            if (pkg == context.packageName) continue
            if (label.length >= 3 && fixed.contains(label) && label.length > bestLength) {
                best = Pair(label, pkg)
                bestLength = label.length
            }
        }
        return best
    }
}
