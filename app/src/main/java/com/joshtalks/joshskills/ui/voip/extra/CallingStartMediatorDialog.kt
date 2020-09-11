package com.joshtalks.joshskills.ui.voip.extra

import android.app.Dialog
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.interfaces.OnDismissWithDialog
import com.joshtalks.joshskills.databinding.FragmentDialogCallingStartMediatorBinding

class CallingStartMediatorDialog : DialogFragment() {
    private var listener: OnDismissWithDialog? = null
    private lateinit var binding: FragmentDialogCallingStartMediatorBinding
    private var timer: CountDownTimer? = null
    private var time = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.full_dialog)
        listener = requireActivity() as OnDismissWithDialog
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : Dialog(requireActivity(), R.style.full_dialog) {
            override fun onBackPressed() {
                listener?.onDismiss()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.run {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            window?.setLayout(width, height)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_dialog_calling_start_mediator,
                container,
                false
            )
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rippleBg.startPulse()
        startTimer()
    }

    private fun startTimer() {
        timer = object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (timer != null && isAdded && isVisible) {
                    AppObjectController.uiHandler.post {
                        binding.tvCountDown.text = (time).toString()
                        time--
                    }
                }
            }

            override fun onFinish() {
                if (isAdded && isVisible) {
                    dismissAllowingStateLoss()
                    listener?.onSuccessDismiss()
                }
            }
        }
        timer?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        timer = null
    }

    fun stopCalling() {
        listener?.onDismiss()
    }


    companion object {
        @JvmStatic
        fun newInstance() = CallingStartMediatorDialog()
    }

}