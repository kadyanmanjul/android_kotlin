package com.joshtalks.joshskills.ui.userprofile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.databinding.FragmentAwardShowBinding
import com.joshtalks.joshskills.repository.server.Award

class ShowAwardFragment : Fragment() {

    private lateinit var binding: FragmentAwardShowBinding
    private lateinit var award: Award

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            award = it.getParcelable(AWARD_DETAILS)!!
        }
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
        award.imageUrl?.let {
            binding.image.setImage(it)
        }
        binding.text.text = award.awardDescription
    }

    fun dismiss() {
        requireActivity().supportFragmentManager.beginTransaction().remove(this).commit()
    }

    companion object {
        const val AWARD_DETAILS = "award_details"

        @JvmStatic
        fun newInstance(award: Award) =
            ShowAwardFragment()
                .apply {
                    arguments = Bundle().apply {
                        putParcelable(AWARD_DETAILS, award)
                    }
                }
    }

}
