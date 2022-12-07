package com.joshtalks.joshskills.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.databinding.DialogBottomBinding

class BottomAlertDialog : BottomSheetDialogFragment() {
    private lateinit var binding: DialogBottomBinding
    private var title: String? = null
    private var message: String? = null
    private var positiveButtonText: String? = null
    private var negativeButtonText: String? = null
    private var positiveButtonClickListener: ((BottomAlertDialog) -> Unit)? = null
    private var negativeButtonClickListener: ((BottomAlertDialog) -> Unit)? = null
    private var customView: View? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DialogBottomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            title?.let {
                tvTitle.text = it
                tvTitle.visibility = View.VISIBLE
            }
            message?.let {
                tvMessage.text = it
                tvMessage.visibility = View.VISIBLE
            }
            positiveButtonText?.let {
                btnPositive.text = it
                btnPositive.visibility = View.VISIBLE
                btnPositive.setOnClickListener {
                    positiveButtonClickListener?.invoke(this@BottomAlertDialog)
                    dismiss()
                }
            }
            negativeButtonText?.let {
                btnNegative.text = it
                btnNegative.visibility = View.VISIBLE
                btnNegative.setOnClickListener {
                    negativeButtonClickListener?.invoke(this@BottomAlertDialog)
                    dismiss()
                }
            }
            customView?.let {
                containerCustomView.addView(it)
                containerCustomView.visibility = View.VISIBLE
            }
        }
    }

    fun setPositiveButton(text: String, listener: ((BottomAlertDialog) -> Unit)? = null): BottomAlertDialog {
        positiveButtonText = text
        positiveButtonClickListener = listener
        return this
    }

    fun setNegativeButton(text: String, listener: ((BottomAlertDialog) -> Unit)? = null): BottomAlertDialog {
        negativeButtonText = text
        negativeButtonClickListener = listener
        return this
    }

    fun setTitle(title: String): BottomAlertDialog {
        this.title = title
        return this
    }

    fun setMessage(message: String): BottomAlertDialog {
        this.message = message
        return this

    }

    fun setCustomView(view: View): BottomAlertDialog {
        customView = view
        return this
    }

    fun show(manager: FragmentManager) {
        show(manager, null)
    }

    override fun show(manager: FragmentManager, tag: String?) {
        super.show(manager, tag)
    }
}