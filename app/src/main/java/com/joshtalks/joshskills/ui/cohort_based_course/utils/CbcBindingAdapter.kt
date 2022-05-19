package com.joshtalks.joshskills.ui.cohort_based_course.utils


import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.content.res.AppCompatResources.getColorStateList
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.ui.cohort_based_course.adapters.ScheduleAdapter
import com.joshtalks.joshskills.ui.cohort_based_course.models.CohortItemModel

@BindingAdapter("setTextAdapter")
fun AutoCompleteTextView.setTextAdapter(a: String) {
    val option = resources.getStringArray(R.array.form_options)
    val arrayAdapter = ArrayAdapter(context, R.layout.dropdown_item, option)
    this.setAdapter(arrayAdapter)
}

@BindingAdapter("setButtonBackground")
fun MaterialButton.setBackgroundState(boolean: Boolean) {
    when (boolean) {
        true -> {
            this.isEnabled = true
            this.backgroundTintList = getColorStateList(context, R.color.colorPrimary);
        }
        false -> {
            this.backgroundTintList = getColorStateList(context, R.color.gray_6F);
            this.isEnabled = false
        }
    }
}

@BindingAdapter("setSelectedText")
fun AutoCompleteTextView.setSelectedText(function: (a: String) -> Unit) {
    this.onItemClickListener =
        AdapterView.OnItemClickListener { parent, view, position, id ->
            val item: String = parent.getItemAtPosition(position) as String
            function.invoke(item)
        }
}

@BindingAdapter("setGridAdapter","setItemListener", requireAll = false)
fun RecyclerView.setGridAdapter( list: (ArrayList<CohortItemModel>)?,function: ((a: String) -> Unit)?) {
    val adapter = list?.let { ScheduleAdapter(it) }
    this.adapter = adapter
    this.layoutManager = GridLayoutManager(context, 2)
    adapter?.setClickListener(function)

}