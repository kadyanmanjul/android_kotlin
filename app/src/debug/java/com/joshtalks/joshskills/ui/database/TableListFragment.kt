package com.joshtalks.joshskills.ui.database

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.FragmentTableListBinding
import com.joshtalks.joshskills.databinding.ItemNameCountBinding

class TableListFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentTableListBinding
    private val viewModel by lazy {
        ViewModelProvider(this)[DatabaseViewModel::class.java]
    }
    private val adapter = DbTableAdapter()

    private var onTableSelectionListener: (String) -> Unit = {}

    companion object {
        fun newInstance(): TableListFragment {
            return TableListFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_table_list, container, false)
        return binding.root
    }

    fun setOnTableSelectedListener(function: (String) -> Unit) {
        onTableSelectionListener = function
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.adapter = adapter
        viewModel.getListOfTablesInDb()
        addObservers()
        binding.search.doOnTextChanged { text, _, _, _ ->
            viewModel.getListOfTablesInDb(text.toString())
        }
    }

    private fun addObservers() {
        viewModel.pairList.observe(viewLifecycleOwner) {
            adapter.submitList(it.map { it1 -> Pair(it1.first, it1.second.toInt()) })
        }
    }

    inner class DbTableAdapter :
        ListAdapter<Pair<String, Int>, DbTableAdapter.DbTableViewHolder>(TableDiffCallback()) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DbTableViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val binding = ItemNameCountBinding.inflate(layoutInflater, parent, false)
            return DbTableViewHolder(binding)
        }

        override fun onBindViewHolder(holder: DbTableViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        inner class DbTableViewHolder(private val binding: ItemNameCountBinding) :
            RecyclerView.ViewHolder(binding.root) {
            fun bind(item: Pair<String, Int>) {
                binding.data = item
                binding.root.setOnClickListener {
                    onTableSelectionListener.invoke(item.first)
                    dismiss()
                }
            }
        }
    }
}

private class TableDiffCallback : DiffUtil.ItemCallback<Pair<String, Int>>() {
    override fun areItemsTheSame(oldItem: Pair<String, Int>, newItem: Pair<String, Int>): Boolean =
        oldItem == newItem

    override fun areContentsTheSame(oldItem: Pair<String, Int>, newItem: Pair<String, Int>): Boolean =
        oldItem.first.equals(newItem.first, false)
}