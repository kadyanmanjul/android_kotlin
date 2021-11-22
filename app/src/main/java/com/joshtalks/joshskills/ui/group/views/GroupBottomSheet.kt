package com.joshtalks.joshskills.ui.group.views

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.map
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.constants.ON_BACK_PRESSED
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.FragmentGroupListBinding
import com.joshtalks.joshskills.ui.group.viewmodels.JoshGroupViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import com.google.android.material.bottomsheet.BottomSheetBehavior

import com.google.android.material.bottomsheet.BottomSheetDialog
import com.joshtalks.joshskills.ui.group.model.GroupItemData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class GroupBottomSheet : BottomSheetDialogFragment() {
    lateinit var binding: FragmentGroupListBinding
    val events = EventLiveData
    val vm by lazy {
        ViewModelProvider(this)[JoshGroupViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launchWhenStarted {
            vm.getGroupData().distinctUntilChanged().collectLatest {
                withContext(Dispatchers.IO) {
                    val groupList = it.map { data -> data as GroupItemData }
                    withContext(Dispatchers.Main) {
                        vm.adapter.submitData(groupList)
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_group_list, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.vm = vm
        binding.executePendingBindings()
        events.observe(viewLifecycleOwner) {
            when(it.what) {
                ON_BACK_PRESSED -> {
                    showToast("Group BottomSheet")
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = super.onCreateDialog(savedInstanceState)
        view.setOnShowListener {
            val dialog = it as BottomSheetDialog
            val bottomSheet: FrameLayout = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet)!!
            BottomSheetBehavior.from(bottomSheet).state =
                BottomSheetBehavior.STATE_EXPANDED
            BottomSheetBehavior.from(bottomSheet).skipCollapsed = true
            BottomSheetBehavior.from(bottomSheet).setHideable(true)
        }
        return view
    }
}