package com.joshtalks.joshskills.ui.sign_up_old

import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics

class LoginActivity : BaseActivity() {
    private val viewModel: LoginViewModel by lazy {
        ViewModelProviders.of(this).get(LoginViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        //launchFBSignUpActivity()
        addObserver()

    }


    private fun addObserver() {
        viewModel.loginStatusCallback.observe(this, Observer {
            if (it) {
                val intent = getIntentForState()
                if (intent == null) {
                    AppAnalytics.create(AnalyticsEvent.LOGIN_SUCCESS.NAME).push()
                    startActivity(getInboxActivityIntent())
                } else {
                    startActivity(intent)
                }
                finish()
            }
        })

    }

    /* private fun launchFBSignUpActivity() {
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
         AppAnalytics.create(AnalyticsEvent.OTP_ACCOUNT_KIT_ACTIVITY.NAME).push()


     }*/

    /*override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != RC_ACCOUNT_KIT)
            return

        val loginResult =
            data?.getParcelableExtra<AccountKitLoginResult>(AccountKitLoginResult.RESULT_KEY)

        if (loginResult != null) {
            if (loginResult.error != null) {
                AppAnalytics.create(AnalyticsEvent.LOGIN_ERROR.NAME).addParam("Error",loginResult.error?.userFacingMessage).push()
                showError(loginResult.error?.userFacingMessage)
                finish()
            } else if (loginResult.wasCancelled()) {
                AppAnalytics.create(AnalyticsEvent.LOGIN_CANCELLED.NAME).push()
                finish()
                // toastMessage = "Login Cancelled"
            } else {
                loginResult.authorizationCode?.let {
                    FullScreenProgressDialog.display(this)
                    viewModel.onAuthorizationCodeFetched(it)
                }
            }
        }
    }*/


    private fun showError(error: String?) {
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        AppAnalytics.create(AnalyticsEvent.BACK_PRESSED.NAME)
            .addParam("name", javaClass.simpleName)
            .push()

    }
}