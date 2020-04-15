package com.joshtalks.joshskills.ui.chat.course_content

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.HELP_ACTIVITY_REQUEST_CODE
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.service.CONVERSATION_ID
import com.joshtalks.joshskills.databinding.FragmentContentTimelineBinding
import com.joshtalks.joshskills.ui.help.HelpActivity
import com.joshtalks.joshskills.ui.pdfviewer.COURSE_NAME


class ContentTimelineFragment : DialogFragment() {
    private var listener: OnGoContentListener? = null

    companion object {
        @JvmStatic
        fun newInstance(conversationId: String, courseName: String) =
            ContentTimelineFragment().apply {
                arguments = Bundle().apply {
                    putString(CONVERSATION_ID, conversationId)
                    putString(COURSE_NAME, courseName)
                }
            }
    }

    private lateinit var binding: FragmentContentTimelineBinding
    private lateinit var viewModel: ContentViewModel
    private var contentTimelineAdapter = ContentTimelineAdapter(emptyList())
    private var courseName: String? = null
    private var conversationId: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = activity?.run { ViewModelProvider(this).get(ContentViewModel::class.java) }
            ?: throw Exception("Invalid Activity")
        setStyle(STYLE_NO_FRAME, R.style.full_dialog)
        arguments?.let {
            courseName = it.getString(COURSE_NAME)
            conversationId = it.getString(CONVERSATION_ID)
        }
        viewModel.conversationId = conversationId!!
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnGoContentListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            dialog.window?.setLayout(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_content_timeline, container, false)
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.ivHelp.setOnClickListener {
            val i = Intent(requireActivity(), HelpActivity::class.java)
            startActivityForResult(i, HELP_ACTIVITY_REQUEST_CODE)
            AppAnalytics.create(AnalyticsEvent.HELP_SELECTED.NAME).push()
        }
        val layoutManager = LinearLayoutManager(context)
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.setHasFixedSize(false)
        binding.recyclerView.adapter = contentTimelineAdapter
        viewModel.userContentViewModel.observe(viewLifecycleOwner, Observer { list ->
            contentTimelineAdapter.addItem(list.filter { it.title.isNullOrEmpty().not() })
        })
        viewModel.getReceivedCourseContent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        AppAnalytics.create(AnalyticsEvent.BACK_PRESSED.NAME)
            .addParam("name", javaClass.simpleName)
            .push()
    }

    /* override fun onClick(courseContentEntity: CourseContentEntity) {
         listener?.onFocused(courseContentEntity.chat_id)
         dismissAllowingStateLoss()
     }*/

}

interface OnGoContentListener {
    fun onFocused(chatId: String)
}
