package com.joshtalks.joshskills.quizgame

import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.ActivityStartBinding
import com.joshtalks.joshskills.quizgame.ui.data.model.AddUserDb
import com.joshtalks.joshskills.quizgame.ui.data.repository.StartRepo
import com.joshtalks.joshskills.quizgame.ui.main.view.fragment.ChoiceFragnment
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.StartViewModel
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.StartViewProviderFactory
import com.joshtalks.joshskills.quizgame.util.AudioManagerQuiz
import com.joshtalks.joshskills.quizgame.util.UpdateReceiver
import com.joshtalks.joshskills.repository.local.model.Mentor


class StartActivity : AppCompatActivity(){
    private lateinit var startBinding:ActivityStartBinding
    private var updateReceiver: UpdateReceiver? = null
    private var startRepo : StartRepo?=null
    private var startViewModel : StartViewModel?=null
    private var factory: StartViewProviderFactory? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startBinding = DataBindingUtil.setContentView(this, R.layout.activity_start)
        startBinding.clickHandler = this
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        playSound(R.raw.compress_background_util_quiz)

        updateReceiver = UpdateReceiver()
        val intentFilterForUpdate = IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
        registerReceiver(updateReceiver, intentFilterForUpdate)

        setUpViewModel()

    }


    fun setUpViewModel(){
        try {
            startRepo = StartRepo()
            factory = StartViewProviderFactory(this.application, startRepo!!)
            startViewModel = ViewModelProvider(this, factory!!).get(StartViewModel::class.java)
            startViewModel = factory.let { ViewModelProvider(this, it!!).get(StartViewModel::class.java) }
            try {
                if (UpdateReceiver.isNetworkAvailable(this)){
                    startViewModel?.addUserToDB(
                        AddUserDb(
                            Mentor.getInstance().getUserId(),
                            Mentor.getInstance().getUser()?.firstName,
                            Mentor.getInstance().getUser()?.photo)
                    )
                }
            } catch (e: Exception) {
                // showToast(e.message?:"")
            }
        }catch (ex:Exception){

        }
    }

    fun startQuiz(){
        startViewModel?.addData?.observe(this, androidx.lifecycle.Observer {
            Log.d("start_activty", "onCreate: "+it.message)
        })
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.container,  ChoiceFragnment())
            .commit()
        startBinding.rootLayout.visibility = View.INVISIBLE
        startBinding.rectangle9.visibility = View.INVISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        AudioManagerQuiz.audioRecording.stopPlaying()
    }

    override fun onPause() {
        super.onPause()
        AudioManagerQuiz.audioRecording.stopPlaying()
    }

    override fun onRestart() {
        super.onRestart()
        playSound(R.raw.compress_background_util_quiz)
    }

    fun playSound(sound:Int){
        if (!AudioManagerQuiz.audioRecording.isPlaying()){
            AudioManagerQuiz.audioRecording.startPlaying(this,sound,true)
        }
    }
}