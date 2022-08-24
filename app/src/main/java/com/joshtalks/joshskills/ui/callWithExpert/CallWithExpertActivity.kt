package com.joshtalks.joshskills.ui.callWithExpert

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import android.view.View
import androidx.fragment.app.commit
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.ActivityCallWithExpertBinding
import com.joshtalks.joshskills.ui.callWithExpert.fragment.ExpertListFragment
import com.joshtalks.joshskills.ui.callWithExpert.viewModel.CallWithExpertViewModel

class CallWithExpertActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCallWithExpertBinding

    private val viewModel by lazy {
        ViewModelProvider(this)[CallWithExpertViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_call_with_expert)
        binding.lifecycleOwner = this
        binding.handler = this
        binding.viewModel = this.viewModel
        initToolbar()
        openExpertList()
    }

    fun openWalletScreen(){
        // TODO: Open Wallet Screen.
    }
    private fun initToolbar() {
        with(findViewById<View>(R.id.iv_back)) {
            visibility = View.VISIBLE
            setOnClickListener {
                onBackPressed()
            }
        }
    }

    fun openExpertList(){
        supportFragmentManager.commit {
            val fragment = ExpertListFragment()
            replace(R.id.fragmentContainer, fragment, "CALL WITH EXPERT")
        }
    }
}