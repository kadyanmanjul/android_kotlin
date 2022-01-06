package com.joshtalks.joshskills.quizgame

import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.afollestad.materialdialogs.MaterialDialog
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.USER_ACTIVE_IN_GAME
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.databinding.ActivityStartBinding
import com.joshtalks.joshskills.quizgame.ui.data.network.FirebaseDatabase
import com.joshtalks.joshskills.quizgame.ui.data.network.FirebaseTemp
import com.joshtalks.joshskills.quizgame.ui.main.view.fragment.ChoiceFragment
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.StartViewModel
import com.joshtalks.joshskills.quizgame.util.AudioManagerQuiz
import com.joshtalks.joshskills.quizgame.util.CHOICE_FRAGMENT
import com.joshtalks.joshskills.quizgame.util.ON_BACK_PRESSED
import com.joshtalks.joshskills.quizgame.util.OPEN_CHOICE_SCREEN
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener

class StartActivity : BaseQuizActivity() {

    val vm by lazy {
        ViewModelProvider(this)[StartViewModel::class.java]
    }
    val binding by lazy<ActivityStartBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_start)
    }

    private var firebaseTemp: FirebaseTemp = FirebaseTemp()
    private var firebaseDatabase = FirebaseDatabase()
    private var mentorId: String = Mentor.getInstance().getUserId()

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
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.container,  ChoiceFragment(), CHOICE_FRAGMENT)
            .commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        PrefManager.put(USER_ACTIVE_IN_GAME, false)
        AudioManagerQuiz.audioRecording.stopPlaying()
    }

    override fun onPause() {
        super.onPause()
        AudioManagerQuiz.audioRecording.stopPlaying()
        vm.homeInactive()
    }

    override fun onRestart() {
        super.onRestart()
        playSound(R.raw.compress_background_util_quiz)
        vm.statusChange()
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
    }

    // TODO: Need to refactor
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