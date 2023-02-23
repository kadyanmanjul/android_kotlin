package com.joshtalks.joshskills.ui.inbox

import android.app.Activity
import android.content.Intent
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseActivity
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.databinding.ActivityTransactionBinding
import com.joshtalks.joshskills.ui.inbox.adapter.TransactionAdapter
import com.joshtalks.joshskills.ui.referral.FROM_CLASS

class TransactionActivity : BaseActivity() {

    val binding by lazy<ActivityTransactionBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_transaction)
    }

    override fun initViewBinding() {
        binding.executePendingBindings()
    }

    override fun onCreated() {
        lifecycleScope.launchWhenCreated {
            try {
                val transactions = AppObjectController.commonNetworkService.getTransactionHistory()
                    .sortedByDescending { it.getTransactionTime().time }
                binding.transactionRv.adapter = TransactionAdapter(transactions)
            }catch (ex:Exception){
                ex.printStackTrace()
            }
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