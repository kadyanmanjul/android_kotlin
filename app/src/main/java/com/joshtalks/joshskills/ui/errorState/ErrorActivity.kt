package com.joshtalks.joshskills.ui.errorState

import androidx.databinding.DataBindingUtil
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseActivity
import com.joshtalks.joshskills.databinding.ActivityErrorBinding

class ErrorActivity : BaseActivity() {

    val binding by lazy<ActivityErrorBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_error)
    }

    override fun initViewBinding() {
        binding.executePendingBindings()
    }

    override fun onCreated() {
        binding.errorTitle.text = errorTitle
        binding.errorCode.text = errorCode
        binding.errorSubTitle.text = errorSubtitle
    }

    override fun initViewState() {

    }

    companion object {
        var errorCode: String = ""
        var icon: Int = 0
        lateinit var errorTitle: String
        lateinit var errorSubtitle: String
        fun showErrorScreen(
            icon: Int,
            errorCode: String = "",
            errorTitle: String,
            errorSubtitle: String
//            onActionClick: () -> Unit = {}
        ) {
            this.errorCode = errorCode
            this.errorTitle = errorTitle
            this.errorSubtitle = errorSubtitle
            this.icon = icon
        }
    }
}