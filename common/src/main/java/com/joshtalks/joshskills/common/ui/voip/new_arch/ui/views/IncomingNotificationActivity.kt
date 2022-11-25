package com.joshtalks.joshskills.common.ui.voip.new_arch.ui.views

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.View
import android.view.View.VISIBLE
import android.view.Window
import android.view.WindowManager.LayoutParams
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.voip.base.constants.SERVICE_ACTION_INCOMING_CALL_DECLINE
import com.joshtalks.joshskills.common.databinding.ActivityIncomingNotificationBinding
import com.joshtalks.joshskills.common.voip.Utils
import com.joshtalks.joshskills.common.voip.data.CallingRemoteService
import com.joshtalks.joshskills.common.voip.data.local.PrefManager as VoipPrefManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class IncomingNotificationActivity : AppCompatActivity() {

    private val notificationBinding by lazy<ActivityIncomingNotificationBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_incoming_notification)
    }
    private var powerManager: PowerManager? = null
    val wl: PowerManager.WakeLock? = null
    val wlCpu: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            this.setTurnScreenOn(true)
        }
        if ((getSystemService(POWER_SERVICE)) != null) {
            powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val isScreenOn = powerManager?.isInteractive
            if (!isScreenOn!!) {
                val wl: PowerManager.WakeLock? =
                    powerManager?.newWakeLock(PowerManager.FULL_WAKE_LOCK or
                            PowerManager.ACQUIRE_CAUSES_WAKEUP or
                            PowerManager.ON_AFTER_RELEASE, "myApp:MyLock")
                wl?.acquire(20000)
                val wlCpu: PowerManager.WakeLock? =
                    powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "myApp:mycpuMyCpuLock")
                wlCpu?.acquire(20000)
            }
        }
        window.addFlags(LayoutParams.FLAG_DISMISS_KEYGUARD)
        window.addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)

        val shouldDestroy = intent?.getBooleanExtra("destroy_activity",false)
        if(shouldDestroy == true) {
            closeActivity()
        }
        startTimer()
        checkForPremiumUser()
    }

    private fun startTimer() {
        CoroutineScope(Dispatchers.Main).launch {
            delay(20000)
            closeActivity()
        }
    }

    private fun checkForPremiumUser() {
        if (VoipPrefManager.getExpertPremiumUser()) {
            notificationBinding.cImage.setImageResource(R.drawable.josh_skill)
            notificationBinding.premiumBanner.visibility = VISIBLE
        }
    }

    private fun closeActivity() {
        finishAndRemoveTask()
    }

     fun declineCall(v:View) {
        val intent = Intent(Utils.context, CallingRemoteService::class.java).apply {
            action =
                SERVICE_ACTION_INCOMING_CALL_DECLINE
        }
        Utils.context?.startService(intent)
         closeActivity()
    }

     fun acceptCall(v: View) {
        val destination = "com.joshtalks.joshskills.ui.voip.new_arch.ui.views.VoiceCallActivity"
        val intent = Intent()
        intent.apply {
            setClassName(Utils.context!!.applicationContext, destination)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        Utils.context?.startActivity(intent)
    }

    override fun onStart() {
        super.onStart()
        notificationBinding.handler = this
    }

    override fun onPause() {
        super.onPause()
        if(wl?.isHeld == true) wl.release()
        if(wlCpu?.isHeld == true) wlCpu.release()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val shouldDestroy = intent?.getBooleanExtra("destroy_activity",false)
        if(shouldDestroy == true){
           closeActivity()
        }
    }
}