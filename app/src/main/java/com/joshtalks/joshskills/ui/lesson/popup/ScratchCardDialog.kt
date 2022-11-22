package com.joshtalks.joshskills.ui.lesson.popup

import android.content.res.Resources
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.joshtalks.joshskills.base.BaseDialogFragment
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.databinding.ScratchCardDialogBinding
import com.joshtalks.joshskills.util.scratch.ScratchView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScratchCardDialog : BaseDialogFragment() {

    private lateinit var binding: ScratchCardDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ScratchCardDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = true
        savePopupImpression("SCRATCH_CARD_SHOWN")

        binding.scratchView.setRevealListener(object : ScratchView.IRevealListener {
            override fun onRevealed(scratchView: ScratchView) {
                scratchView.reveal()
                binding.cardConfetti.visibility = VISIBLE
                binding.cardConfetti.playAnimation()
            }

            override fun onRevealPercentChangedListener(scratchView: ScratchView, percent: Float) {
                if (percent >= 0.5) {
                    scratchView.reveal()
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        val d = dialog
        d?.let {
            try {
                val rect = Resources.getSystem().displayMetrics.run { Rect(0, 0, widthPixels, heightPixels) }
                val percentWidth = rect.width() * 0.8
                d.window?.setLayout(percentWidth.toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
                d.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            } catch (e: Exception) {
                val width = ViewGroup.LayoutParams.MATCH_PARENT
                val height = ViewGroup.LayoutParams.WRAP_CONTENT
                d.window?.setLayout(width, height)
            }
        }
    }

    private fun savePopupImpression(eventName: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                AppObjectController.commonNetworkService.savePopupImpression(
                    mapOf(
                        "event_name" to eventName,
                        "popup_key" to ("")
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun show(manager: FragmentManager, tag: String?) {
        if (!manager.isDestroyed && !manager.isStateSaved) {
            super.show(manager, tag)
        }
    }

    companion object {

        @JvmStatic
        fun newInstance(): ScratchCardDialog =
            ScratchCardDialog().apply {
                arguments = Bundle().apply {

                }
            }
    }
}