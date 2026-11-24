package com.myra.assistant

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class MyraAccessibilityService : AccessibilityService() {

    companion object {
        var instance: MyraAccessibilityService? = null

        fun goHome() {
            instance?.performGlobalAction(GLOBAL_ACTION_HOME)
        }

        fun goBack() {
            instance?.performGlobalAction(GLOBAL_ACTION_BACK)
        }

        fun showRecents() {
            instance?.performGlobalAction(GLOBAL_ACTION_RECENTS)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }
}
