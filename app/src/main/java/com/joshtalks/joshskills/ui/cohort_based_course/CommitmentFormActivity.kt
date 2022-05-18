package com.joshtalks.joshskills.ui.cohort_based_course

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.databinding.ActivityCommitmentFormBinding

class CommitmentFormActivity : CoreJoshActivity() {
    private val binding by lazy{
        ActivityCommitmentFormBinding.inflate(layoutInflater)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

    }
}