package com.joshtalks.joshskills.ui.apirequest

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.datetimeutils.DateTimeUtils
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.FragmentViewApiRequestBinding
import com.joshtalks.joshskills.repository.entity.ApiRequest
import com.joshtalks.joshskills.util.showAppropriateMsg
import java.text.SimpleDateFormat

class ViewApiRequestFragment : Fragment() {
    private lateinit var binding: FragmentViewApiRequestBinding
    private lateinit var apiRequest: ApiRequest
    private val args: ViewApiRequestFragmentArgs by navArgs<ViewApiRequestFragmentArgs>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_view_api_request, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        apiRequest = args.apiRequest
        try {
            binding.initUI()
        } catch (e: Exception) {
            e.showAppropriateMsg()
            e.printStackTrace()
        }
    }

    private fun FragmentViewApiRequestBinding.initUI() {
        binding.data = apiRequest
        btnCopyCurl.setOnClickListener {
            val clipboardManager =
                AppObjectController.joshApplication.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText(
                "CURL Request",
                apiRequest.curl
            )
            clipboardManager.setPrimaryClip(clipData)
            showToast(requireContext().getString(R.string.copied_to_clipboard))
        }
        tvDuration.text = apiRequest.duration.toString().plus(" ms")
        tvTime.text = SimpleDateFormat("hh:mm:ss a").format(DateTimeUtils.formatDate(apiRequest.time))
        tvMethod.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                when (apiRequest.method) {
                    "GET" -> R.color.success
                    "POST" -> R.color.decorative_one
                    "PATCH" -> R.color.quantum_orange
                    "DELETE" -> R.color.critical
                    else -> R.color.quantum_lime500
                }
            )
        )
        tvStatusCode.setTextColor(
            ContextCompat.getColor(
                root.context,
                when (apiRequest.status) {
                    in 200..299 -> R.color.success
                    in 400..499 -> R.color.quantum_orange
                    else -> R.color.critical
                }
            )
        )
        viewPager.adapter = ViewApiRequestPagerAdapter(childFragmentManager, lifecycle)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Headers"
                1 -> "Request"
                2 -> "Response"
                else -> "Unknown"
            }
        }.attach()
    }

    inner class ViewApiRequestPagerAdapter(
        fragmentManager: FragmentManager,
        lifecycle: Lifecycle
    ) : FragmentStateAdapter(fragmentManager, lifecycle) {

        override fun getItemCount() = 3
        override fun createFragment(position: Int) = ViewApiDataFragment.newInstance(position, apiRequest)
    }
}