package com.joshtalks.joshskills.ui.sign_up_old

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.TelephonyManager
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.ActivityRegisterInfoBinding
import com.joshtalks.joshskills.repository.local.model.CountryDetail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RegisterInfoActivity : BaseActivity() {

    private lateinit var layout: ActivityRegisterInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layout = DataBindingUtil.setContentView(this, R.layout.activity_register_info)
        layout.handler = this
        supportActionBar?.hide()
        AppAnalytics.create(AnalyticsEvent.COURSE_FAILURE_SCREEN.NAME).push()
        layout.phoneNumberEt.prefix = "+91"

        layout.tv2.text = AppObjectController.getFirebaseRemoteConfig().getString("self_register_text")


        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tm =this@RegisterInfoActivity.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                val countryCodeValue = tm.networkCountryIso
                val json: String =
                    applicationContext.assets.open("country_details.json").bufferedReader()
                        .use { it.readText() }
                val countryList = AppObjectController.gsonMapper.fromJson<List<CountryDetail>>(json,
                    object : TypeToken<List<CountryDetail>>() {}.type)
                val country= countryList.find { it.code.equals(countryCodeValue,ignoreCase = true) }
                layout.phoneNumberEt.prefix = country?.dialCode!!

            } catch (ex: Exception) {
            }
        }

    }

    fun registerUser() {
        if (layout.phoneNumberEt.text.toString().isEmpty() || layout.phoneNumberEt.text.toString().length < 10) {
            layout.phoneNumberEt.error = getString(R.string.invalid_phone_number);
            return
        } else {
            layout.phoneNumberEt.error = null
        }
        val phoneNumber = layout.phoneNumberEt.prefix + layout.phoneNumberEt.text


        val alertDialog: AlertDialog? = this@RegisterInfoActivity?.let {
            val builder = AlertDialog.Builder(it)
            builder.apply {
                setMessage(getString(R.string.anonymous_user_login))
                setPositiveButton(
                    R.string.register
                ) { dialog, _ ->
                    dialog.dismiss()
                    registerAnonymousUser(phoneNumber)
                }
            }
            builder.create()
        }
        alertDialog?.show()

    }

    fun clickToPay() {
        AppAnalytics.create(AnalyticsEvent.CLICK_TO_PAY_SELECTED.NAME).push()
        Utils.openUrl(
            AppObjectController.getFirebaseRemoteConfig().getString("registration_url"),
            this@RegisterInfoActivity
        )
    }


    override fun onBackPressed() {
        AppAnalytics.create(AnalyticsEvent.BACK_PRESSED.NAME)
            .addParam("name", javaClass.simpleName)
            .push()

        super.onBackPressed()
        val resultIntent = Intent()
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun registerAnonymousUser(phoneNumber: String) {
        AppAnalytics.create(AnalyticsEvent.UNREGISTER_USER.NAME).push()
        CoroutineScope(Dispatchers.IO).launch {
            val map = mapOf("mobile" to phoneNumber)
            try {
                AppObjectController.signUpNetworkService.registerAnonymousUser(map).await()
                PrefManager.clear()
                AppObjectController.appDatabase.clearAllTables()
                val intent = Intent(applicationContext, LoginActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)

            } catch (e: Exception) {
                e.printStackTrace()
                // onFailedToFetchLocation()
            }


        }

    }

}