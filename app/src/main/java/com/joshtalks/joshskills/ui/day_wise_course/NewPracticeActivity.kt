package com.joshtalks.joshskills.ui.day_wise_course

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.ui.practise.PractiseSubmitFragment

class NewPracticeActivity : CoreJoshActivity() {

    companion object {
        fun startNewPracticeActivity(
            context: Activity,
            requestCode: Int,
            chatModel: ChatModel
        ) {
            val intent = Intent(context, NewPracticeActivity::class.java).apply {
                putExtra(CHAT_OBJECT, chatModel)
                //      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                //    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
            context.startActivity(intent)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.practice_activity_new)

//        replaceFragment(R.id.practice_container,SettingsFragment(),SettingsFragment.TAG)
        supportFragmentManager.beginTransaction().add(
            R.id.practice_container,
            PractiseSubmitFragment.instance(
                intent.getParcelableExtra(
                    CHAT_OBJECT
                )!!
            ), PractiseSubmitFragment::class.java.name
        ).commit()
    }
}