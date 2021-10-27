package com.joshtalks.joshskills.ui.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.constants.CLEAR_SEARCH
import com.joshtalks.joshskills.databinding.FragmentGroupSearchBinding
import com.joshtalks.joshskills.ui.group.viewmodels.GroupSearchViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

class GroupSearchFragment : BaseFragment() {
    lateinit var binding : FragmentGroupSearchBinding
    @FlowPreview
    val vm by lazy {
        ViewModelProvider(this)[GroupSearchViewModel::class.java]
    }

    @FlowPreview
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launchWhenStarted {
            vm.groupLiveData.distinctUntilChanged().collectLatest {
                vm.adapter.submitData(it)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_group_search, container, false)
        return binding.root
    }

    override fun initViewBinding() {
        binding.vm = vm
        binding.executePendingBindings()
    }
    // TODO: Might Changes Flow Signature
    @FlowPreview
    override fun initViewState() {
        liveData.observe(viewLifecycleOwner) {
            when(it.what) {
                CLEAR_SEARCH -> {
                    binding.searchView.setText("")
                }
            }
        }
    }

    @FlowPreview
    override fun onStart() {
        super.onStart()
        vm.adapter.refresh()
    }


    override fun setArguments() {
        arguments.let {
            vm.isFromVoip.set(it?.getBoolean(IS_FROM_VOIP, false) ?: false)
        }
    }
}