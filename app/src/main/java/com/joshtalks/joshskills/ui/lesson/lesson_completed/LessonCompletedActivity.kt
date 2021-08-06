package com.joshtalks.joshskills.ui.lesson.lesson_completed

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.databinding.DataBindingUtil
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.LESSON_COMPLETE_SNACKBAR_TEXT_STRING
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.extension.setImageAndFitCenter
import com.joshtalks.joshskills.core.playSnackbarSound
import com.joshtalks.joshskills.databinding.AcitivityUnlockNextClassLayoutBinding
import com.joshtalks.joshskills.repository.local.entity.LessonModel
import com.joshtalks.joshskills.ui.video_player.IS_BATCH_CHANGED

class LessonCompletedActivity : BaseActivity() {

    private lateinit var lessonName: TextView
    private lateinit var lessonIv: ImageView
    private lateinit var descTv: TextView

    private var lessonModel: LessonModel? = null
    private var snackBarText: String? = null
    private lateinit var binding: AcitivityUnlockNextClassLayoutBinding

    companion object {
        private val LESSON_MODEL = "lesson_model"
        fun getActivityIntent(
            context: Context,
            lessonModel: LessonModel
        ) = Intent(context, LessonCompletedActivity::class.java).apply {
            putExtra(LESSON_MODEL, lessonModel)
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lessonModel = intent.getParcelableExtra(LESSON_MODEL)
        snackBarText = PrefManager.getStringValue(LESSON_COMPLETE_SNACKBAR_TEXT_STRING,false, EMPTY)
        PrefManager.put(LESSON_COMPLETE_SNACKBAR_TEXT_STRING, EMPTY,false)

        binding = DataBindingUtil.setContentView(
            this,
            R.layout.acitivity_unlock_next_class_layout
        )
        binding.handler = this
        val card = findViewById<CardView>(R.id.card)

        val animation: Animation =
            AnimationUtils.loadAnimation(this, R.anim.enter_from_right_rotating)
        card.animation = animation

        lessonName = findViewById(R.id.lesson_name_tv)
        lessonIv = findViewById(R.id.lesson_iv)
        descTv = findViewById(R.id.description_tv)

        lessonModel?.let {
            lessonName.text = getString(R.string.lesson_name, it.lessonNo, it.lessonName)
            lessonIv.setImageAndFitCenter(it.thumbnailUrl)
        }
        if (snackBarText.isNullOrBlank().not()){
            showSnackBar(binding.rootView, Snackbar.LENGTH_LONG, snackBarText)
            playSnackbarSound(this)
        }

        findViewById<TextView>(R.id.continue_btn).setOnClickListener {
            setResult(RESULT_OK, Intent().apply { putExtra(IS_BATCH_CHANGED, false) })
            finish()
        }
    }

    override fun onBackPressed() {
        setResult(RESULT_OK, Intent().apply { putExtra(IS_BATCH_CHANGED, false) })
        finish()
        super.onBackPressed()
    }
}