package com.joshtalks.joshskills.ui.chat.extra

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.databinding.BottomSheetFirstCallBinding
import com.joshtalks.joshskills.ui.extra.setOnShortSingleClickListener
import com.joshtalks.joshskills.ui.lesson.LessonActivity
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val FIRST_CALL_POPUP_SHOWN = "FIRST_CALL_POPUP_SHOWN"
const val CALL_NOW_FIRST_CALL_POPUP = "CALL_NOW_FIRST_CALL_POPUP"
const val FIRST_CALL_POPUP = "FIRST_CALL_POPUP"

class FirstCallBottomSheet : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetFirstCallBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        isCancelable = true
        binding = DataBindingUtil.inflate(inflater, R.layout.bottom_sheet_first_call, container, false)
        binding.lifecycleOwner = this

        initView()
        return binding.root
    }

    private fun initView() {
        savePopupImpression(FIRST_CALL_POPUP_SHOWN)
        binding.callNow.setOnShortSingleClickListener {
            savePopupImpression(CALL_NOW_FIRST_CALL_POPUP)
            startPractise()
        }

        binding.cross.setOnClickListener { dismiss() }
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
            lifecycleScope.launch(Dispatchers.IO) {
                val lessonId = AppObjectController.appDatabase.lessonDao()
                    .getLastLessonIdForCourse(
                        (PrefManager.getStringValue(CURRENT_COURSE_ID).ifEmpty { DEFAULT_COURSE_ID }).toInt()
                    )
                val conversationId = AppObjectController.appDatabase.courseDao()
                    .getConversationIdFromCourseId(
                        (PrefManager.getStringValue(CURRENT_COURSE_ID).ifEmpty { DEFAULT_COURSE_ID })
                    )
                withContext(Dispatchers.Main) {
                    startActivity(
                        LessonActivity.getActivityIntent(
                            requireActivity(),
                            lessonId = lessonId,
                            conversationId = conversationId,
                            shouldStartCall = true
                        )
                    )
                    dismiss()
                }
            }
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
                fragmentTransaction.remove(prev)
            }
            fragmentTransaction.addToBackStack(null)
            FirstCallBottomSheet().show(supportFragmentManager, tag)
        }
    }
}