package com.joshtalks.joshskills.ui.signup

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.databinding.ActivitySignUpBinding

class SignUpActivity : BaseActivity(),VerifyDialogFragmentListener {

    private lateinit var layout: ActivitySignUpBinding

    private val viewModel: SignUpViewModel by lazy {
        ViewModelProviders.of(this).get(SignUpViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layout = DataBindingUtil.setContentView(this, R.layout.activity_sign_up)
        supportActionBar?.hide()
/*
        supportFragmentManager.commit(true) {
            addToBackStack("...")
            *//*setCustomAnimations(
                R.anim.enter_anim,'
                R.anim.exit_anim)*//*
            add(R.id.container, SignUpStep1Fragment.newInstance(), SignUpStep1Fragment::class.java.name)
        }*/


        /*val prefs = getPref(this)
        prefs?.edit {
            clear()
        }*/
        addObserver()
    }

    private fun addObserver() {
        viewModel.signUpStatus.observe(this, Observer {

            when (it) {
                /*SignUpStepStatus.SignUpStepFirst -> supportFragmentManager.commit(true) {
                    addToBackStack(SignUpStep2Fragment::class.java.name)
                    add(R.id.container, SignUpStep2Fragment.newInstance(), SignUpStep2Fragment::class.java.name)
                }

                SignUpStepStatus.SignUpStepSecond -> confirmMobilNumberScreen()
*/


            }


        })

    }

    private fun confirmMobilNumberScreen(){
        var verifyDialogFragment =VerifyDialogFragment.newInstance(viewModel.phoneNumber)
        verifyDialogFragment.show(supportFragmentManager.beginTransaction(),VerifyDialogFragment::class.java.name)
    }
    override fun edit() {

    }

    override fun ok() {
        /*supportFragmentManager.commit(true) {
            addToBackStack(SignUpStep3Fragment::class.java.name)
            add(
                R.id.container,
                SignUpStep3Fragment.newInstance(viewModel.phoneNumber),
                SignUpStep3Fragment::class.java.name
            )
        }*/

    }


}
