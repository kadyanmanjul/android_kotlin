package com.joshtalks.joshskills.ui.error

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.databinding.FragmentOfflineConnectionErrorBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.ConnectionErrorRetryEvent

class ConnectionErrorDialogFragment : DialogFragment() {
    private lateinit var binding: FragmentOfflineConnectionErrorBinding
    private var titleText: String = EMPTY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
        setStyle(STYLE_NO_FRAME, R.style.full_dialog)
        arguments?.let {
            titleText = it.getString(TITLE_ERROR, getString(R.string.connection_error))
        }
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.window?.setLayout(width, height)
            dialog.setCanceledOnTouchOutside(true)
        }
    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_offline_connection_error,
            container,
            false
        )
        binding.lifecycleOwner = this
        binding.handler = this
        binding.title.text = titleText
        return binding.root
    }

    fun retry() {
            RxBus2.publish(ConnectionErrorRetryEvent(titleText))
            dismiss()
    }

    companion object {
        const val TITLE_ERROR = "TITLE_ERROR"
        const val TAG = "ConnectionErrorDialogFragment"

        @JvmStatic
        fun newInstance(transactionId: String?) =
            ConnectionErrorDialogFragment().apply {
                arguments = Bundle().apply {
                    transactionId?.let {
                        putString(TITLE_ERROR, transactionId)
                    }
                }
            }
    }
}
