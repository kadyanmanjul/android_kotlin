package com.joshtalks.joshskills.ui.course_progress_new

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.custom_ui.decorator.StickHeaderItemDecoration
import com.joshtalks.joshskills.databinding.ProgressActivityAdapterHeaderViewLayoutBinding
import com.joshtalks.joshskills.databinding.ProgressActivityAdapterMainViewLayoutBinding
import com.joshtalks.joshskills.repository.local.entity.CExamStatus
import com.joshtalks.joshskills.repository.server.course_overview.CourseOverviewResponse
import java.util.ArrayList

class ProgressActivityAdapter(
    val context: Context,
    val onItemClickListener: CourseProgressAdapter.ProgressItemClickListener,
    val conversationId: String,
    val lastAvailableLessonId: Int?,
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    StickHeaderItemDecoration.StickyHeaderInterface {
    private var list: ArrayList<CourseOverviewResponse> = arrayListOf()
    private val diffCallback: CourseProgressAdapterDiffCallback by lazy { CourseProgressAdapterDiffCallback() }
    private val viewPool: RecyclerView.RecycledViewPool by lazy { RecyclerView.RecycledViewPool() }

    private var MAIN_VIEW: Int = 1
    private var HEADER_VIEW: Int = 0
    private var updatedListPosition: Int = -1

    fun addItems(newList: List<CourseOverviewResponse>) {
        if (newList.isEmpty()) {
            return
        }
        diffCallback.setItems(list, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        list.clear()
        list.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

   /* fun updateItem(courseOverviewResponseDiff: CourseOverviewResponse, titleKey: String?) {
        if (titleKey.isNullOrBlank().not()) {
            list.forEachIndexed { index, courseOverviewResponse ->
                if (courseOverviewResponse.title.equals(titleKey) && courseOverviewResponse.data.isNullOrEmpty().not()){
                    updatedListPosition=index
                    this.get(updatedListPosition)
                    courseOverviewResponseDiff.data.minus(courseOverviewResponse.data)
                }
            }
            Log.d(
                "Manjul",
                "updateItem() called with: lessonId = $lessonId lesssonData = $lesssonData"
            )
        }
    }*/

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return when (viewType) {
            HEADER_VIEW -> HeaderViewHolder(
                ProgressActivityAdapterHeaderViewLayoutBinding.inflate(
                    LayoutInflater.from(context),
                    parent,
                    false
                )
            )
            else -> {
                ProgressViewHolder(
                    ProgressActivityAdapterMainViewLayoutBinding.inflate(
                        LayoutInflater.from(context),
                        parent,
                        false
                    )
                )
            }
        }
    }

    fun getListner(): StickHeaderItemDecoration.StickyHeaderInterface {
        return this
    }

    override fun getItemViewType(position: Int): Int {
        if (list.get(position).type == -1) {
            return MAIN_VIEW
        } else {
            return HEADER_VIEW
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            MAIN_VIEW -> {
                (holder as ProgressViewHolder).bind(position, list[position])
            }
            else -> {
                (holder as HeaderViewHolder).bind(list[position])
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class ProgressViewHolder(val binding: ProgressActivityAdapterMainViewLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        lateinit var adapter: CourseProgressAdapter

        fun bind(position: Int, item: CourseOverviewResponse) {
            item.data = item.data.sortedBy { it.lessonNo }

            adapter = CourseProgressAdapter(
                context,
                ArrayList(item.data),
                onItemClickListener,
                conversationId,
                item.chatId ?: "0",
                item.certificateExamId ?: 0,
                item.examStatus ?: CExamStatus.FRESH,
                lastAvailableLessonNo = lastAvailableLessonId,
                parentPosition = layoutPosition,
                title = item.title
            )
            binding.progressRv.adapter = adapter
            binding.progressRv.setHasFixedSize(true)
            binding.progressRv.setRecycledViewPool(viewPool)
            if (position == list.size.minus(1)) {
                binding.view.visibility = View.VISIBLE
            } else {
                binding.view.visibility = View.GONE
            }

        }

        fun updateItem(item: CourseOverviewResponse, position: Int) {
            list[position] = item
        }
    }

    inner class HeaderViewHolder(val binding: ProgressActivityAdapterHeaderViewLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CourseOverviewResponse) {
            item.data = item.data.sortedBy { it.lessonNo }
            binding.progressTitleTv.text = item.title
        }
    }

    override fun getHeaderPositionForItem(itemPosition: Int): Int {
        var item = itemPosition
        var headerPosition = 0
        do {
            if (isHeader(item)) {
                headerPosition = item
                break
            }
            item -= 1
        } while (item >= 0)
        return headerPosition
    }

    override fun getHeaderLayout(headerPosition: Int): Int {
        return R.layout.progress_activity_adapter_header_view_layout
    }

    override fun bindHeaderData(header: View?, headerPosition: Int) {
        header?.findViewById<MaterialTextView>(R.id.progress_title_tv)?.text =
            list.get(headerPosition).title
    }

    override fun isHeader(itemPosition: Int): Boolean {
        return list.get(itemPosition).type != -1
    }
}