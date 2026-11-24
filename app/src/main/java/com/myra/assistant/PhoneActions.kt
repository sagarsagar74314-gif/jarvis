package com.myra.assistant

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

class PhoneActions(
    private val context: Context,
    private val say: (String) -> Unit
) {

    private val filler = setOf(
        "टैप", "क्लिक", "click", "दबाओ", "करो", "कर", "दो", "पर", "को",
        "फोल्डर", "folder", "बनाओ", "बना", "नाम", "का", "से", "एक", "create"
    )

    // True लौटाए तो काम हो गया, वरना बाकी कोड आगे देखेगा
    fun handle(text: String): Boolean {
        return when {
            has(text, "फोल्डर बना", "folder बना", "create folder") -> {
                createFolder(words(text))
                true
            }

            has(text, "डाउनलोड", "download") -> {
                val url = Regex("https?://\\S+").find(text)?.value
                if (url != null) {
                    download(url)
                    true
                } else {
                    false
                }
            }

            has(text, "वापस जाओ", "पीछे जाओ", "बैक", "go back") ->
                access { MyraAccessibilityService.goBack() }

            has(text, "होम स्क्रीन", "होम पर", "घर जाओ", "go home") ->
                access { MyraAccessibilityService.goHome() }

            has(text, "रिसेंट", "recent") ->
                access { MyraAccessibilityService.showRecents() }

            has(text, "नोटिफिकेशन खोल", "नोटिफिकेशन दिखाओ", "notification") ->
                access { MyraAccessibilityService.showNotifications() }

            has(text, "स्क्रॉल", "scroll") -> {
                val forward = !has(text, "ऊपर", "up")
                access {
                    if (!MyraAccessibilityService.scroll(forward)) say("स्क्रॉल नहीं हो पाया")
                }
            }

            has(text, "लिखो", "टाइप करो", "type") -> {
                val keyword = listOf("लिखो", "टाइप करो", "type").first { text.contains(it) }
                val content = text.substringAfter(keyword).trim()
                access {
                    if (content.isEmpty()) {
                        say("क्या लिखूँ?")
                    } else if (!MyraAccessibilityService.typeText(content)) {
                        say("लिखने की जगह नहीं मिली, पहले उस खाने पर दबाओ")
                    }
                }
            }

            has(text, "टैप", "क्लिक", "click", "दबाओ") -> {
                val label = words(text)
                access {
                    if (label.isEmpty()) {
                        say("किस पर दबाऊँ?")
                    } else if (!MyraAccessibilityService.clickByText(label)) {
                        say("स्क्रीन पर \"$label\" नहीं मिला")
                    }
                }
            }

            else -> false
        }
    }

    private fun access(action: () -> Unit): Boolean {
        if (!MyraAccessibilityService.isReady()) {
            say("पहले एक्सेसिबिलिटी चालू करो")
            return true
        }
        action()
        return true
    }

    private fun has(text: String, vararg keys: String): Boolean {
        return keys.any { text.contains(it) }
    }

    private fun words(text: String): String {
        return text.split(" ")
            .filter { it.isNotBlank() && it !in filler }
            .joinToString(" ")
            .replace(Regex("[^\\p{L}\\p{M}\\p{N} _-]"), "")
            .trim()
    }

    private fun createFolder(name: String) {
        if (name.isEmpty()) {
            say("फोल्डर का नाम बोलो")
            return
        }
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "Jarvis.txt")
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/Jarvis/$name")
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Files.getContentUri("external"), values
                )
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write("Jarvis".toByteArray())
                    }
                    say("फोल्डर बन गया: Documents/Jarvis/$name")
                } else {
                    say("फोल्डर नहीं बन पाया")
                }
            } else {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    "Jarvis/$name"
                )
                say(if (dir.mkdirs()) "फोल्डर बन गया" else "फोल्डर नहीं बन पाया")
            }
        } catch (e: Exception) {
            say("फोल्डर नहीं बन पाया")
        }
    }

    private fun download(url: String) {
        try {
            val fileName = url.substringAfterLast("/").substringBefore("?").ifEmpty { "file" }
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(fileName)
                .setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS, "Jarvis/$fileName"
                )
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
            say("डाउनलोड शुरू हो गया")
        } catch (e: Exception) {
            say("डाउनलोड नहीं हो पाया")
        }
    }
}
