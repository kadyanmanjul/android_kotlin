package com.joshtalks.joshskills.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.DEBUG_BASE_URL
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.USER_UNIQUE_ID
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.FragmentDeviceInfoBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.ui.BottomAlertDialog
import com.joshtalks.joshskills.ui.DebugActivity
import com.joshtalks.joshskills.util.showAppropriateMsg

class DeviceInfoFragment : Fragment() {
    private lateinit var binding: FragmentDeviceInfoBinding
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_device_info, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        try {
            binding.initUI()
        } catch (e: Exception) {
            e.showAppropriateMsg()
            e.printStackTrace()
        }
    }

    private fun FragmentDeviceInfoBinding.initUI() {
        tvVersionCode.text = "Version Code : " + BuildConfig.VERSION_CODE.toString()
        tvVersionName.text = "Version Name : " + BuildConfig.VERSION_NAME
        etGaid.setText(PrefManager.getStringValue(USER_UNIQUE_ID).ifEmpty { "-" })
        etMentor.setText(Mentor.getInstance().getId().ifEmpty { "-" })
        etBaseUrl.setText(PrefManager.getStringValue(DEBUG_BASE_URL, defaultValue = BuildConfig.BASE_URL))
        etUser.setText(User.getInstance().userId.ifEmpty { "-" })
        etPhone.setText(User.getInstance().phoneNumber?.ifEmpty { "-" } ?: "-")
        layoutGaid.setEndIconOnClickListener {
            if (etGaid.text?.isNotBlank() == true && etGaid.text?.toString() != "-") {
                copyTextToClipboardAndShowToast(
                    title = "GAID",
                    text = etGaid.text.toString(),
                    message = "Google Advertising ID copied successfully!",
                )
            }
        }
        layoutMentor.setEndIconOnClickListener {
            if (etMentor.text?.isNotBlank() == true && etMentor.text?.toString() != "-")
                copyTextToClipboardAndShowToast(
                    title = "Mentor ID",
                    text = etMentor.text.toString(),
                    message = "Mentor ID copied successfully",
                )
        }
        layoutUser.setEndIconOnClickListener {
            if (etUser.text?.isNotBlank() == true && etUser.text?.toString() != "-")
                copyTextToClipboardAndShowToast(
                    title = "User Id",
                    text = etUser.text.toString(),
                    message = "User ID copied successfully",
                )
            else
                showToast("Nothing to copy!")
        }
        layoutPhone.setEndIconOnClickListener {
            if (etPhone.text?.isNotBlank() == true && etPhone.text?.toString() != "-")
                copyTextToClipboardAndShowToast(
                    title = "Phone Number",
                    text = etPhone.text.toString(),
                    message = "Phone  Number copied successfully",
                )
            else
                showToast("Nothing to copy!")
        }
        layoutBaseUrl.setEndIconOnClickListener {
            val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_text, null)
            val editText = view.findViewById<TextInputEditText>(R.id.et_base_url)
            editText?.setText(PrefManager.getStringValue(DEBUG_BASE_URL, defaultValue = BuildConfig.BASE_URL))
            BottomAlertDialog()
                .setTitle("Enter Base URL")
                .setCustomView(view)
                .setPositiveButton("OK") { _ ->
                    editText?.let {
                        if (URLUtil.isValidUrl(it.text.toString())) {
                            PrefManager.put(DEBUG_BASE_URL, it.text.toString())
                            etBaseUrl.setText(it.text.toString())
                        } else {
                            showToast("Invalid URL!")
                            PrefManager.removeKey(DEBUG_BASE_URL)
                            etBaseUrl.setText(BuildConfig.BASE_URL)
                        }
                    }
                }
                .setNegativeButton("Cancel") { dialog -> dialog.dismiss() }
                .show(childFragmentManager)
        }
        deleteGaid.setOnClickListener { (requireActivity() as DebugActivity).deleteGaid() }
    }

    private fun copyTextToClipboardAndShowToast(title: String, text: String, message: String) {
        val clipboardManager =
            requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText(
            title,
            text
        )
        clipboardManager.setPrimaryClip(clipData)
        showToast(message)
    }
}