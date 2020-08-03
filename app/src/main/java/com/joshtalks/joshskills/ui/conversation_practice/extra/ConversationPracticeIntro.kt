package com.joshtalks.joshskills.ui.conversation_practice.extra

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import com.github.vipulasri.timelineview.TimelineView
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.loadJSONFromAsset
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.databinding.ConversationPracticeTimelineItemBinding
import com.joshtalks.joshskills.repository.local.model.PractiseFlowOptionModel
import com.joshtalks.joshskills.repository.server.conversation_practice.ConversationPractiseModel
import com.joshtalks.joshskills.ui.conversation_practice.IMAGE_URL
import com.joshtalks.joshskills.ui.conversation_practice.adapter.ARG_PRACTISE_OBJ
import kotlinx.android.synthetic.main.fragment_conversation_practice_ntro.continue_btn
import kotlinx.android.synthetic.main.fragment_conversation_practice_ntro.image_view
import kotlinx.android.synthetic.main.fragment_conversation_practice_ntro.recycler_view
import kotlinx.android.synthetic.main.fragment_conversation_practice_ntro.text_header
import kotlinx.android.synthetic.main.fragment_conversation_practice_ntro.text_sub_header
import java.lang.reflect.Type


class ConversationPracticeIntro private constructor() : DialogFragment() {

    companion object {
        @JvmStatic
        fun newInstance(conversationPractiseModel: ConversationPractiseModel) =
            ConversationPracticeIntro().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_PRACTISE_OBJ, conversationPractiseModel)
                }
            }
    }

    private lateinit var conversationPractiseModel: ConversationPractiseModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            conversationPractiseModel =
                it.getParcelable<ConversationPractiseModel>(ARG_PRACTISE_OBJ) as ConversationPractiseModel
        }
        setStyle(STYLE_NO_FRAME, R.style.FullDialogWithAnimationV2)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.window?.setLayout(width, height)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_conversation_practice_ntro, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        requireActivity().intent?.getStringExtra(IMAGE_URL)?.run {
            if (this.isNotEmpty()) {
                image_view.visibility = View.VISIBLE
                image_view.setImage(this)
            }
        }

        text_header.text = conversationPractiseModel.title
        text_sub_header.text = conversationPractiseModel.subTitle

        val typeToken: Type = object : TypeToken<List<PractiseFlowOptionModel>>() {}.type
        val obj = AppObjectController.gsonMapperForLocal.fromJson<List<PractiseFlowOptionModel>>(
            loadJSONFromAsset("practise_flow_details.json"), typeToken
        )
        recycler_view.adapter = ConversationPracticeTimelineAdapter(obj)
        continue_btn.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }
}


private class ConversationPracticeTimelineAdapter(private var items: List<PractiseFlowOptionModel>) :
    RecyclerView.Adapter<ConversationPracticeTimelineAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ConversationPracticeTimelineItemBinding.inflate(inflater, parent, false)
        return ViewHolder(binding, viewType)
    }

    override fun getItemViewType(position: Int): Int {
        return TimelineView.getTimeLineViewType(position, itemCount)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(items.get(position))

    inner class ViewHolder(
        val binding: ConversationPracticeTimelineItemBinding,
        private val viewType: Int
    ) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(practiseFlowOptionModel: PractiseFlowOptionModel) {
            with(binding) {
                timeline.initLine(viewType)
                textHeader.text = practiseFlowOptionModel.header
                textSubHeader.text = practiseFlowOptionModel.subHeader
            }
        }
    }

}


