package com.joshtalks.joshskills.ui.conversation_practice.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.repository.server.conversation_practice.ConversationPractiseModel

class SelfPractiseFragment private constructor() : Fragment() {
    private lateinit var conversationPractiseModel: ConversationPractiseModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            conversationPractiseModel =
                it.getParcelable<ConversationPractiseModel>(ARG_PRACTISE_OBJ) as ConversationPractiseModel
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.self_practise_layout, container, false)
    }

    companion object {
        private const val ARG_PRACTISE_OBJ = "practise-obj"

        @JvmStatic
        fun newInstance(conversationPractiseModel: ConversationPractiseModel) =
            SelfPractiseFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_PRACTISE_OBJ, conversationPractiseModel)
                }
            }
    }

}