package com.joshtalks.joshskills.ui.conversation_practice.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.joshtalks.joshskills.repository.server.conversation_practice.ConversationPractiseModel

class RecordPractiseFragment private constructor() : Fragment() {
    private lateinit var conversationPractiseModel: ConversationPractiseModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            conversationPractiseModel =
                it.getParcelable<ConversationPractiseModel>(ARG_PRACTISE_OBJ) as ConversationPractiseModel
        }
    }

    companion object {
        private const val ARG_PRACTISE_OBJ = "practise-obj"

        @JvmStatic
        fun newInstance(conversationPractiseModel: ConversationPractiseModel) =
            RecordPractiseFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_PRACTISE_OBJ, conversationPractiseModel)
                }
            }
    }
}