package com.joshtalks.joshskills.ui.apirequest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.FragmentApiRequestBinding
import com.joshtalks.joshskills.databinding.ItemApiRequestBinding
import com.joshtalks.joshskills.repository.entity.ApiRequest
import com.joshtalks.joshskills.ui.BottomAlertDialog
import com.joshtalks.joshskills.ui.DebugViewModel

class ApiRequestFragment : Fragment() {

    private lateinit var binding: FragmentApiRequestBinding
    private val viewModel by lazy {
        ViewModelProvider(this)[DebugViewModel::class.java]
    }
    private val adapter = ApiRequestAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_api_request, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.initUI()
        addObserver()
    }

    private fun FragmentApiRequestBinding.initUI() {
        recyclerView.adapter = adapter
        adapter.setOnItemClickListener { apiRequest ->
            findNavController().navigate(
                ApiRequestFragmentDirections.actionApiRequestFragmentToViewApiRequestFragment(
                    apiRequest
                )
            )
        }
        btnClearRequests.setOnClickListener {
            BottomAlertDialog()
                .setTitle("Clear all requests")
                .setMessage("Are you sure you want to clear all requests?")
                .setPositiveButton("Yes") { d ->
                    viewModel.deleteAllApiRequests()
                    d.dismiss()
                }
                .setNegativeButton("No") { d ->
                    d.dismiss()
                }
                .show(childFragmentManager)
        }
    }

    private fun addObserver() {
        viewModel.getApiRequests()?.observe(viewLifecycleOwner) {
            if (it.isEmpty())
                binding.noResultsStub.viewStub?.inflate()
            adapter.submitList(it)
        }
    }
}

class ApiRequestAdapter : ListAdapter<ApiRequest, ApiRequestAdapter.ApiRequestViewHolder>(object :
    DiffUtil.ItemCallback<ApiRequest>() {
    override fun areItemsTheSame(oldItem: ApiRequest, newItem: ApiRequest): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: ApiRequest, newItem: ApiRequest): Boolean =
        oldItem == newItem
}) {

    private var onItemClickListener: ((ApiRequest) -> Unit)? = null

    inner class ApiRequestViewHolder(private val binding: ItemApiRequestBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(apiRequest: ApiRequest) {
            binding.apply {
                root.setOnClickListener { onItemClickListener?.invoke(apiRequest) }
                tvStatusCode.text = apiRequest.status.toString()
                tvEndpoint.text = apiRequest.url
                tvMethod.text = apiRequest.method
                tvMethod.setTextColor(
                    ContextCompat.getColor(
                        root.context,
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
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApiRequestViewHolder =
        ApiRequestViewHolder(
            ItemApiRequestBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: ApiRequestViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun setOnItemClickListener(function: (ApiRequest) -> Unit) {
        this.onItemClickListener = function
    }
}