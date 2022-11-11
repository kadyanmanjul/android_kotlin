package com.joshtalks.joshskills.ui.cohort_based_course.utils


import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.content.res.AppCompatResources.getColorStateList
import androidx.core.content.ContextCompat
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

@BindingAdapter("setButtonBackground", "buttonText", requireAll = false)
fun MaterialButton.setBackgroundState(boolean: Boolean,string: String? = "") {
    when (boolean) {
        true -> {
            this.isEnabled = true
            this.backgroundTintList = getColorStateList(context, R.color.primary_500)
            this.setTextColor(ContextCompat.getColor(context,R.color.pure_white))
            if (string?.isEmpty()?.not() == true)
                this.text = "Continue to course >"
        }
        false -> {
            this.isEnabled = false
            this.backgroundTintList = getColorStateList(context, R.color.disabled)
            this.setTextColor(ContextCompat.getColor(context,R.color.pure_white))
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
 fun RecyclerView.setGridAdapter( list: (ArrayList<CohortItemModel>)?,
                                  function: ((cohortItemModel: CohortItemModel) -> Unit)?) {
    val adapter = list?.let { ScheduleAdapter(it) }
    this.adapter = adapter
    this.setHasFixedSize(true)
    this.layoutManager = GridLayoutManager(context, 2)
    adapter?.setClickListener(function)
}