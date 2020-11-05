package com.joshtalks.joshskills.ui.certification_exam.questionlistbottom

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.databinding.QuestionListItemViewBinding
import com.joshtalks.joshskills.repository.server.certification_exam.CertificationQuestion

class QuestionListAdapter(
    private var items: List<CertificationQuestion>,
    private val cPosition: Int,
    private val listener: Callback?
) :
    RecyclerView.Adapter<QuestionListAdapter.ViewHolder>() {
    private var context = AppObjectController.joshApplication

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = QuestionListItemViewBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(items[position], position)

    inner class ViewHolder(val binding: QuestionListItemViewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(obj: CertificationQuestion, position: Int) {
            with(binding) {
                textView.text = (position + 1).toString()
                frameLayout.setOnClickListener {
                    listener?.onGoToQuestion(position)
                }
                when {
                    obj.isBookmarked -> {
                        textView.text = EMPTY
                        textView.setBackgroundResource(R.drawable.ic_baseline_bookmark)
                        textView.updatePadding(0, 0, 0, 0)
                    }
                    position == cPosition -> {
                        frameLayout.backgroundTintList = ContextCompat.getColorStateList(
                            AppObjectController.joshApplication,
                            R.color.colorAccent
                        )
                        textView.setTextColor(ContextCompat.getColor(context, R.color.white))
                    }
                    obj.isAttempted -> {
                        textView.setTextColor(ContextCompat.getColor(context, R.color.colorAccent))
                    }
                    /*

                    obj.isViewed -> {
                          textView.setTextColor(ContextCompat.getColor(context, R.color.colorAccent))
                      }*/
                    else -> {
                        textView.setTextColor(ContextCompat.getColor(context, R.color.dark_grey))
                    }
                }
            }
        }
    }
}


interface Callback {
    fun onGoToQuestion(position: Int)
}
