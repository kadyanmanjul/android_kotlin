package com.joshtalks.joshskills.premium.ui.sharedpreferences

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.databinding.FragmentSharedPreferencesBinding
import com.joshtalks.joshskills.premium.databinding.ItemSharedPreferenceBinding
import com.joshtalks.joshskills.premium.ui.DebugViewModel

class SharedPreferencesFragment : Fragment() {
    private lateinit var binding: FragmentSharedPreferencesBinding
    private val viewModel by lazy {
        ViewModelProvider(this)[DebugViewModel::class.java]
    }
    private lateinit var list: List<Pair<String, Any>>
    private lateinit var adapter: SharedPreferencesAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_shared_preferences, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        adapter = SharedPreferencesAdapter()
        list = viewModel.getSharedPreferences()
        adapter.submitList(list)
        binding.initUI()
    }

    private fun FragmentSharedPreferencesBinding.initUI() {
        recyclerView.adapter = adapter
        binding.search.doOnTextChanged { text, _, _, _ ->
            list.filter { it.first.contains(text.toString(), true) }.let {
                adapter.submitList(it)
            }
        }
    }

    private class SharedPreferencesAdapter :
        RecyclerView.Adapter<SharedPreferencesAdapter.SharedPreferencesViewHolder>() {

        private var list: List<Pair<String, Any>> = emptyList()

        inner class SharedPreferencesViewHolder(val binding: ItemSharedPreferenceBinding) :
            RecyclerView.ViewHolder(binding.root) {
            fun bind(item: Pair<String, Any>) {
                binding.name.text = item.first
                binding.value.text = item.second.toString()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SharedPreferencesViewHolder {
            val binding = ItemSharedPreferenceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return SharedPreferencesViewHolder(binding)
        }

        override fun onBindViewHolder(holder: SharedPreferencesViewHolder, position: Int) {
            holder.bind(list[position])
        }

        override fun getItemCount(): Int = list.size

        fun submitList(list: List<Pair<String, Any>>) {
            this.list = list
            notifyDataSetChanged()
        }
    }

}