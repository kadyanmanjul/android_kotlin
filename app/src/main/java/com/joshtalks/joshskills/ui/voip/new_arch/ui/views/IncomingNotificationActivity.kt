package com.joshtalks.joshskills.ui.voip.new_arch.ui.views

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.view.Window
import android.view.WindowManager.LayoutParams
import androidx.databinding.DataBindingUtil
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseActivity
import com.joshtalks.joshskills.databinding.ActivityIncomingNotificationBinding

class IncomingNotificationActivity : BaseActivity() {

    private val notificationBinding by lazy<ActivityIncomingNotificationBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_incoming_notification)
    }
    private var powerManager: PowerManager? = null
    val wl: PowerManager.WakeLock? = null
    val wlCpu: PowerManager.WakeLock? = null

    override fun initViewBinding() {
        notificationBinding
    }

    override fun onCreated() {
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
    }

    override fun initViewState() {}

    override fun onPause() {
        super.onPause()
        if(wl?.isHeld == true) wl.release()
        if(wlCpu?.isHeld == true) wlCpu.release()
    }


}