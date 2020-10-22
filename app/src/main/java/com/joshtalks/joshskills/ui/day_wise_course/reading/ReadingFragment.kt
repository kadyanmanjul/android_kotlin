package com.joshtalks.joshskills.ui.day_wise_course.reading

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import java.util.ArrayList

class ReadingFragment : Fragment() {

    companion object {
        const val READING_OBJECT = "reading_object"

        @JvmStatic
        fun instance(chatModelList: ArrayList<ChatModel>) = ReadingFragment().apply {
            arguments = Bundle().apply {
                putParcelableArrayList(READING_OBJECT, chatModelList)
            }
        }
    }
}