package com.joshtalks.joshskills.ui.group

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.constants.ADD_GROUP_TO_SERVER
import com.joshtalks.joshskills.constants.GROUP_IMAGE_SELECTED
import com.joshtalks.joshskills.databinding.FragmentNewGroupBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.group.model.AddGroupRequest
import com.joshtalks.joshskills.ui.group.viewmodels.JoshGroupViewModel
import com.joshtalks.joshskills.ui.userprofile.UserPicChooserFragment
import java.io.File

class NewGroupFragment : BaseFragment() {
    lateinit var binding  : FragmentNewGroupBinding
    var imagePath : String? = null
    val vm by lazy {
        ViewModelProvider(requireActivity())[JoshGroupViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.fragment_new_group, container, false)
        return binding.root
    }

    override fun initViewBinding() {
        binding.vm = vm
        binding.executePendingBindings()
    }

    override fun initViewState() {
        liveData.observe(viewLifecycleOwner) {
            when(it.what) {
                GROUP_IMAGE_SELECTED -> {
                    imagePath = it.obj as? String
                    imagePath?.let {
                        Glide.with(this)
                            .load(Uri.fromFile(File(imagePath)))
                            .into(binding.imgGroup)
                    }
                }
                ADD_GROUP_TO_SERVER -> {
                    val groupName = binding.etGroupName.text.toString()
                    if (groupName.isNotEmpty()) {
                        val request = AddGroupRequest(
                            mentorId = Mentor.getInstance().getId(),
                            groupName =groupName,
                            groupIcon = imagePath ?: ""
                        )
                        vm.addGroup(request)
                    } else
                        showToast("Please enter group name")
                }
            }
        }
    }

    override fun setArguments() {}
}