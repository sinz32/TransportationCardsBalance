package me.sinz.transitcard

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import me.sinz.library.SinZ

class MainActivity : Activity() {

    private var txt: TextView? = null
    private var adapter: NfcAdapter? = null
    private var intent: PendingIntent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this)
        layout.orientation = 1
        txt = TextView(this)
        txt?.setText(R.string.scan_card)
        txt?.layoutParams = LinearLayout.LayoutParams(-1, -1)
        txt?.textSize = 32f
        txt?.gravity = Gravity.CENTER or Gravity.CENTER_VERTICAL
        layout.addView(txt)
        val pad = SinZ.dip2px(this, 16)
        txt?.setPadding(pad, pad, pad, pad)
        setContentView(layout)

        adapter = NfcAdapter.getDefaultAdapter(this)
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (Build.VERSION.SDK_INT >= 31) {
            //FLAG_MUTABLE 대신 FLAG_IMMUTABLE를 사용하면 잔액 정보가 안넘어옴
            this.intent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        } else {
            this.intent = PendingIntent.getActivity(this, 0, intent, 0)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        try {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return
            parseCardData(tag)
        } catch (e: Exception) {
            toast("교통카드 잔액 읽기 실패\n$e")
        }
    }

    private fun parseCardData(tag: Tag) {
        val id = IsoDep.get(tag)

        //한국 교통카드
        if (id != null) {
            val card = TMoney(id)
            txt?.text = card.type + "\n" + card.balance + "원"
        }

        //일본 교통카드
        else {

        }

    }


    override fun onResume() {
        super.onResume()
        if (adapter != null) adapter!!.enableForegroundDispatch(this, intent, null, null)
    }

    override fun onPause() {
        super.onPause()
        if (adapter != null) adapter!!.disableForegroundDispatch(this)
    }

    private fun toast(msg: String) {
        runOnUiThread { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
    }

}