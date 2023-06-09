package com.joshtalks.joshskills.ui.group

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider

import com.bumptech.glide.Glide

import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.constants.CREATE_GROUP_VALIDATION
import com.joshtalks.joshskills.constants.GROUP_IMAGE_SELECTED
import com.joshtalks.joshskills.constants.SAVE_GROUP_INFO
import com.joshtalks.joshskills.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.core.analytics.ParamKeys
import com.joshtalks.joshskills.core.isValidContextForGlide
import com.joshtalks.joshskills.databinding.FragmentNewGroupBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.group.constants.*
import com.joshtalks.joshskills.ui.group.model.AddGroupRequest
import com.joshtalks.joshskills.ui.group.model.EditGroupRequest
import com.joshtalks.joshskills.ui.group.viewmodels.JoshGroupViewModel

import java.io.File

class NewGroupFragment : BaseFragment() {
    lateinit var binding: FragmentNewGroupBinding
    var groupId: String? = null
    var imagePath: String? = null
    val vm by lazy {
        ViewModelProvider(requireActivity())[JoshGroupViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(layoutInflater, R.layout.fragment_new_group, container, false)
        return binding.root
    }

    override fun initViewBinding() {
        binding.vm = vm
        binding.executePendingBindings()
    }

    override fun initViewState() {
        liveData.observe(viewLifecycleOwner) {
            when (it.what) {
                GROUP_IMAGE_SELECTED -> {
                    imagePath = it.obj as? String
                    vm.isImageChanged = true
                    imagePath?.let {
                        if (isValidContextForGlide(this.requireContext())){
                            Glide.with(this)
                                .load(Uri.fromFile(File(imagePath)))
                                .into(binding.imgGroup)
                        }
                    }
                }
                CREATE_GROUP_VALIDATION -> {
                    val groupName = binding.etGroupName.text.toString().trim()
                    var groupType = OPENED_GROUP
                    if (binding.tvSelectGroupType.text.toString().contains(CLOSED_GROUP, true))
                        groupType = CLOSED_GROUP

                    if (groupName.isNotBlank() && groupName.length <= 25 && groupType.isNotEmpty()) {
                        val request = AddGroupRequest(
                            mentorId = Mentor.getInstance().getId(),
                            groupName = groupName,
                            groupIcon = imagePath ?: "",
                            groupType = groupType
                        )
                        vm.openAdminResponsibility(request)
                    } else if (groupName.length > 25)
                        showToast("Group Name should be 25 character or less")
                    else if (groupType.isBlank())
                        showToast("Please select the group type")
                    else
                        showToast("Please enter group name")
                }
                SAVE_GROUP_INFO -> {
                    val groupName = binding.etGroupName.text.toString().trim()
                    if (vm.isImageChanged || (groupName != vm.groupTitle.get() && groupName.isNotEmpty() && groupName.length <= 25)) {
                        val request = EditGroupRequest(
                            groupId = groupId ?: "",
                            groupName = groupName,
                            groupIcon = imagePath ?: vm.groupImageUrl.get() ?: "",
                            isImageChanged = vm.isImageChanged,
                            groupType = vm.groupType.get()!!
                        )
                        vm.editGroup(request, groupName != vm.groupTitle.get())
                    } else if (groupName.length > 25)
                        showToast("Group Name should be 25 character or less")
                    else if (groupName == vm.groupTitle.get() && !vm.isImageChanged)
                        showToast("Change group information before saving")
                    else
                        showToast("Please enter group name")
                }
            }
        }
    }

    override fun getConversationId(): String? {
        return if (vm.conversationId.isBlank()) null else vm.conversationId
    }

    override fun setArguments() {
        arguments?.let {
            vm.isFromGroupInfo.set(it.getBoolean(IS_FROM_GROUP_INFO, false))
            vm.groupTitle.set(it.getString(GROUPS_TITLE))
            vm.groupImageUrl.set(it.getString(GROUPS_IMAGE))
            vm.groupType.set(it.getString(GROUP_TYPE, ""))
            groupId = it.getString(GROUPS_ID)
        }
        vm.isImageChanged = false
    }
}