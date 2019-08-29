package com.joshtalks.joshskills.ui.sign_up_old

import android.content.Intent
import android.os.Bundle
import android.widget.Toast

import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders

import com.facebook.CallbackManager
import com.facebook.accountkit.AccountKitLoginResult
import com.facebook.accountkit.ui.AccountKitActivity
import com.facebook.accountkit.ui.AccountKitConfiguration
import com.facebook.accountkit.ui.LoginType
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.RC_ACCOUNT_KIT
import com.joshtalks.joshskills.core.custom_ui.FullScreenProgressDialog
import com.joshtalks.joshskills.databinding.ActivityOnboardBinding


class OnBoardActivity : CoreJoshActivity() {

    private lateinit var layout: ActivityOnboardBinding
    private var callbackManager: CallbackManager? = null

    private val viewModel: LoginViewModel by lazy {
        ViewModelProviders.of(this).get(LoginViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        layout = DataBindingUtil.setContentView(
            this,
            R.layout.activity_onboard
        )

        layout.handler = this
        addObserver()

    }

    fun signUp() {
        val intent = Intent(this, AccountKitActivity::class.java)
        val configurationBuilder = AccountKitConfiguration.AccountKitConfigurationBuilder(
            LoginType.PHONE,
            AccountKitActivity.ResponseType.CODE
        )
        intent.putExtra(
            AccountKitActivity.ACCOUNT_KIT_ACTIVITY_CONFIGURATION,
            configurationBuilder.build()
        )
        startActivityForResult(intent, RC_ACCOUNT_KIT)

    }

    private fun addObserver() {
        viewModel.loginStatusCallback.observe(this, Observer {
            if (it) {
                val intent = getConfigIntent()
                startActivity(intent)
                finish()
            }else{

            }
        })

    }

    override fun onBackPressed() {
        super.onBackPressed()
        this@OnBoardActivity.finish()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != RC_ACCOUNT_KIT)
            return

        val loginResult =
            data?.getParcelableExtra<AccountKitLoginResult>(AccountKitLoginResult.RESULT_KEY)

        if (loginResult != null) {
            if (loginResult.error != null) {
                showError(loginResult.error!!.userFacingMessage)
            } else if (loginResult.wasCancelled()) {
                // toastMessage = "Login Cancelled";

            } else {
                loginResult.authorizationCode?.let {
                    FullScreenProgressDialog.display(this)
                    viewModel.onAuthorizationCodeFetched(it)
                }
            }
        }
    }


    private fun showError(error: String) {
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
    }


}
