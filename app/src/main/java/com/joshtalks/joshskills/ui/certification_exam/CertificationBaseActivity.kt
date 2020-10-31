package com.joshtalks.joshskills.ui.certification_exam

import android.os.Bundle
import android.view.WindowManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.BaseActivity

class CertificationBaseActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_certification_base)
    }


    private fun openExamInstructionScreen() {

    }


}