package com.joshtalks.joshskills.core

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import com.joshtalks.joshskills.R

abstract class CoreJoshActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        routeApplicationState()
    }


    private fun routeApplicationState() {
        val intent = getIntentForState()
        if (intent != null) {
            startActivity(intent)
            finish()
        }
    }

    fun updateTitleBar(string: String){
        val v = LayoutInflater.from(this).inflate(R.layout.title_view, null)
        (v.findViewById(R.id.text_view) as TextView).text = string
        this.supportActionBar?.customView = v
        supportActionBar?.setBackgroundDrawable (ColorDrawable(Color.parseColor("#075E54")));

    }


}


enum class ProfileStep {
    DATE_OF_BIRTH, LANGUAGE, INTEREST, LOCALITY
}