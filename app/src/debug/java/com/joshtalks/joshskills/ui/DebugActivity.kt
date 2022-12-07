package com.joshtalks.joshskills.ui

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.databinding.ActivityDebugBinding
import com.joshtalks.joshskills.ui.launch.LauncherActivity
import com.joshtalks.joshskills.util.ApiRequestNotification

class DebugActivity : CoreJoshActivity() {
    private lateinit var binding: ActivityDebugBinding
    private val viewModel by lazy {
        ViewModelProvider(this)[DebugViewModel::class.java]
    }
    private lateinit var countDownTimer: CountDownTimer
    private lateinit var navController: NavController
    private var isTimerRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#3C4657")
        binding = DataBindingUtil.setContentView(this, R.layout.activity_debug)
        binding.handler = this
        binding.viewModel = viewModel
        navController = findNavController(R.id.nav_host_fragment)
        initNotificationChannel()
        initTimerAndStartActivity()
    }

    private fun initNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ApiRequestNotification.createNotificationChannel()
        }
    }

    private fun initTimerAndStartActivity() {
        binding.progressBar.visibility = View.VISIBLE
        binding.progressBar.max = 5000
        binding.progressBar.isIndeterminate = false
        countDownTimer = object : CountDownTimer(5000L, 100L) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt()
                binding.tvCountdown.text = secondsLeft.toString()
                binding.progressBar.progress = (5000 - millisUntilFinished).toInt()
            }

            override fun onFinish() {
                startLauncherActivity()
            }
        }
        isTimerRunning = true
        countDownTimer.start()
        binding.btnStart.setOnClickListener { startLauncherActivity() }
        binding.tvDebugTools.setOnClickListener {
            if (isTimerRunning) {
                countDownTimer.cancel()
                isTimerRunning = false
                binding.initUI()
            }
        }
    }

    fun ActivityDebugBinding.initUI() {
        launcherContainer.visibility = View.GONE
        ivStart.setOnClickListener { startLauncherActivity() }
        navController.addOnDestinationChangedListener { _, _, _ ->
            binding.ivBack.isVisible = navController.currentDestination?.id != R.id.debugHomeFragment
        }
        binding.ivBack.setOnClickListener {
            navController.popBackStack()
        }
    }

    private fun startLauncherActivity() {
        startActivity(Intent(this@DebugActivity, LauncherActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    override fun onPause() {
        if (isTimerRunning) countDownTimer.cancel()
        super.onPause()
    }

    fun deleteGaid() {
        BottomAlertDialog()
            .setTitle("Delete Google Advertising ID")
            .setMessage("Are you sure you want to delete the GAID?")
            .setPositiveButton("Yes") { d ->
                viewModel.deleteGaid()
                d.dismiss()
            }
            .setNegativeButton("No") { d -> d.dismiss() }
            .show(supportFragmentManager)
    }

    fun clearData() {
        BottomAlertDialog()
            .setTitle("Clear all data")
            .setMessage("Are you sure you want to clear all data?")
            .setPositiveButton("Yes") { d ->
                super.logout()
                viewModel.clearData()
                d.dismiss()
            }
            .setNegativeButton("No") { d -> d.dismiss() }
            .show(supportFragmentManager)
    }

    fun deleteUser() {
        BottomAlertDialog()
            .setTitle("Delete User")
            .setMessage("Are you sure you want to delete the user?")
            .setPositiveButton("Yes") { d ->
                viewModel.deleteUser()
                d.dismiss()
            }
            .setNegativeButton("No") { d -> d.dismiss() }
            .show(supportFragmentManager)
    }
}