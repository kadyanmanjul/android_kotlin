package com.joshtalks.joshskills.ui.reminder.reminder_listing

import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.GestureDetectorCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.databinding.ActivityReminderListLayoutBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.reminder.ReminderResponse
import com.joshtalks.joshskills.ui.reminder.set_reminder.ReminderActivity
import com.joshtalks.joshskills.util.DividerItemDecoration
import com.mindorks.placeholderview.SmoothLinearLayoutManager

class ReminderListActivity : CoreJoshActivity(), ReminderAdapter.ReminderItemActionListener,
    RecyclerView.OnItemTouchListener {

    private lateinit var gestureDetector: GestureDetectorCompat
    private var actionMode: Boolean = false
    private lateinit var titleView: AppCompatTextView
    private lateinit var helpIv: AppCompatImageView
    private val reminderItemList: ArrayList<ReminderResponse> = ArrayList()
    lateinit var binding: ActivityReminderListLayoutBinding
    private val viewModel by lazy { ViewModelProvider(this).get(ReminderListingViewModel::class.java) }
    private lateinit var adapter: ReminderAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_reminder_list_layout)
        binding.lifecycleOwner = this
        binding.handler = this
        titleView = findViewById<AppCompatTextView>(R.id.text_message_title)
        helpIv = findViewById<AppCompatImageView>(R.id.iv_help)
        titleView.text = getString(R.string.set_reminder)
        helpIv.visibility = View.GONE
        findViewById<View>(R.id.iv_back).visibility = View.VISIBLE
        findViewById<View>(R.id.iv_back).setOnClickListener {
            onBackPressed()
        }
        binding.createReminderBtn.setOnClickListener {
            openSetReminder()
        }
        binding.deleteRemindersBtn.setOnClickListener {
            deleteReminders()
        }

        initRv()
        viewModel.reminderList.observe(this,
            Observer { data ->
                reminderItemList.clear()
                reminderItemList.addAll(data.filter {
                    !it.status.equals(
                        ReminderActivity.Companion.ReminderStatus.DELETED.name,
                        true
                    )
                })
                adapter.notifyDataSetChanged()
            })
    }

    override fun onResume() {
        super.onResume()
        viewModel.getReminders(Mentor.getInstance().getId())
    }

    private fun initRv() {
        var linearLayoutManager = SmoothLinearLayoutManager(this)
        linearLayoutManager.isSmoothScrollbarEnabled = true
        binding.reminderRecyclerView.layoutManager = linearLayoutManager
        binding.reminderRecyclerView.addItemDecoration(
            DividerItemDecoration(
                this,
                R.drawable.list_divider
            )
        )
        adapter = ReminderAdapter(this, reminderItemList, this)
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

    override fun onStatusUpdate(status: ReminderActivity.Companion.ReminderStatus, position: Int) {
        val reminderItem = reminderItemList[position]
        if (status.name != reminderItem.status) {
            viewModel.updateReminder(
                reminderItem.reminderTime,
                reminderItem.reminderFrequency,
                status.name,
                reminderItem.mentor,
                reminderItem.reminderTime
            )
        }
    }

    override fun onItemTimeClick(position: Int) {
        startActivity(
            ReminderActivity.getIntent(
                this,
                reminderItemList.get(position).reminderTime,
                reminderItemList.get(position).reminderFrequency
            )
        )
    }

    private fun deleteReminders() {
        adapter.getSelectedItems().let {
            it?.forEach {
                viewModel.updateReminder(
                    it.reminderTime,
                    it.reminderFrequency,
                    ReminderActivity.Companion.ReminderStatus.DELETED.name,
                    it.mentor,
                    it.reminderTime
                )
            }
        }

        disableActionMode()
    }


    fun onTap(view: View?) {
        // item click
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