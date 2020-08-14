package com.joshtalks.joshskills.ui.reminder.reminder_listing

import android.content.Context
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
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.repository.server.reminder.ReminderResponse
import com.joshtalks.joshskills.ui.reminder.set_reminder.ReminderActivity

class ReminderAdapter(
    var context: Context,
    private var reminderItemList: List<ReminderResponse>,
    private var actionListener: ReminderItemActionListener
) : RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder>() {

    private val selectedItems: SparseBooleanArray = SparseBooleanArray()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        return ReminderViewHolder(
            LayoutInflater.from(context)
                .inflate(R.layout.reminder_item_view_holder, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return reminderItemList.size
    }

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        val reminderItem = reminderItemList.get(position)
        reminderItem.reminderTime.let {
            val timeParts = it.split(":")
            if (timeParts.isEmpty())
                return
            val hour = timeParts[0]
            val mins = timeParts[1]
            hour.toIntOrNull()?.let {
                var timeStr = EMPTY
                var amPm = EMPTY

                if (it < 12) {
                    if (it == 0)
                        timeStr = "12:$mins"
                    else
                        timeStr = "${hour}:$mins"
                    amPm = "am"
                } else {
                    if (it == 12)
                        timeStr = "12:$mins"
                    else
                        timeStr = "${it - 12}:$mins"
                    amPm = "pm"
                }
                holder.timeTv.text =
                    org.shadow.apache.commons.lang3.StringUtils.stripStart(timeStr, "0")
                holder.amPmTv.text = amPm

            }
        }
        holder.status.isChecked =
            reminderItem.status == ReminderActivity.Companion.ReminderStatus.ACTIVE.name

        holder.status.setOnCheckedChangeListener { _, isChecked ->
            if (reminderItem.isSelected == isChecked)
                return@setOnCheckedChangeListener

            if (isChecked)
                actionListener.onStatusUpdate(
                    ReminderActivity.Companion.ReminderStatus.ACTIVE,
                    position
                )
            else {
                actionListener.onStatusUpdate(
                    ReminderActivity.Companion.ReminderStatus.INACTIVE,
                    position
                )
            }
        }

        if (selectedItems.size() == 0) {
            holder.status.visibility = VISIBLE
            holder.checkBox.visibility = GONE
        } else {
            holder.status.visibility = GONE
            holder.checkBox.visibility = VISIBLE
        }
        holder.timeTv.setOnClickListener(View.OnClickListener {
            actionListener.onItemTimeClick(position)
        })
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
            java.util.ArrayList<ReminderResponse>(selectedItems.size())
        for (i in 0 until selectedItems.size()) {
            items.add(reminderItemList.get(selectedItems.keyAt(i)))
        }
        return items
    }

    inner class ReminderViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        val item = itemView
        var timeTv: MaterialTextView = itemView.findViewById(R.id.time_tv)
        var amPmTv: MaterialTextView = itemView.findViewById(R.id.am_pm_tv)
        var status: SwitchMaterial = itemView.findViewById(R.id.alarm_status_sw)
        var checkBox: MaterialCheckBox = itemView.findViewById(R.id.alarm_check)
    }

    interface ReminderItemActionListener {
        fun onStatusUpdate(status: ReminderActivity.Companion.ReminderStatus, position: Int)
        fun onItemTimeClick(position: Int)
    }
}