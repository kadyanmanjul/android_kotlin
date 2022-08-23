package com.joshtalks.joshskills.ui.callWithExpert

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import android.view.View
import android.widget.TextView
import androidx.navigation.findNavController
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.ActivityCallWithExpertBinding
import com.joshtalks.joshskills.ui.callWithExpert.viewModel.CallWithExpertViewModel

class CallWithExpertActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCallWithExpertBinding
    private lateinit var balanceTv: TextView

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
        balanceTv.setOnClickListener {
            openWalletScreen()
        }
        attachObservers()
    }

    private fun attachObservers() {
        viewModel.creditsCount.observe(this){
            balanceTv.text = it
        }
    }

    fun openWalletScreen(){
//        findNavController().navigate(R.id.)
    }

    companion object {
        fun open(activity: AppCompatActivity){
            Intent(activity, CallWithExpertActivity::class.java).also {
                activity.startActivity(it)
            }
        }
    }
    private fun initToolbar() {
        balanceTv = findViewById<TextView>(R.id.iv_earn)
        findViewById<TextView>(R.id.iv_earn)
        with(findViewById<View>(R.id.iv_back)) {
            visibility = View.VISIBLE
            setOnClickListener {
                onBackPressed()
            }
        }
    }

}