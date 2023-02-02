package com.joshtalks.joshskills.ui.chat.extra

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.constants.*
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.databinding.BottomSheetFirstCallBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.call.data.local.VoipPref
import com.joshtalks.joshskills.ui.chat.CHAT_ROOM_ID
import com.joshtalks.joshskills.ui.extra.setOnShortSingleClickListener
import com.joshtalks.joshskills.ui.lesson.LessonActivity
import com.joshtalks.joshskills.ui.lesson.LessonViewModel
import com.joshtalks.joshskills.ui.video_player.LAST_LESSON_INTERVAL
import com.joshtalks.joshskills.ui.voip.new_arch.ui.views.VoiceCallActivity
import com.joshtalks.joshskills.voip.constant.Category
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val FIRST_CALL_POPUP_SHOWN = "FIRST_CALL_POPUP_SHOWN"
const val CALL_NOW_FIRST_CALL_POPUP = "CALL_NOW_FIRST_CALL_POPUP"
const val FIRST_CALL_POPUP = "FIRST_CALL_POPUP"

class FirstCallBottomSheet : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetFirstCallBinding

    private val viewModel by lazy {
        ViewModelProvider(requireActivity())[LessonViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        isCancelable = true
        binding = DataBindingUtil.inflate(inflater, R.layout.bottom_sheet_first_call, container, false)
        binding.lifecycleOwner = this

        initView()
        return binding.root
    }

    private fun initView() {
        savePopupImpression(FIRST_CALL_POPUP_SHOWN)

        binding.callPopupSubtitle.text =
            getString(R.string.first_call_popup_subtitle, Mentor.getInstance().getUser()?.firstName ?: "User")

        binding.callNow.setOnShortSingleClickListener {
            savePopupImpression(CALL_NOW_FIRST_CALL_POPUP)
            startPractise()
        }

        binding.cross.setOnClickListener {
            dismiss()
            val resultIntent = Intent()
            viewModel.lessonLiveData.value?.let {
                resultIntent.putExtra(CHAT_ROOM_ID, it.chatId)
                resultIntent.putExtra(LAST_LESSON_INTERVAL, it.interval)
                resultIntent.putExtra(LessonActivity.LAST_LESSON_STATUS, it.status?.name)
                resultIntent.putExtra(LESSON_NUMBER, it.lessonNo)
            }
            activity?.setResult(AppCompatActivity.RESULT_OK, resultIntent)
            activity?.finish()
        }
    }

    private fun savePopupImpression(event: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                AppObjectController.commonNetworkService.savePopupImpression(
                    mapOf(
                        "popup_key" to FIRST_CALL_POPUP,
                        "event_name" to event
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startPractise() {
        PrefManager.put(CALL_BTN_CLICKED, true)
        if (isAdded && activity != null) {
            if (PermissionUtils.isCallingPermissionEnabled(requireContext())) {
                startPracticeCall()
                return
            }
        }
        if (isAdded && activity != null) {
            PermissionUtils.callingFeaturePermission(
                requireActivity(),
                object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.areAllPermissionsGranted()?.let { flag ->
                            if (report.isAnyPermissionPermanentlyDenied) {
                                if (isAdded && activity != null) {
                                    PermissionUtils.callingPermissionPermanentlyDeniedDialog(
                                        requireActivity(),
                                        message = R.string.call_start_permission_message
                                    )
                                    return
                                }
                            }
                            if (flag) {
                                startPracticeCall()
                                return
                            } else {
                                if (isAdded && activity != null) {
                                    MaterialDialog(requireActivity()).show {
                                        message(R.string.call_start_permission_message)
                                        positiveButton(R.string.ok)
                                    }
                                }
                            }
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?,
                    ) {
                        token?.continuePermissionRequest()
                    }
                }
            )
        }
    }

    private fun startPracticeCall() {
        if (isAdded && activity != null) {
            PrefManager.increaseCallCount()
            val callIntent = Intent(requireActivity(), VoiceCallActivity::class.java)
            callIntent.apply {
                putExtra(INTENT_DATA_COURSE_ID, PrefManager.getStringValue(CURRENT_COURSE_ID).ifEmpty { DEFAULT_COURSE_ID })
                putExtra(INTENT_DATA_TOPIC_ID, "5")
                putExtra(STARTING_POINT, FROM_ACTIVITY)
                putExtra(INTENT_DATA_CALL_CATEGORY, Category.PEER_TO_PEER.ordinal)
            }
            VoipPref.resetAutoCallCount()
            dismiss()
            startActivity(callIntent)
        }
    }

    companion object {

        fun showDialog(
            supportFragmentManager: FragmentManager,
            tag: String = FirstCallBottomSheet::class.java.simpleName
        ) {
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            val prev = supportFragmentManager.findFragmentByTag(tag)
            if (prev != null) {
                return
            }
            fragmentTransaction.addToBackStack(null)
            FirstCallBottomSheet().show(supportFragmentManager, tag)
        }
    }
}