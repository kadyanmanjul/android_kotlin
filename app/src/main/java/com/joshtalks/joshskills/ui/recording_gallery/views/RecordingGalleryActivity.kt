package com.joshtalks.joshskills.ui.recording_gallery.views

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.ActivityRecordingGalleryBinding
import com.joshtalks.joshskills.ui.recording_gallery.viewmodels.RecordingGalleryViewModel

class RecordingGalleryActivity : AppCompatActivity() {

    private val binding by lazy<ActivityRecordingGalleryBinding> {
        DataBindingUtil.setContentView(this,R.layout.activity_recording_gallery)
    }

    val viewModel by lazy {
        ViewModelProvider(this)[RecordingGalleryViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.vm =viewModel
    }
}