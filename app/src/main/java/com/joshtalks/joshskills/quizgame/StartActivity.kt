package com.joshtalks.joshskills.quizgame

import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.databinding.ActivityStartBinding
import com.joshtalks.joshskills.quizgame.analytics.GameAnalytics
import com.joshtalks.joshskills.quizgame.ui.data.network.GameFirebaseDatabase
import com.joshtalks.joshskills.quizgame.ui.data.network.GameNotificationFirebaseData
import com.joshtalks.joshskills.quizgame.ui.main.view.fragment.ChoiceFragment
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.StartViewModelGame
import com.joshtalks.joshskills.quizgame.util.*
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StartActivity : BaseQuizActivity() {

    val vm by lazy {
        ViewModelProvider(this)[StartViewModelGame::class.java]
    }
    val binding by lazy<ActivityStartBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_start)
    }

    private var firebaseTemp: GameNotificationFirebaseData = GameNotificationFirebaseData()
    private var firebaseDatabase = GameFirebaseDatabase()
    private var mentorId: String = Mentor.getInstance().getId()

    init {
        deleteDataFromFireStore()
    }

    override fun setIntentExtras() {}

    override fun initViewBinding() {
        binding.vm = vm
        binding.executePendingBindings()
    }

    override fun onCreated() {
        try {
            PrefManager.put(USER_ACTIVE_IN_GAME, true)
            if (Utils.isInternetAvailable()){
                vm.addUserToDB()
            }
        } catch (e: Exception) {
        }
    }

    override fun initViewState() {
        event.observe(this) {
            when (it.what) {
                ON_BACK_PRESSED -> popBackStack()
                OPEN_CHOICE_SCREEN -> startQuizGame()
            }
        }
    }

    private fun popBackStack() {
        if (supportFragmentManager.backStackEntryCount > 1) {
            supportFragmentManager.popBackStack()
        } else
            onBackPressed()
    }

    fun openChoiceScreen() {
        GameAnalytics.push(GameAnalytics.Event.CLICK_ON_PLAY_BUTTON)
        AudioManagerQuiz.audioRecording.tickPlaying(this)
        lifecycleScope.launch(Dispatchers.Main) {
            delay(1000)
            binding.rectangle9.visibility = View.GONE
        }
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.container,  ChoiceFragment(), CHOICE_FRAGMENT)
            .commit()
    }

    override fun onDestroy() {
        PrefManager.put(USER_ACTIVE_IN_GAME, false)
        AudioManagerQuiz.audioRecording.stopPlaying()
        AudioManagerQuiz.audioRecording.stop5secTickPlaying()
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        AudioManagerQuiz.audioRecording.stopPlaying()
        AudioManagerQuiz.audioRecording.stop5secTickPlaying()
        PrefManager.put(USER_ACTIVE_IN_GAME, false)
        firebaseTemp.changeUserStatus(mentorId, IN_ACTIVE)
        vm.homeInactive()
    }

    override fun onRestart() {
        super.onRestart()
        playSound(R.raw.compress_background_util_quiz)
        vm.statusChange()
        PrefManager.put(USER_ACTIVE_IN_GAME, true)
        firebaseTemp.changeUserStatus(mentorId, ACTIVE)
    }

    fun playSound(sound: Int) {
        if (!AudioManagerQuiz.audioRecording.isPlaying())
            AudioManagerQuiz.audioRecording.startPlaying(this, sound, true)
    }

    fun deleteDataFromFireStore(){
        firebaseTemp.deleteRequested(mentorId)
        firebaseTemp.deleteDeclineData(mentorId)
        firebaseDatabase.deleteMuteUnmute(mentorId)
        firebaseDatabase.deleteAllData(mentorId)
        firebaseDatabase.deleteRoomData(mentorId)
        firebaseDatabase.deleteAnimUser(mentorId)
        firebaseDatabase.deleteUserPlayAgainCollection(mentorId)
        firebaseDatabase.deletePlayAgainNotification(mentorId)
        firebaseDatabase.deleteChange(mentorId)
    }

    private fun startQuizGame() {
        if (PermissionUtils.isCallingPermissionEnabled(this)) {
            if (Utils.isInternetAvailable()) {
                openChoiceScreen()
                return
            }else{
                showToast(getString(R.string.internet_not_available_msz))
            }
        }
        PermissionUtils.callingFeaturePermission(
            this,
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (report.isAnyPermissionPermanentlyDenied) {
                            PermissionUtils.callingPermissionPermanentlyDeniedDialog(
                                this@StartActivity,
                                message = R.string.call_start_permission_message
                            )
                            return
                        }
                        if (flag) {
                            if (Utils.isInternetAvailable()) {
                                openChoiceScreen()
                                return
                            }else{
                                showToast(getString(R.string.internet_not_available_msz))
                            }
                        } else {
                            MaterialDialog(this@StartActivity).show {
                                message(R.string.call_start_permission_message)
                                positiveButton(R.string.ok)
                            }
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            }
        )
    }
}