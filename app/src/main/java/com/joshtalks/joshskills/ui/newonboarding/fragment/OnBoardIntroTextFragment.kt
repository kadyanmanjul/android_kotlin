package com.joshtalks.joshskills.ui.newonboarding.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.FragmentOnBoardIntroTextBinding
import com.joshtalks.joshskills.repository.server.onboarding.Content

const val CONTENT_TEXT = "content_obj_text"
const val CONTENT_DESC = "content_obj_desc"

class OnBoardIntroTextFragment : Fragment() {

    private lateinit var binding: FragmentOnBoardIntroTextBinding
    companion object {
        fun newInstance(text: String?, desc: String?) = OnBoardIntroTextFragment().apply {
            arguments = Bundle().apply {
                putString(CONTENT_TEXT, text)
                putString(CONTENT_DESC, desc)
            }
        }
    }

    private lateinit var content: Content

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val desc = it.getString(CONTENT_DESC)
            val text = it.getString(CONTENT_TEXT)
            if (desc.isNullOrBlank()) {
                requireActivity().supportFragmentManager.beginTransaction().remove(this).commit()
            }
            content = Content(text, desc)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_on_board_intro_text,
                container,
                false
            )
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.title.text = content.text
        binding.description.text = content.description
    }

}
