package com.joshtalks.joshskills.ui.lesson

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.ActivityLessonNewBinding

class LessonSectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLessonNewBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_lesson_new)

    }
}