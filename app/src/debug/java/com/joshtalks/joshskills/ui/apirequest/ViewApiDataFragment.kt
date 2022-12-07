package com.joshtalks.joshskills.ui.apirequest

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.FragmentViewApiDataBinding
import com.joshtalks.joshskills.repository.entity.ApiRequest

class ViewApiDataFragment : Fragment() {

    private lateinit var binding: FragmentViewApiDataBinding
    private lateinit var apiRequest: ApiRequest
    private var tabPosition: Int = 0

    companion object {
        private const val TAB_POSITION = "tab_position"
        private const val API_DATA = "api_data"

        fun newInstance(position: Int, apiRequest: ApiRequest): ViewApiDataFragment {
            val args = Bundle()
            args.putInt(TAB_POSITION, position)
            args.putParcelable(API_DATA, apiRequest)
            val fragment = ViewApiDataFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_view_api_data, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tabPosition = arguments?.getInt(TAB_POSITION) ?: 0
        apiRequest = if (Build.VERSION.SDK_INT >= 33) {
            arguments?.getParcelable(API_DATA, ApiRequest::class.java) ?: return
        } else {
            arguments?.getParcelable(API_DATA) ?: return
        }
        binding.textView.text = when (tabPosition) {
            0 -> apiRequest.headers
            1 -> apiRequest.request ?: "No request body found!"
            2 -> apiRequest.response ?: "No response body found!"
            else -> ""
        }
    }
}