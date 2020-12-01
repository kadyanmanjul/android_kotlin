package com.joshtalks.joshskills.ui.userprofile

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.text.HtmlCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.databinding.FragmentAwardShowBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.Award

class ShowAwardFragment : DialogFragment() {

    private lateinit var binding: FragmentAwardShowBinding
    private var award: ArrayList<Award>? = null
    private var position: Int = 0
    private var isFromUserProfile: Boolean = false
    private val viewModel by lazy { ViewModelProvider(requireActivity()).get(AwardViewModel::class.java) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.full_dialog)
        changeDialogConfiguration()
        arguments?.let {
            award = it.getParcelableArrayList(AWARD_DETAILS)
            isFromUserProfile = it.getBoolean(IS_FROM_USER_PROFILE)
        }
        if (award == null) {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.run {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            window?.setLayout(width, height)
        }
    }

    private fun changeDialogConfiguration() {
        val params: WindowManager.LayoutParams? = dialog?.window?.attributes
        params?.width = WindowManager.LayoutParams.MATCH_PARENT
        params?.height = WindowManager.LayoutParams.MATCH_PARENT
        params?.gravity = Gravity.CENTER
        dialog?.window?.attributes = params
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    fun nextAward() {
        if (position >= award?.size!!) {
            return
        } else {
            position = position.plus(1)
            showNewAward(position)
            if (position == award?.size!!) {
                binding.next.visibility = View.GONE
            } else {
                binding.next.visibility = View.VISIBLE
                binding.previous.visibility = View.VISIBLE
            }
        }
    }

    fun prevAward() {
        if (position <= 0) {
            return
        } else {
            position = position.minus(1)
            showNewAward(position)
            if (position == 0) {
                binding.previous.visibility = View.GONE
            } else {
                binding.previous.visibility = View.VISIBLE
                binding.next.visibility = View.VISIBLE
            }
        }
    }

    fun showNewAward(position: Int) {
        if (position >= 0 && position < award?.size!!) {
            award?.get(position)?.imageUrl?.let {
                binding.image.setImage(it)
            }
            binding.text.text = HtmlCompat.fromHtml(
                award?.get(position)?.awardDescription.toString(),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        }
    }

    fun goToProfile() {
        UserProfileActivity.startUserProfileActivity(
            requireActivity(), Mentor.getInstance().getId(),
            arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        )
        dismiss()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_award_show, container, false)
        binding.lifecycleOwner = this
        binding.fragment = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.close.visibility = View.VISIBLE
        if (award?.size!! < 2) {
            binding.btnProfile.visibility = View.GONE
            binding.next.visibility = View.GONE
            binding.previous.visibility = View.GONE
        } else {
            binding.btnProfile.visibility = View.VISIBLE
            binding.next.visibility = View.VISIBLE
            binding.previous.visibility = View.GONE
        }
        award?.get(0)?.imageUrl?.let {
            binding.image.setImage(it)
        }
        binding.text.text = HtmlCompat.fromHtml(
            award?.get(0)?.awardDescription.toString(),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )

    }

    override fun onDismiss(dialog: DialogInterface) {
        if (isFromUserProfile.not()) {
            val list = ArrayList<Int>()
            award?.filter { it.isSeen == false }?.map {
                list.add(it.id!!)
            }
            if (list.isNullOrEmpty().not())
                viewModel.patchAwardDetails(list)
        }
        super.onDismiss(dialog)
    }

    companion object {
        const val TAG = "ShowAwardFragment"
        const val AWARD_DETAILS = "award_details"
        const val IS_FROM_USER_PROFILE = "is_from_user_profile"
        fun newInstance(award: List<Award>, isFromUserProfile: Boolean = false) =
            ShowAwardFragment()
                .apply {
                    arguments = Bundle().apply {
                        putParcelableArrayList(AWARD_DETAILS, ArrayList(award))
                        putBoolean(IS_FROM_USER_PROFILE, isFromUserProfile)
                    }
                }

        fun showDialog(
            supportFragmentManager: FragmentManager,
            awardList: List<Award>,
            isFromUserProfile: Boolean = false
        ) {
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            val prev = supportFragmentManager.findFragmentByTag(TAG)
            if (prev != null) {
                fragmentTransaction.remove(prev)
            }
            fragmentTransaction.addToBackStack(null)
            newInstance(awardList, isFromUserProfile)
                .show(supportFragmentManager, TAG)
        }
    }

}
