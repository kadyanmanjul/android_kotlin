package com.joshtalks.joshskills.premium.ui.errorState

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.play.core.splitcompat.SplitCompat
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.base.BaseActivity
import com.joshtalks.joshskills.premium.core.EMPTY
import com.joshtalks.joshskills.premium.core.IS_FREE_TRIAL
import com.joshtalks.joshskills.premium.core.PrefManager
import com.joshtalks.joshskills.premium.core.Utils
import com.joshtalks.joshskills.premium.databinding.ActivityErrorBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ErrorActivity : BaseActivity() {

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        SplitCompat.installActivity(this)
    }

    private val vm: ErrorStateViewModel by lazy {
        ViewModelProvider(this)[ErrorStateViewModel::class.java]
    }
    var isFreeTrial: Boolean = PrefManager.getBoolValue(IS_FREE_TRIAL)

    val binding by lazy<ActivityErrorBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_error)
    }

    override fun initViewBinding() {
        binding.handler = this
        binding.executePendingBindings()
    }

    override fun onCreated() {
        lifecycleScope.launch (Dispatchers.IO){
            vm.saveApiFail(errorCode, payload, exception)
        }
    }

    override fun initViewState() {

    }

    fun backPressCall() {
        val intent = Intent(this, activity!!::class.java)
        startActivity(intent)
        this.finish()
    }

    fun makeCall() {
        Utils.call(this, binding.supportPhoneNumber.text.toString())
    }

    companion object {
        lateinit var errorCode: String
        lateinit var errorTitle: String
        lateinit var errorSubtitle: String
        private var activity: Activity? = null
        lateinit var payload : String
        lateinit var exception :String
        fun showErrorScreen(
            errorCode: String,
            errorTitle: String,
            errorSubtitle: String,
            activity: Activity,
            payload:String = EMPTY,
            exception: String = EMPTY
        ) {
            this.errorCode = errorCode
            this.errorTitle = errorTitle
            this.errorSubtitle = errorSubtitle
            this.activity = activity
            this.payload = payload
            this.exception = exception
            val intent = Intent(activity, ErrorActivity::class.java)
            activity.startActivity(intent)
            activity.finish()
        }
    }
}