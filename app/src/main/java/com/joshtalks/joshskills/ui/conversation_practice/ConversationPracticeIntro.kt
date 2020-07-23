package com.joshtalks.joshskills.ui.conversation_practice

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.joshtalks.joshskills.R
import kotlinx.android.synthetic.main.fragment_conversation_practice_ntro.image_view


class ConversationPracticeIntro : DialogFragment() {

    companion object {
        fun newInstance() =
            ConversationPracticeIntro().apply {
                arguments = Bundle().apply {
                }
            }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
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
        setupProfilePicture()

    }

    private fun setupProfilePicture() {
        Glide.with(requireContext())
            .load(this)
            .override(Target.SIZE_ORIGINAL)
            .optionalTransform(
                WebpDrawable::class.java,
                WebpDrawableTransformation(CircleCrop())
            )
            .apply(RequestOptions.circleCropTransform())
            .into(image_view)

    }
}
/*

//SparseArray<String> sparseArray = new SparseArray<>();
class ConversationPracticeTimelineAdapter(private var items: SparseArray<String>) :
    RecyclerView.Adapter<ConversationPracticeTimelineAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ContentTimelineItemBinding.inflate(inflater, parent, false)
        return ViewHolder(binding, viewType)
    }

    override fun getItemViewType(position: Int): Int {
        return TimelineView.getTimeLineViewType(position, itemCount)
    }

    override fun getItemCount(): Int = items.size()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items.get(position))

    fun addItem(items: List<CourseContentEntity>) {
        this.items = items
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ContentTimelineItemBinding, private val viewType: Int) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(courseContentEntity: CourseContentEntity) {
            with(binding) {
                timeline.initLine(viewType)
                this.textTitle.text = convertCamelCase(courseContentEntity.title!!)
                this.textTitle.setOnClickListener {
                    RxBus2.publish(ContentClickEventBus(courseContentEntity))
                }
                this.rootView.setOnClickListener {
                    RxBus2.publish(ContentClickEventBus(courseContentEntity))
                }
            }
        }
    }

}
*/

