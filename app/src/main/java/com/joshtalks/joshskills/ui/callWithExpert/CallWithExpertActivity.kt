package com.joshtalks.joshskills.ui.callWithExpert

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.ActivityCallWithExpertBinding
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
    }

    fun openWalletScreen(){
        // TODO: Open Wallet Screen.
    }
}