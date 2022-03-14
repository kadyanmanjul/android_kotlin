package com.joshtalks.joshskills.ui.voip

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.ui.call.WebrtcRepository
import kotlinx.android.synthetic.main.activity_call.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

private const val TAG = "CallActivity"

class CallActivity : AppCompatActivity() {
    val scope = CoroutineScope(Dispatchers.IO)
    val handler by lazy{Handler(mainLooper)}
    val webrtc = WebrtcRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        btn_incoming_connect.setOnClickListener {
//            call connect here
        }
        btn_incoming_disconnect.setOnClickListener {
//            call disconnect here

        }
    }

    override fun onStart() {
        super.onStart()
        webrtc.startService()
    }
}