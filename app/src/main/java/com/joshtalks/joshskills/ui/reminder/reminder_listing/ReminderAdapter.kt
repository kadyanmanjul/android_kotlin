package com.joshtalks.joshskills.ui.reminder.reminder_listing

import android.content.Context
import android.graphics.Color
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.repository.server.reminder.ReminderResponse
import com.joshtalks.joshskills.util.ReminderUtil
import java.text.DecimalFormat
import java.text.NumberFormat

class ReminderAdapter(
    var context: Context,
    private var onStatusUpdate: ((status: ReminderUtil.Companion.ReminderStatus, reminderItem: ReminderResponse) -> Unit)? =
        null,
    private var onItemTimeClick: ((reminderItem: ReminderResponse) -> Unit)? = null,
    private val itemList: List<ReminderResponse>
) : RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder>() {
    var formatter: NumberFormat = DecimalFormat("00")
    private val selectedItems: SparseBooleanArray = SparseBooleanArray()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        return ReminderViewHolder(
            LayoutInflater.from(context)
                .inflate(R.layout.reminder_item_view_holder, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        val reminderItem = getItem(position)
        reminderItem.reminderTime.let { time ->

            val timeParts = time.trim().split(":")

            if (timeParts.isEmpty())
                return
            val mins = formatter.format(timeParts[1].toInt())

            timeParts[0].toIntOrNull()?.let {
                val timeStr: String

                holder.amPmTv.text = if (it < 12) {
                    timeStr = if (it == 0) "12:$mins" else "${formatter.format(it)}:$mins"
                    "am"
                } else {
                    timeStr = if (it == 12) "12:$mins" else "${formatter.format(it - 12)}:$mins"
                    "pm"
                }
                holder.timeTv.text = timeStr
            }
        }
        holder.statusSw.setOnCheckedChangeListener(null)
        if (reminderItem.status == ReminderUtil.Companion.ReminderStatus.ACTIVE.name) {
            holder.statusSw.isChecked = true
            holder.timeTv.setTextColor(Color.parseColor("#172344"))
            holder.amPmTv.setTextColor(Color.parseColor("#172344"))
        } else {
            holder.statusSw.isChecked = false
            holder.timeTv.setTextColor(Color.parseColor("#9098B1"))
            holder.amPmTv.setTextColor(Color.parseColor("#9098B1"))

        }
        holder.statusSw.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked)
                onStatusUpdate?.invoke(
                    ReminderUtil.Companion.ReminderStatus.ACTIVE,
                    getItem(position)
                )
            else {
                onStatusUpdate?.invoke(
                    ReminderUtil.Companion.ReminderStatus.INACTIVE,
                    getItem(position)
                )
            }
        }

        if (selectedItems.size() == 0) {
            holder.statusSw.visibility = VISIBLE
            holder.checkBox.visibility = GONE
        } else {
            holder.statusSw.visibility = GONE
            holder.checkBox.visibility = VISIBLE
        }
        holder.timeTv.setOnClickListener {
            onItemTimeClick?.invoke(getItem(position))
        }
        holder.checkBox.isChecked = selectedItems.get(position, false)

    }


    fun toggleSelection(pos: Int?) {
        pos?.let {
            if (selectedItems.get(pos, false)) {
                selectedItems.delete(pos)
            } else {
                selectedItems.put(pos, true)
            }
            if (selectedItems.size() <= 1)
                notifyDataSetChanged()
            else
                notifyItemChanged(pos)
        }
    }

    fun clearSelections() {
        selectedItems.clear()
        notifyDataSetChanged()
    }

    fun getSelectedItemCount(): Int {
        return selectedItems.size()
    }

    fun getSelectedItems(): java.util.ArrayList<ReminderResponse>? {
        val items: java.util.ArrayList<ReminderResponse> =
            java.util.ArrayList(selectedItems.size())
        for (i in 0 until selectedItems.size()) {
            items.add(getItem(selectedItems.keyAt(i)))
        }
        return items
    }

    inner class ReminderViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        val item = itemView
        var timeTv: MaterialTextView = itemView.findViewById(R.id.time_tv)
        var amPmTv: MaterialTextView = itemView.findViewById(R.id.am_pm_tv)
        var statusSw: SwitchMaterial = itemView.findViewById(R.id.alarm_status_sw)
        var checkBox: MaterialCheckBox = itemView.findViewById(R.id.alarm_check)
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    private fun getItem(position: Int): ReminderResponse {
        return itemList.get(position)
    }
}