package com.joshtalks.joshskills.ui.group

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.ui.group.viewmodels.GroupInfoViewModel

class GroupInfoFragment : Fragment() {

    companion object {
        fun newInstance() = GroupInfoFragment()
    }

    private lateinit var viewModel: GroupInfoViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.group_info_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(GroupInfoViewModel::class.java)
        // TODO: Use the ViewModel
    }
}