package com.joshtalks.joshskills.ui.voip.extra

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.IS_PRACTISE_PARTNER_VIEWED
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.core.interfaces.OnDismissWithSuccess
import com.joshtalks.joshskills.databinding.PractisePartnerLayoutBinding
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve

class PractisePartnerDialogFragment : DialogFragment() {
    private var listener: OnDismissWithSuccess? = null
    private lateinit var binding: PractisePartnerLayoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.full_dialog)
        listener = requireActivity() as OnDismissWithSuccess
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
                R.layout.practise_partner_layout,
                container,
                false
            )
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.builder
            .setHasFixedSize(true)
            .setLayoutManager(LinearLayoutManager(requireContext()))
        binding.recyclerView.addItemDecoration(
            LayoutMarginDecoration(
                Utils.dpToPx(
                    requireContext(),
                    12f
                )
            )
        )
        val faqArray = resources.getStringArray(R.array.practise_partner_faq)
        faqArray.forEach {
            binding.recyclerView.addView(VideoViewHolder(it))
        }
    }

    fun startPractise() {
        if (binding.checkBox.isChecked.not()) {
            return
        }
        PrefManager.put(IS_PRACTISE_PARTNER_VIEWED, true)
        dismissAllowingStateLoss()
        listener?.onSuccessDismiss()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (PrefManager.hasKey(IS_PRACTISE_PARTNER_VIEWED).not()) {
            listener?.onDismiss()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = PractisePartnerDialogFragment()
    }

}


@Layout(R.layout.pp_faq_item_layout)
class VideoViewHolder(var title: String) {
    @com.mindorks.placeholderview.annotations.View(R.id.text_view)
    lateinit var textView: MaterialTextView

    @Resolve
    fun onViewInflated() {
        textView.text = title
    }
}