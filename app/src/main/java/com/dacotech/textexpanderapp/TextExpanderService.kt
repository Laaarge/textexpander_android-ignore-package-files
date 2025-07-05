package com.dacotech.textexpanderapp

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log

class TextExpanderService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            val source = event.source ?: return
            val beforeText = event.beforeText?.toString()
            val addedText = event.text?.firstOrNull()?.toString()
            handleTextChanged(source, beforeText, addedText)
        }
    }

    private fun handleTextChanged(node: AccessibilityNodeInfo, beforeText: String?, addedText: String?) {
        val currentText = node.text?.toString() ?: return

        // Find the last word and the character before it
        val lastWordInfo = findLastWord(currentText)

        TriggerRepository.triggers.forEach { match ->
            // Word trigger logic
            if (match.word) {
                // Check if the last word matches the trigger and is preceded by a separator
                if (lastWordInfo != null && lastWordInfo.word == match.trigger && lastWordInfo.isPrecededBySeparator) {
                    // The user just typed a separator, which is now at the end of currentText
                    val separator = currentText.last().toString()
                    val triggerStartIndex = lastWordInfo.startIndex

                    val newText = currentText.substring(0, triggerStartIndex) + match.replace + separator

                    node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                        putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
                    })
                    return // Exit after expansion
                }
            }
            // Non-word trigger logic (instant expansion)
            else {
                if (currentText.endsWith(match.trigger)) {
                    val newText = currentText.substring(0, currentText.length - match.trigger.length) + match.replace
                    node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                        putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
                    })
                    return // Exit after expansion
                }
            }
        }
    }

    private data class LastWordInfo(val word: String, val startIndex: Int, val isPrecededBySeparator: Boolean)

    private fun findLastWord(text: String): LastWordInfo? {
        if (text.length < 2 || !isSeparator(text.last().toString())) {
            return null // Need at least one char and a separator at the end
        }

        val textBeforeSeparator = text.substring(0, text.length - 1)

        // Use lastIndexOfAny to find the start of the word more directly
        val wordStartIndex = textBeforeSeparator.lastIndexOfAny(" .,;:!?\n".toCharArray()) + 1

        val word = textBeforeSeparator.substring(wordStartIndex)
        val isPrecededBySeparator = (wordStartIndex == 0) || isSeparator(text[wordStartIndex - 1].toString())

        return LastWordInfo(word, wordStartIndex, isPrecededBySeparator)
    }

    private fun isSeparator(char: String): Boolean {
        // Basic separator check, can be expanded
        return char.isBlank() || char in ".,;:!?\n"
    }

    override fun onInterrupt() {
        // Handle service interruption (e.g., log the event)
    }
}
