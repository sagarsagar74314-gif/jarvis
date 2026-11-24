package com.myra.assistant

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class MyraAccessibilityService : AccessibilityService() {

    companion object {
        var instance: MyraAccessibilityService? = null

        fun isReady(): Boolean = instance != null

        fun goHome() {
            instance?.performGlobalAction(GLOBAL_ACTION_HOME)
        }

        fun goBack() {
            instance?.performGlobalAction(GLOBAL_ACTION_BACK)
        }

        fun showRecents() {
            instance?.performGlobalAction(GLOBAL_ACTION_RECENTS)
        }

        fun showNotifications() {
            instance?.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
        }

        fun clickByText(text: String): Boolean {
            return instance?.clickText(text) ?: false
        }

        fun scroll(forward: Boolean): Boolean {
            return instance?.scrollScreen(forward) ?: false
        }

        fun typeText(text: String): Boolean {
            return instance?.setFocusedText(text) ?: false
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

    private fun clickText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(text)
        for (node in nodes) {
            var current: AccessibilityNodeInfo? = node
            while (current != null) {
                if (current.isClickable) {
                    return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                current = current.parent
            }
        }
        return false
    }

    private fun findScrollable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val found = findScrollable(node.getChild(i))
            if (found != null) return found
        }
        return null
    }

    private fun scrollScreen(forward: Boolean): Boolean {
        val target = findScrollable(rootInActiveWindow) ?: return false
        val action = if (forward) {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        } else {
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        }
        return target.performAction(action)
    }

    private fun setFocusedText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val field = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        val args = Bundle()
        args.putCharSequence(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
            text
        )
        return field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }
}
