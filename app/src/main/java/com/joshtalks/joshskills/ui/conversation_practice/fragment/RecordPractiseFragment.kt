package com.joshtalks.joshskills.ui.conversation_practice.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.FragmentRecordPractiseBinding
import com.joshtalks.joshskills.repository.server.conversation_practice.ConversationPractiseModel
import com.joshtalks.joshskills.ui.conversation_practice.ConversationPracticeViewModel
import com.joshtalks.joshskills.ui.conversation_practice.adapter.ARG_PRACTISE_OBJ
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener

class RecordPractiseFragment private constructor() : Fragment() {
    private lateinit var conversationPractiseModel: ConversationPractiseModel
    private lateinit var binding: FragmentRecordPractiseBinding

    private val viewModel: ConversationPracticeViewModel by lazy {
        ViewModelProvider(this).get(ConversationPracticeViewModel::class.java)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            conversationPractiseModel =
                it.getParcelable<ConversationPractiseModel>(ARG_PRACTISE_OBJ) as ConversationPractiseModel
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_record_practise,
                container,
                false
            )
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }


    fun practiseWithFirstUser() {

    }

    fun practiseWithSecondUser() {

    }

    fun requestForRecording() {
        if (PermissionUtils.isAudioAndStoragePermissionEnable(requireActivity()).not()) {
            PermissionUtils.audioRecordStorageReadAndWritePermission(requireActivity(),
                object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.areAllPermissionsGranted()?.let { flag ->
                            if (flag) {
                                startRecording()
                                return
                            }
                            if (report.isAnyPermissionPermanentlyDenied) {
                                PermissionUtils.permissionPermanentlyDeniedDialog(
                                    requireActivity(),
                                    R.string.record_permission_message
                                )
                                return
                            }
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        token?.continuePermissionRequest()
                    }
                })
        } else {
            startRecording()
        }
    }

    private fun startRecording() {

        if (viewModel.practiseWho == null) {
            showToast(getString(R.string.select_your_character))
            return
        }

        if (viewModel.isRecordingRunning) {
            binding.btnRecord.backgroundTintList = ContextCompat.getColorStateList(
                AppObjectController.joshApplication,
                R.color.button_primary_color
            )
            viewModel.stopRecording()
            viewModel.isRecordingRunning = false
        } else {
            binding.btnRecord.backgroundTintList = ContextCompat.getColorStateList(
                AppObjectController.joshApplication,
                R.color.recording_9D
            )
            viewModel.startRecord()
            viewModel.isRecordingRunning = true
        }
    }

    companion object {

        @JvmStatic
        fun newInstance(conversationPractiseModel: ConversationPractiseModel) =
            RecordPractiseFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_PRACTISE_OBJ, conversationPractiseModel)
                }
            }
    }
}