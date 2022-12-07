package com.joshtalks.joshskills.ui.redirect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.setPadding
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.FragmentRedirectBinding
import com.joshtalks.joshskills.deeplink.DeepLinkData
import com.joshtalks.joshskills.deeplink.DeepLinkRedirect
import com.joshtalks.joshskills.deeplink.DeepLinkRedirectUtil
import com.joshtalks.joshskills.ui.BottomAlertDialog
import kotlinx.coroutines.launch
import org.json.JSONObject

class RedirectFragment : Fragment() {

    private lateinit var binding: FragmentRedirectBinding
    private val adapter = RedirectAdapter()

    private val activityMap: Map<DeepLinkRedirect, List<DeepLinkData>> = mapOf(
        DeepLinkRedirect.CONVERSATION_ACTIVITY to listOf(DeepLinkData.COURSE_ID),
        DeepLinkRedirect.GROUP_ACTIVITY to listOf(DeepLinkData.COURSE_ID),
        DeepLinkRedirect.P2P_ACTIVITY to listOf(DeepLinkData.COURSE_ID, DeepLinkData.TOPIC_ID),
        DeepLinkRedirect.FPP_ACTIVITY to listOf(DeepLinkData.COURSE_ID),
        DeepLinkRedirect.COURSE_DETAILS to listOf(DeepLinkData.TEST_ID),
        DeepLinkRedirect.LESSON_ACTIVITY to listOf(DeepLinkData.LESSON_ID, DeepLinkData.COURSE_ID),
        DeepLinkRedirect.CUSTOMER_SUPPORT_ACTIVITY to listOf(),
        DeepLinkRedirect.CUSTOMER_SUPPORT_ACTIVITY to listOf(),
        DeepLinkRedirect.BUY_PAGE_ACTIVITY to listOf(DeepLinkData.COUPON_CODE),
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_redirect, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.recyclerView.adapter = adapter
        adapter.submitList(activityMap.keys.map { it.name })
        adapter.setOnClickListener { activityName, position ->
            val dialog = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_text, null)
            //remove all views from dialog
            (dialog as ViewGroup).removeAllViews()
            val params = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            dialog.layoutParams = params
            val deepLinkDataList: List<DeepLinkData>? = activityMap[DeepLinkRedirect.valueOf(activityName)]
            val jsonObject = JSONObject()
            jsonObject.put(DeepLinkData.REDIRECT_TO.key, DeepLinkRedirect.valueOf(activityName).key)
            if (deepLinkDataList?.isEmpty() == true) {
                lifecycleScope.launch {
                    DeepLinkRedirectUtil.getIntent(requireActivity(), jsonObject)
                }
                return@setOnClickListener
            }
            if (deepLinkDataList != null) {
                for (i in deepLinkDataList) {
                    val textInputLayout = TextInputLayout(requireContext())
                    textInputLayout.setPadding(16)
                    textInputLayout.layoutParams = params
                    textInputLayout.hint = i.key.replace("_", " ").replaceFirstChar { it.uppercase() }
                    val textInputEditText = TextInputEditText(requireContext())
                    textInputEditText.id = i.key.hashCode()
                    textInputLayout.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                    textInputLayout.addView(textInputEditText)
                    dialog.addView(textInputLayout)
                }
            }
            BottomAlertDialog()
                .setTitle("Open $activityName")
                .setMessage("Enter the data for the activity")
                .setCustomView(dialog)
                .setPositiveButton("Open") { d ->
                    if (deepLinkDataList != null) {
                        for (i in deepLinkDataList) {
                            jsonObject.put(
                                i.key,
                                dialog.findViewById<TextInputEditText>(i.key.hashCode()).text.toString()
                            )
                        }
                    }
                    lifecycleScope.launch {
                        if (DeepLinkRedirectUtil.getIntent(requireActivity(), jsonObject))
                            Toast.makeText(requireContext(), "Opening activity", Toast.LENGTH_SHORT).show()
                        else
                            Toast.makeText(requireContext(), "Invalid Data", Toast.LENGTH_SHORT).show()
                    }
                    d.dismiss()
                }
                .setNegativeButton("Cancel") { d ->
                    d.dismiss()
                }
                .show(childFragmentManager)
        }
    }


}