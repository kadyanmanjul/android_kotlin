package com.joshtalks.joshskills.ui.signup

import com.joshtalks.joshskills.core.CoreJoshActivity

abstract class BaseRegistrationActivity : CoreJoshActivity() {
    abstract fun openChooseLanguageFragment()
    abstract fun openChooseGoalFragment()
    abstract fun startFreeTrial(testId:String)
}