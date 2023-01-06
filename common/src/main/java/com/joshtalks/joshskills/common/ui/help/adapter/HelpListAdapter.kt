package com.joshtalks.joshskills.common.ui.help.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.twocoffeesoneteam.glidetovectoryou.GlideToVectorYou
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.FRESH_CHAT_UNREAD_MESSAGES
import com.joshtalks.joshskills.common.core.PrefManager
import com.joshtalks.joshskills.common.core.SINGLE_SPACE
import com.joshtalks.joshskills.common.databinding.HelpViewLayoutBinding
import com.joshtalks.joshskills.common.repository.server.help.Option
import com.joshtalks.joshskills.common.ui.extra.setOnSingleClickListener

class HelpListAdapter(private var listOfOption: ArrayList<Option> = arrayListOf()): RecyclerView.Adapter<HelpListAdapter.HelpViewHolder>() {
    private var onClickListener: ((Option, Int) -> Unit) = { _, _ -> }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HelpViewHolder {
        val binding = HelpViewLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return HelpViewHolder(binding)
    }

    override fun getItemCount() = listOfOption.size

    override fun onBindViewHolder(holder: HelpViewHolder, position: Int) {
        holder.setData(listOfOption[position])
    }

    fun addListOfHelp(help: List<Option>){
        if (listOfOption.isEmpty()) {
            listOfOption.addAll(help)
            notifyDataSetChanged()
        }
    }

    fun setOnClickListener(onClickListener: (Option, Int) -> Unit) {
        this.onClickListener = onClickListener
    }

    inner class HelpViewHolder(val binding: HelpViewLayoutBinding) :RecyclerView.ViewHolder(binding.root) {
        fun setData(helpList:Option) {
            val unreadMessages = PrefManager.getIntValue(FRESH_CHAT_UNREAD_MESSAGES)
            if (unreadMessages <= 1)
                binding.tvCategoryName.text = helpList.name
            else
                binding.tvCategoryName.text = helpList.name.plus(SINGLE_SPACE).plus("(${unreadMessages} Msg)")
            GlideToVectorYou
                .init()
                .with(AppObjectController.joshApplication)
                .requestBuilder
                .load(helpList.url)
                .into(binding.ivCategoryIcon)

            binding.rootView.setOnSingleClickListener { onClickListener.invoke( helpList,absoluteAdapterPosition) }
        }
    }
}