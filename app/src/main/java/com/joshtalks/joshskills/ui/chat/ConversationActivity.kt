package com.joshtalks.joshskills.ui.chat

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.databinding.ActivityConversationBinding

class ConversationActivity : CoreJoshActivity() {

    private lateinit var conversationBinding: ActivityConversationBinding

    private val conversationViewModel: ConversationViewModel by lazy {
        ViewModelProviders.of(this).get(ConversationViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        conversationBinding = DataBindingUtil.setContentView(this, R.layout.activity_conversation)
        conversationBinding.viewmodel = conversationViewModel
        addListenerObservable()
    }


    private fun addListenerObservable() {


    }


}
