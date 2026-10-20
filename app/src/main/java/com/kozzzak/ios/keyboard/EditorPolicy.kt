package com.kozzzak.ios.keyboard

import android.text.InputType
import android.view.inputmethod.EditorInfo

data class EditorPolicy(val sensitive: Boolean, val words: Boolean, val layout: String) {
    companion object {
        fun from(info: EditorInfo): EditorPolicy {
            val cls = info.inputType and InputType.TYPE_MASK_CLASS
            val variation = info.inputType and InputType.TYPE_MASK_VARIATION
            val password = (cls == InputType.TYPE_CLASS_TEXT && variation in setOf(
                InputType.TYPE_TEXT_VARIATION_PASSWORD, InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)) ||
                (cls == InputType.TYPE_CLASS_NUMBER && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD)
            val private = password || info.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != 0
            val words = !private && cls == InputType.TYPE_CLASS_TEXT &&
                variation !in setOf(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
                    InputType.TYPE_TEXT_VARIATION_URI, InputType.TYPE_TEXT_VARIATION_FILTER) &&
                info.inputType and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS == 0
            return EditorPolicy(private, words, when (cls) {
                InputType.TYPE_CLASS_NUMBER -> "number"
                InputType.TYPE_CLASS_PHONE -> "phone"
                InputType.TYPE_CLASS_DATETIME -> "number"
                else -> "letters"
            })
        }
    }
}
