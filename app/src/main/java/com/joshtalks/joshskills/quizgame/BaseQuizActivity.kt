package com.joshtalks.joshskills.quizgame

import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.custom_ui.PointSnackbar
import com.joshtalks.joshskills.quizgame.base.EventLiveData
import com.joshtalks.joshskills.quizgame.util.AudioManagerQuiz
import com.joshtalks.joshskills.quizgame.util.UpdateReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class BaseQuizActivity : AppCompatActivity() {
    protected var event = EventLiveData
    private var updateReceiver: UpdateReceiver? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        PrefManager.put(USER_ACTIVE_IN_GAME, true)
        PrefManager.put(USER_LEAVE_THE_GAME, false)
        updateReceiver = UpdateReceiver()

        val intentFilterForUpdate = IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
        registerReceiver(updateReceiver, intentFilterForUpdate)

        setIntentExtras()
        initViewBinding()
        onCreated()
        initViewState()
    }

    protected abstract fun setIntentExtras()
    protected abstract fun initViewBinding()
    protected abstract fun onCreated()
    protected abstract fun initViewState()

    protected fun showToast(msg : String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}