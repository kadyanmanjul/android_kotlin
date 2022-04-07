package com.joshtalks.joshskills.ui.userprofile

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.constants.INVITE_FRIENDS_METHOD
import com.joshtalks.joshskills.databinding.FragmentShareFromProfileBinding

class ShareFromProfileFragment : BaseFragment() {
    lateinit var binding: FragmentShareFromProfileBinding
    val vm by lazy {
        ViewModelProvider(requireActivity())[UserProfileViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_share_from_profile,
            container,
            false
        )
        return binding.root
    }

    override fun initViewBinding() {
        binding.let {
            binding.vm = vm
            binding.executePendingBindings()
        }
    }

    override fun initViewState() {
        liveData.observe(viewLifecycleOwner) {
            when (it.what) {
                INVITE_FRIENDS_METHOD -> inviteFriends(it.obj as Intent)
            }
        }
    }

    override fun setArguments() {
        arguments?.let { it ->
            it.getInt(REFERRAL_COUNT)?.let { path ->
                vm.count.set(path)
            }
        }
    }

    fun inviteFriends(waIntent: Intent){
        try {
            startActivity(Intent.createChooser(waIntent, "Share with"))
        } catch (e : PackageManager.NameNotFoundException){
            showToast(getString(R.string.whatsApp_not_installed))
        }
    }
}