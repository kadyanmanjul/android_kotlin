package com.joshtalks.badebhaiya.liveroom.bottomsheet

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.databinding.BottomSheetCreateRoomBinding
import com.joshtalks.badebhaiya.feed.FeedViewModel
import com.joshtalks.badebhaiya.feed.model.RoomListResponseItem
import com.joshtalks.badebhaiya.repository.model.ConversationRoomResponse
import com.joshtalks.badebhaiya.utils.DATE_FORMATTER
import com.joshtalks.badebhaiya.utils.TWENTY_FOUR_HOUR_CLOCK_TIME
import com.joshtalks.badebhaiya.utils.datetimeutils.DateTimeStyle
import com.joshtalks.badebhaiya.utils.datetimeutils.DateTimeUtils
import com.joshtalks.badebhaiya.utils.Utils
import com.joshtalks.badebhaiya.utils.YEAR_DIFFERENCE
import java.util.Date

class CreateRoom : BottomSheetDialogFragment() {
    interface CreateRoomCallback {
        fun onRoomCreated(conversationRoomResponse: ConversationRoomResponse, topic: String)
        fun onRoomSchedule(room: RoomListResponseItem)
        fun onError(error: String)
    }

    private var callback: CreateRoomCallback? = null
    private lateinit var binding: BottomSheetCreateRoomBinding
    private val viewModel by activityViewModels<FeedViewModel>()

    private val dateSetListener = object: DatePickerDialog.OnDateSetListener {
        override fun onDateSet(p0: DatePicker?, p1: Int, p2: Int, p3: Int) {
            DateTimeUtils.setTimeInMillis(System.currentTimeMillis())
            val selectedDate = Date(p1 - YEAR_DIFFERENCE, p2, p3, DateTimeUtils.getHours()+5, DateTimeUtils.getMinutes()+30, DateTimeUtils.getSeconds())
            viewModel.setScheduleStartDate(DATE_FORMATTER.format(selectedDate))
            binding.dateBtn.text = DateTimeUtils.formatWithStyle(selectedDate, DateTimeStyle.SEMI_MEDIUM)
        }
    }
    private val timeSetListener = object: TimePickerDialog.OnTimeSetListener {
        override fun onTimeSet(p0: TimePicker?, p1: Int, p2: Int) {
            val todayDate = Date(System.currentTimeMillis())
            val selectedDate = Date(todayDate.year, todayDate.month, todayDate.date, p1, p2)
            viewModel.setScheduleStartTime(TWENTY_FOUR_HOUR_CLOCK_TIME.format(selectedDate))
            binding.timeBtn.text = Utils.getMessageTimeInHours(selectedDate)
        }
    }

    companion object {
        fun newInstance(): CreateRoom {
            return CreateRoom()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        isCancelable = true
        binding = DataBindingUtil.inflate(
            inflater, R.layout.bottom_sheet_create_room, container, false
        )
        addObserver()
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.callback = callback
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
    }

    private fun initUI() {
        binding.apply {
            shouldStartNow.setOnCheckedChangeListener { switch, isChecked ->
                if (isChecked) {
                    createBtn.visibility = View.VISIBLE
                    scheduleBtn.visibility = View.GONE
                    dateBtn.visibility = View.GONE
                    timeBtn.visibility = View.GONE
                } else {
                    shouldStartNow.visibility = View.GONE
                    createBtn.visibility = View.GONE
                    scheduleBtn.visibility = View.VISIBLE
                    dateBtn.visibility = View.VISIBLE
                    timeBtn.visibility = View.VISIBLE
                }
            }
            dateBtn.apply {
                val todayDate = Date(System.currentTimeMillis())
                viewModel?.setScheduleStartDate(DATE_FORMATTER.format(todayDate))
                text = DateTimeUtils.formatWithStyle(todayDate, DateTimeStyle.SEMI_MEDIUM)
                setOnClickListener {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        DateTimeUtils.setTimeInMillis(System.currentTimeMillis())
                        val datePicker = DatePickerDialog(requireContext(), dateSetListener,
                            DateTimeUtils.getYears(), DateTimeUtils.getMonths(), DateTimeUtils.getDay())
                        datePicker.show()
                    }
                }
            }
            timeBtn.apply {
                val todayDate = Date(System.currentTimeMillis())
                viewModel?.setScheduleStartTime(TWENTY_FOUR_HOUR_CLOCK_TIME.format(todayDate))
                text = Utils.getMessageTimeInHours(todayDate)
                setOnClickListener {
                    DateTimeUtils.setTimeInMillis(System.currentTimeMillis())
                    val timePicker = TimePickerDialog(requireContext(), timeSetListener,
                        DateTimeUtils.getHours(), DateTimeUtils.getMinutes(), false)
                    timePicker.show()
                }
            }
        }
    }

    fun addObserver(){
        viewModel.isRoomCreated.observe(viewLifecycleOwner){
            if(it){
                binding.createBtn.isEnabled=false
            }
            else
                binding.createBtn.isEnabled=true
        }
    }

    fun addRoomCallbacks(callback: CreateRoomCallback) {
        this.callback = callback
    }

}