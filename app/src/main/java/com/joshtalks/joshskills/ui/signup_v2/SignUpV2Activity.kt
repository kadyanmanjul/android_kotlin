package com.joshtalks.joshskills.ui.signup_v2

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.ActivitySignUpV2Binding
import com.joshtalks.joshskills.ui.signup.SignUpStep1Fragment

class SignUpV2Activity : BaseActivity() {
    private lateinit var appAnalytics: AppAnalytics
    private val viewModel: SignUpV2ViewModel by lazy {
        ViewModelProvider(this).get(SignUpV2ViewModel::class.java)
    }
    private lateinit var binding: ActivitySignUpV2Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sign_up_v2)
        binding.handler = this


        supportFragmentManager.commit(true) {
            addToBackStack(SignUpStep1Fragment::class.java.name)
            setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
            add(
                R.id.container,
                SignUpOptionsFragment.newInstance(),
                SignUpOptionsFragment::class.java.name
            )
        }
    }

}

