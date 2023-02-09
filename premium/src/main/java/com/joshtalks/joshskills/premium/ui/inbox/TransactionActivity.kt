package com.joshtalks.joshskills.premium.ui.inbox

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.google.android.play.core.splitcompat.SplitCompat
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.base.BaseActivity
import com.joshtalks.joshskills.premium.core.AppObjectController
import com.joshtalks.joshskills.premium.databinding.ActivityTransactionBinding
import com.joshtalks.joshskills.premium.ui.inbox.adapter.TransactionAdapter
import com.joshtalks.joshskills.premium.ui.referral.FROM_CLASS

class TransactionActivity : BaseActivity() {

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        SplitCompat.installActivity(this)
    }

    val binding by lazy<ActivityTransactionBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_transaction)
    }

    override fun initViewBinding() {
        binding.executePendingBindings()
    }

    override fun onCreated() {
        lifecycleScope.launchWhenCreated {
            val transactions = AppObjectController.commonNetworkService.getTransactionHistory()
                .sortedByDescending { it.getTransactionTime().time }
            binding.transactionRv.adapter = TransactionAdapter(transactions)
        }
    }

    override fun initViewState() {
        findViewById<AppCompatTextView>(R.id.text_message_title).text = "Purchase history"
        findViewById<View>(R.id.iv_back).setOnClickListener {
            onBackPressed()
        }
    }

    companion object {
        @JvmStatic
        fun startActivity(context: Activity, className: String = "") {
            Intent(context, TransactionActivity::class.java).apply {
                putExtra(FROM_CLASS, className)
            }.run {
                context.startActivity(this)
            }
        }
    }
}