package com.joshtalks.joshskills.ui.gif

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import com.bumptech.glide.Glide
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CoreJoshActivity
import kotlinx.android.synthetic.main.activity_gif.image

class GIFActivity : CoreJoshActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = if (Build.VERSION.SDK_INT == 26) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gif)
        Glide.with(this)
            .load(R.raw.award)
            .into(image)
    }
}
