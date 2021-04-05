package com.joshtalks.joshskills.ui.signup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.FragmentSignUpProfilePicUpdateBinding
import com.joshtalks.joshskills.ui.userprofile.UserPicChooserFragment

class SignUpProfilePicUpdateFragment : BaseSignUpFragment() {

    private lateinit var binding: FragmentSignUpProfilePicUpdateBinding

    companion object {
        const val TAG = "SignUpProfilePicUpdateFragment"
        fun newInstance() = SignUpProfilePicUpdateFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_sign_up_profile_pic_update, container, false)
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    fun submitProfilePic() {
        //val requestMap = mutableMapOf<String, String?>()
        //viewModel.completingProfile(requestMap)
        UserPicChooserFragment.showDialog(
            childFragmentManager,
            true
        )
    }

}
