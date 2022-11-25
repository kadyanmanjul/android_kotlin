package com.joshtalks.joshskills.common.ui.certification_exam.utils

import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.databinding.BindingAdapter
import com.joshtalks.joshskills.common.R

@BindingAdapter("setStateAdapter")
fun AutoCompleteTextView.setStateAdapter(a:String){
    val option = resources.getStringArray(R.array.states_select)
    val arrayAdapter = ArrayAdapter(context, R.layout.dropdown_item, option)
    this.setAdapter(arrayAdapter)
}

@BindingAdapter("setSelectedState")
fun AutoCompleteTextView.setSelectedState(function: (a: String) -> Unit) {
    this.onItemClickListener =
        AdapterView.OnItemClickListener { parent, _, position, _ ->
            val item: String = parent.getItemAtPosition(position) as String
            function.invoke(item)
        }
}