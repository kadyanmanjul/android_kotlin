package com.joshtalks.joshskills.ui.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.ViewModelProvider

import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.databinding.GroupInfoFragmentBinding
import com.joshtalks.joshskills.ui.group.viewmodels.GroupInfoViewModel

private const val TAG = "GroupInfoFragment"

class GroupInfoFragment : BaseFragment() {
    lateinit var binding: GroupInfoFragmentBinding

    val vm by lazy {
        ViewModelProvider(this)[GroupInfoViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.group_info_fragment, container, false)
    }

    override fun initViewBinding() {
        TODO("Not yet implemented")
    }

    override fun initViewState() {
        TODO("Not yet implemented")
    }

    override fun setArguments() {
        TODO("Not yet implemented")
    }

}