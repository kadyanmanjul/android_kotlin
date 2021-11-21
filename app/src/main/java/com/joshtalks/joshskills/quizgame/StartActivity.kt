package com.joshtalks.joshskills.quizgame

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.ActivityStartBinding
import com.joshtalks.joshskills.quizgame.ui.main.view.fragment.ChoiceFragnment
import com.joshtalks.joshskills.quizgame.util.AudioManagerQuiz

class StartActivity : AppCompatActivity() {
    private lateinit var startBinding:ActivityStartBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startBinding = DataBindingUtil.setContentView(this, R.layout.activity_start)
        startBinding.clickHandler=this

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
     //   playSound(R.raw.background_until_quiz_start)
    }

    fun startQuiz(){
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.container,  ChoiceFragnment())
            .commit()
        startBinding.rootLayout.visibility=View.INVISIBLE
        startBinding.rectangle9.visibility=View.INVISIBLE
    }

    fun playSound(sound:Int){
        AudioManagerQuiz.audioRecording.startPlaying(application,sound)
    }

    override fun onDestroy() {
        super.onDestroy()
        AudioManagerQuiz.audioRecording.stopPlaying()
        Log.d("ondestroy", "onDestroy: "+"onDestroyCall")
    }

}