package com.kozzzak.ios

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout

/** Debug-only editor for cross-window IME integration tests. */
class InputProbeActivity : Activity() {
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN or android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        val layout=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;setPadding(20,80,20,40)}
        listOf(Triple(R.id.probe_text,"Text",InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE),
            Triple(R.id.probe_password,"Password",InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD),
            Triple(R.id.probe_number,"Number",InputType.TYPE_CLASS_NUMBER)).forEach {(id,label,type) ->
            layout.addView(EditText(this).apply {this.id=id;hint=label;inputType=type;setSingleLine(type and InputType.TYPE_TEXT_FLAG_MULTI_LINE==0)},LinearLayout.LayoutParams(-1,150))
        }
        setContentView(layout)
    }
}
