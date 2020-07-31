package com.joshtalks.joshskills.ui.conversation_practice.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.customview.customView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ALPHA_MAX
import com.joshtalks.joshskills.core.ALPHA_MIN
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.PractiseUser
import com.joshtalks.joshskills.core.ViewTypeForPractiseUser
import com.joshtalks.joshskills.core.custom_ui.SmoothLinearLayoutManager
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.FragmentRecordPractiseBinding
import com.joshtalks.joshskills.repository.server.conversation_practice.ConversationPractiseModel
import com.joshtalks.joshskills.ui.conversation_practice.ConversationPracticeViewModel
import com.joshtalks.joshskills.ui.conversation_practice.adapter.ARG_PRACTISE_OBJ
import com.joshtalks.joshskills.ui.conversation_practice.adapter.AudioPractiseAdapter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.util.*

class RecordPractiseFragment private constructor() : Fragment() {
    private lateinit var conversationPractiseModel: ConversationPractiseModel
    private lateinit var binding: FragmentRecordPractiseBinding
    private var audioPractiseAdapter: AudioPractiseAdapter? = null

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRV()
        initView()
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


    private fun initRV() {
        binding.recyclerView.layoutManager = SmoothLinearLayoutManager(context)
        binding.recyclerView.setHasFixedSize(false)
        audioPractiseAdapter =
            AudioPractiseAdapter(ArrayList(conversationPractiseModel.listen.toMutableList()))
        binding.recyclerView.adapter = audioPractiseAdapter
    }

    private fun initView() {
        binding.ivFirstUser.setImage(conversationPractiseModel.characterUrlA)
        binding.ivSecondUser.setImage(conversationPractiseModel.characterUrlB)
        binding.tvFirstUser.text = conversationPractiseModel.characterNameA
        binding.tvSecondUser.text = conversationPractiseModel.characterNameB
    }

    fun practiseWithFirstUser() {
        if (viewModel.isPractise) {
            viewModel.practiseWho = null
            viewModel.isPractise = false
            binding.ivTickFirstUser.visibility = View.GONE
            enableView(binding.ivSecondUser)
            disableViewTypeInAdapter(null)
        } else {
            viewModel.practiseWho = PractiseUser.FIRST
            viewModel.isPractise = true
            binding.ivTickFirstUser.visibility = View.VISIBLE
            disableView(binding.ivSecondUser)
            disableViewTypeInAdapter(ViewTypeForPractiseUser.FIRST.type)
        }
    }

    fun practiseWithSecondUser() {
        if (viewModel.isPractise) {
            viewModel.practiseWho = null
            viewModel.isPractise = false
            binding.ivTickSecondUser.visibility = View.GONE
            enableView(binding.ivFirstUser)
            disableViewTypeInAdapter(null)
        } else {
            viewModel.practiseWho = PractiseUser.SECOND
            viewModel.isPractise = true
            binding.ivTickSecondUser.visibility = View.VISIBLE
            disableView(binding.ivFirstUser)
            disableViewTypeInAdapter(ViewTypeForPractiseUser.SECOND.type)
        }
    }

    private fun disableViewTypeInAdapter(viewType: Int?) {
        audioPractiseAdapter?.filter?.filter(viewType?.toString() ?: "")

    }

    private fun enableView(view: View) {
        view.isClickable = true
        view.isEnabled = true
        view.alpha = ALPHA_MAX
    }

    private fun disableView(view: View) {
        view.isClickable = false
        view.isEnabled = false
        view.alpha = ALPHA_MIN
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

    fun abcd() {
        val bottomSheet = BottomSheet(LayoutMode.WRAP_CONTENT)
        val view =
            LayoutInflater.from(requireContext())
                .inflate(R.layout.conversation_submit_dialog, null, false)
        val dialog = MaterialDialog(requireActivity(), bottomSheet).show {
            customView(view = view)
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