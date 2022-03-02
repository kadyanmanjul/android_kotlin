package com.joshtalks.joshskills.ui.voip

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.joshtalks.joshskills.R
import kotlinx.android.synthetic.main.activity_call.*

class CallActivity : AppCompatActivity() {
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
}