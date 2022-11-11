package com.joshtalks.joshskills.ui.conversation_practice.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ALPHA_MAX
import com.joshtalks.joshskills.core.ALPHA_MIN
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.ViewTypeForPractiseUser
import com.joshtalks.joshskills.databinding.AudioPractiseReceivedItemBinding
import com.joshtalks.joshskills.databinding.AudioPractiseSentItemBinding
import com.joshtalks.joshskills.repository.server.conversation_practice.ListenModel

class AudioPractiseAdapter(var items: MutableList<ListenModel> = arrayListOf()) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(), Filterable {
    private var disableBG =
        ContextCompat.getColorStateList(AppObjectController.joshApplication, R.color.pure_grey)
    private var sentBG =
        ContextCompat.getColorStateList(AppObjectController.joshApplication, R.color.decorative_three)
    private var receiveBG =
        ContextCompat.getColorStateList(AppObjectController.joshApplication, R.color.surface_information)
    private var standardBG =
        ContextCompat.getColorStateList(
            AppObjectController.joshApplication,
            R.color.primary_500
        )

    fun addItem(list: ListenModel) {
        items.add(list)
    }

    fun addItems(list: List<ListenModel>) {
        items.addAll(list)
    }

    fun clear() {
        items = arrayListOf()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == ViewTypeForPractiseUser.FIRST.type) {
            val binding = AudioPractiseReceivedItemBinding.inflate(inflater, parent, false)
            ViewHolderReceived(binding)
        } else {
            val binding = AudioPractiseSentItemBinding.inflate(inflater, parent, false)
            ViewHolderSent(binding)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].viewType
    }

    override fun getItemId(position: Int): Long {
        return items[position].id.toLong()
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolderSent) {
            (holder).also {
                it.bind(items[position])
            }
        } else if (holder is ViewHolderReceived) {
            (holder).also {
                it.bind(items[position])
            }
        }
    }

    inner class ViewHolderSent(
        val binding: AudioPractiseSentItemBinding
    ) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(listenModel: ListenModel) {
            with(binding) {
                name.apply {
                    text = listenModel.name
                    setTextColor(
                        ContextCompat.getColor(
                            AppObjectController.joshApplication,
                            R.color.dark_grey
                        )
                    )
                }
                statementTv.apply {
                    text = listenModel.text
                    setTextColor(
                        ContextCompat.getColor(
                            AppObjectController.joshApplication,
                            R.color.text_default
                        )
                    )
                }
                if (listenModel.disable) {
                    tvContainer.backgroundTintList = disableBG
                    tvContainer.alpha = ALPHA_MIN
                } else {
                    if (listenModel.hasPractising) {
                        tvContainer.backgroundTintList = standardBG
                        statementTv.setTextColor(
                            ContextCompat.getColor(
                                AppObjectController.joshApplication,
                                R.color.pure_white
                            )
                        )
                        name.apply {
                            text = context.getString(R.string.me)
                            setTextColor(
                                ContextCompat.getColor(
                                    AppObjectController.joshApplication,
                                    R.color.primary_500
                                )
                            )
                        }
                    } else {
                        tvContainer.backgroundTintList = sentBG
                    }
                    tvContainer.alpha = ALPHA_MAX
                }
            }
        }
    }

    inner class ViewHolderReceived(
        val binding: AudioPractiseReceivedItemBinding
    ) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(listenModel: ListenModel) {
            with(binding) {
                name.apply {
                    text = listenModel.name
                    setTextColor(
                        ContextCompat.getColor(
                            AppObjectController.joshApplication,
                            R.color.dark_grey
                        )
                    )
                }
                statementTv.apply {
                    text = listenModel.text
                    setTextColor(
                        ContextCompat.getColor(
                            AppObjectController.joshApplication,
                            R.color.text_default
                        )
                    )
                }

                if (listenModel.disable) {
                    tvContainer.backgroundTintList = disableBG
                    tvContainer.alpha = ALPHA_MIN
                } else {
                    if (listenModel.hasPractising) {
                        tvContainer.backgroundTintList = standardBG
                        name.apply {
                            text = context.getString(R.string.me)
                            setTextColor(
                                ContextCompat.getColor(
                                    AppObjectController.joshApplication,
                                    R.color.primary_500
                                )
                            )
                        }
                        statementTv.setTextColor(
                            ContextCompat.getColor(
                                AppObjectController.joshApplication,
                                R.color.pure_white
                            )
                        )

                    } else {
                        tvContainer.backgroundTintList = receiveBG
                    }
                    tvContainer.alpha = ALPHA_MAX
                }
            }
        }
    }

    override fun getFilter(): Filter {
        return filter
    }

    private val filter: Filter = object : Filter() {
        val filterResults = FilterResults()
        override fun performFiltering(constraint: CharSequence): FilterResults {
            if (constraint.isBlank()) {
                items.listIterator().forEach { it.disable = false }
            } else {
                val number = Integer.parseInt(constraint.toString())
                items.listIterator().forEach {
                    if (it.viewType == number) {
                        it.disable = true
                    }
                }
            }
            return filterResults.also {
                it.values = items
            }
        }

        override fun publishResults(
            constraint: CharSequence,
            results: FilterResults
        ) {
            notifyDataSetChanged()
        }
    }

}
