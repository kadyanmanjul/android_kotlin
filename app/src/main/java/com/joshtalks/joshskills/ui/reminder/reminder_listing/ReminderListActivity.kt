package com.joshtalks.joshskills.ui.reminder.reminder_listing

import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.GestureDetectorCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import com.joshtalks.joshskills.databinding.ActivityReminderListLayoutBinding
import com.joshtalks.joshskills.repository.server.reminder.ReminderResponse
import com.joshtalks.joshskills.ui.reminder.ReminderBaseActivity
import com.joshtalks.joshskills.ui.reminder.set_reminder.ReminderActivity
import com.joshtalks.joshskills.util.DividerItemDecoration

class ReminderListActivity : ReminderBaseActivity(),
    RecyclerView.OnItemTouchListener {

    private val reminderList: ArrayList<ReminderResponse> = ArrayList()
    private lateinit var gestureDetector: GestureDetectorCompat
    private var actionMode: Boolean = false
    private lateinit var titleView: AppCompatTextView
    private lateinit var helpIv: AppCompatImageView
    lateinit var binding: ActivityReminderListLayoutBinding
    private val viewModel by lazy { ViewModelProvider(this).get(ReminderListingViewModel::class.java) }
    private lateinit var adapter: ReminderAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_reminder_list_layout)
        binding.lifecycleOwner = this
        binding.handler = this
        titleView = findViewById(R.id.text_message_title)
        helpIv = findViewById(R.id.iv_help)
        titleView.text = getString(R.string.reminders)
        helpIv.visibility = GONE
        findViewById<View>(R.id.iv_back).visibility = VISIBLE
        findViewById<View>(R.id.iv_back).setOnClickListener {
            onBackPressed()
        }
        binding.createReminderBtn.setOnClickListener {
            openSetReminder()
        }
        binding.deleteRemindersBtn.setOnClickListener {
            deleteReminders()
        }

        binding.alarmListDescription.text = AppObjectController.getFirebaseRemoteConfig()
            .getString(FirebaseRemoteConfigKey.REMINDERS_SCREEN_DESCRIPTION)

        initRv()
        viewModel.reminderList.observe(this,
            Observer { data ->
                reminderList.clear()
                reminderList.addAll(data)
                adapter.notifyDataSetChanged()
            })
    }

    private fun initRv() {
        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.isSmoothScrollbarEnabled = true
        binding.reminderRecyclerView.layoutManager = linearLayoutManager
        binding.reminderRecyclerView.addItemDecoration(
            DividerItemDecoration(
                this,
                R.drawable.list_divider
            )
        )
        adapter = ReminderAdapter(this, this::onStatusUpdate, this::onItemTimeClick, reminderList)
        binding.reminderRecyclerView.adapter = adapter

        binding.reminderRecyclerView.addOnItemTouchListener(this)
        gestureDetector = GestureDetectorCompat(
            this,
            RecyclerViewChatOnGestureListener()
        )
    }

    private fun openSetReminder() {
        startActivity(Intent(this, ReminderActivity::class.java))
    }

    fun onStatusUpdate(
        status: Companion.ReminderStatus,
        reminderItem: ReminderResponse
    ) {
        if (status.name != reminderItem.status) {
            viewModel.updateReminder(
                reminderItem.id,
                reminderItem.reminderTime,
                reminderItem.reminderFrequency,
                status.name,
                reminderItem.mentor,
                reminderItem.reminderTime
            )

        }
    }

    private fun onItemTimeClick(reminderItem: ReminderResponse) {
        startActivity(
            ReminderActivity.getIntent(
                this,
                reminderItem.reminderTime,
                reminderItem.reminderFrequency,
                reminderItem.id
            )
        )
    }

    private fun deleteReminders() {
        adapter.getSelectedItems().let { items ->
            items?.forEach {
                viewModel.updateReminder(
                    it.id,
                    it.reminderTime,
                    it.reminderFrequency,
                    Companion.ReminderStatus.DELETED.name,
                    it.mentor,
                    it.reminderTime,
                    this::onReminderUpdated
                )
            }
        }
        Toast.makeText(this, getString(R.string.alarms_deleted), Toast.LENGTH_SHORT).show()
        disableActionMode()
    }


    private fun onReminderUpdated(reminderResponse: ReminderResponse) {
        val timeParts = reminderResponse.reminderTime.split(":")

        if (timeParts.isEmpty())
            return
        if (reminderResponse.status == Companion.ReminderStatus.ACTIVE.name) {
            setAlarm(
                getReminderFrequency(reminderResponse.reminderFrequency),
                getAlarmPendingIntent(reminderResponse.id),
                timeParts[0].toIntOrNull(),
                timeParts[1].toIntOrNull()
            )
        } else {
            deleteAlarm(getAlarmPendingIntent(reminderResponse.id))
        }

    }

    private fun getReminderFrequency(frequency: String): Companion.ReminderFrequency {
        return when (frequency) {
            Companion.ReminderFrequency.WEEKDAYS.name -> {
                Companion.ReminderFrequency.WEEKDAYS
            }
            Companion.ReminderFrequency.WEEKENDS.name -> {
                Companion.ReminderFrequency.WEEKENDS
            }
            else -> {
                Companion.ReminderFrequency.EVERYDAY
            }
        }

    }

    fun onTap(view: View?) {
        val idx: Int? = view?.let { binding.reminderRecyclerView.getChildAdapterPosition(it) }
        if (actionMode) {
            myToggleSelection(idx)
            return
        }
    }

    private fun myToggleSelection(idx: Int?) {
        adapter.toggleSelection(idx)
        if (adapter.getSelectedItemCount() == 0) {
            disableActionMode()
        }
    }

    private fun disableActionMode() {
        actionMode = false
        binding.deleteRemindersBtn.visibility = GONE
        binding.createReminderBtn.visibility = VISIBLE
        adapter.clearSelections()
    }

    private fun enableActonMode() {
        actionMode = true
        binding.deleteRemindersBtn.visibility = VISIBLE
        binding.createReminderBtn.visibility = GONE
    }

    inner class RecyclerViewChatOnGestureListener :
        GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val view: View? = binding.reminderRecyclerView.findChildViewUnder(e.x, e.y)
            onTap(view)
            return super.onSingleTapUp(e)
        }

        override fun onLongPress(e: MotionEvent) {
            val view: View? = binding.reminderRecyclerView.findChildViewUnder(e.x, e.y)
            if (actionMode) {
                return
            }
            // Start the CAB using the ActionMode.Callback defined above
            enableActonMode()
            val idx: Int? = view?.let { binding.reminderRecyclerView.getChildLayoutPosition(it) }
            myToggleSelection(idx)
            super.onLongPress(e)
        }
    }

    override fun onBackPressed() {
        if (actionMode) {
            disableActionMode()
        } else
            super.onBackPressed()
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {

    }

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(e)
        return false
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {

    }
}
