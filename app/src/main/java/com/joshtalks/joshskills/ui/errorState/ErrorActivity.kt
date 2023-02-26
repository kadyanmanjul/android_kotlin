package com.joshtalks.joshskills.ui.errorState

import android.app.Activity
import android.content.Intent
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseActivity
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.IS_FREE_TRIAL
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.databinding.ActivityErrorBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ErrorActivity : BaseActivity() {


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
            vm.saveApiFail(errorCode?: EMPTY, payload, exception)
        }
    }

    override fun initViewState() {
        if (errorCode != null)
            binding.errorCodeText.text = errorCode

        if (errorTitle!=null)
            binding.errorTitleText.text = errorTitle

        if (errorSubtitle!=null)
            binding.errorSubtitleText.text = errorSubtitle
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
        private var errorCode: String? = EMPTY
        private var errorTitle: String? = EMPTY
        private var errorSubtitle: String? = EMPTY
        private var activity: Activity? = null
        private var payload: String? = EMPTY
        private var exception: String? = EMPTY
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