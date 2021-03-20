package com.joshtalks.joshskills.ui.introduction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.IntroFragmentLayout1Binding

class PageFragment : Fragment() {
    private lateinit var binding: IntroFragmentLayout1Binding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.intro_fragment_layout_1, container, false)
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.text.text = IntroductionActivity.Content.values().get(requireArguments().getInt(PAGE_POSITION)).text
        binding.image.setImageResource(IntroductionActivity.Content.values().get(requireArguments().getInt(PAGE_POSITION)).drawable)
    }

    companion object {
        private const val PAGE_POSITION = "page_position"

        @JvmStatic
        fun newInstance(position: Int): PageFragment {
            val args = Bundle()
            args.putInt(PAGE_POSITION, position)
            val f = PageFragment()
            f.arguments = args
            return f
        }
    }

}
