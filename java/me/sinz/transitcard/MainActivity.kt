package me.sinz.transitcard

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this)
        layout.orientation = 1
        val txt = TextView(this)
        txt.setText(R.string.scan_card)
        txt.layoutParams = LinearLayout.LayoutParams(-1, -1)
        txt.textSize = 32f
        txt.gravity = Gravity.CENTER or Gravity.CENTER_VERTICAL
        layout.addView(txt)
        val pad = SinZ.dip2px(this, 16)
        txt.setPadding(pad, pad, pad, pad)
        setContentView(layout)
    }

}