package com.joshtalks.joshskills.ui.group

import android.os.Bundle
import android.transition.ChangeBounds
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.constants.OPEN_POPUP_MENU
import com.joshtalks.joshskills.databinding.FragmentGroupListBinding
import com.joshtalks.joshskills.ui.group.viewmodels.JoshGroupViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

private const val TAG = "GroupListFragment"
class GroupListFragment : BaseFragment() {
    lateinit var binding: FragmentGroupListBinding
    val vm by lazy {
        ViewModelProvider(requireActivity())[JoshGroupViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launchWhenStarted {
            vm.getGroupData().distinctUntilChanged().collectLatest {
                Log.d(TAG, "onCreate: $it")
                vm.adapter.submitData(it)
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

    override fun initViewBinding() {
        binding.vm = vm
        binding.executePendingBindings()
    }

    override fun initViewState() {
        liveData.observe(viewLifecycleOwner) {
            when(it.what) {
                OPEN_POPUP_MENU -> {
                    openPopupMenu()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        vm.adapter.refresh()
    }

    override fun setArguments() {}

    private fun openPopupMenu() {
        val popupMenu = PopupMenu(requireContext(), binding.groupAppBar.secondIconImageView, R.style.setting_menu_style)
        popupMenu.inflate(R.menu.groups_menu)
        popupMenu.setOnMenuItemClickListener {
            popupMenu.dismiss()
            when (it.itemId) {
                R.id.new_group -> {
                    vm.openNewGroup()
                }
            }
            return@setOnMenuItemClickListener false
        }
        popupMenu.show()
    }
}