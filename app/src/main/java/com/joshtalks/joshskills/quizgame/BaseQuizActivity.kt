package com.joshtalks.joshskills.quizgame

import android.content.IntentFilter
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.USER_LEAVE_THE_GAME
import com.joshtalks.joshskills.quizgame.base.GameEventLiveData
import com.joshtalks.joshskills.quizgame.util.UpdateReceiver

abstract class BaseQuizActivity : AppCompatActivity() {
    protected var event = GameEventLiveData
    private var updateReceiver: UpdateReceiver? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}