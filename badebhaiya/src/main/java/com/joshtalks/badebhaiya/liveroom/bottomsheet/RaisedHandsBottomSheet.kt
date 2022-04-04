package com.joshtalks.badebhaiya.liveroom.bottomsheet

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.collection.ArraySet
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.databinding.BottomSheetRaisedHandsBinding
import com.joshtalks.badebhaiya.feed.model.LiveRoomUser
import com.joshtalks.badebhaiya.liveroom.viewmodel.ConversationRoomViewModel
/*

class RaisedHandsBottomSheet : DialogFragment() {
    private lateinit var binding: BottomSheetRaisedHandsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        isCancelable = true
        binding = DataBindingUtil.inflate(
            inflater, R.layout.bottom_sheet_raised_hands, container, false
        )
        binding.lifecycleOwner = this
        binding.fragment = this
        return binding.root
    }

    interface HandRaiseSheetListener {
        fun onUserInvitedToSpeak(user: LiveRoomUser)
    }
}*/

class RaisedHandsBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding: BottomSheetRaisedHandsBinding
    private var roomId: Int? = null
    private var moderatorUid: Int? = null
    private var moderatorName: String? = null
    private var channelName: String? = null
    private var raisedHandList: List<LiveRoomUser>? = arrayListOf()
    private var bottomSheetAdapter: RaisedHandsBottomSheetAdapter? = null
    private val viewModel by lazy { ViewModelProvider(requireActivity()).get(
        ConversationRoomViewModel::class.java) }
    private var listener: HandRaiseSheetListener? = null

    companion object {
        @JvmStatic
        fun newInstance(
            id: Int,
            moderatorId: Int?,
            name: String?,
            channelName: String?,
            raisedHandList: ArrayList<LiveRoomUser>
        ) =
            RaisedHandsBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(CHANNEL_NAME, channelName)
                    putString(MODERATOR_NAME, name)
                    putInt(MODERATOR_ID, moderatorId ?: 0)
                    putInt(ROOM_ID, id)
                    putParcelableArrayList(USER_LIST, raisedHandList)
                }
            }

        private const val CHANNEL_NAME = "channel_name"
        private const val MODERATOR_NAME = "moderator_name"
        private const val MODERATOR_ID = "moderator_id"
        private const val ROOM_ID = "room_id"
        private const val USER_LIST = "user_list"

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            channelName = it.getString(CHANNEL_NAME)
            moderatorName = it.getString(MODERATOR_NAME)
            moderatorUid = it.getInt(MODERATOR_ID, 0)
            roomId = it.getInt(ROOM_ID, 0)
            raisedHandList = it.getParcelableArrayList<LiveRoomUser>(USER_LIST)
            Log.d("RaisedHandsBottomSheet", "onCreate() called ${raisedHandList}")
        }
        listener = requireActivity() as HandRaiseSheetListener
        setStyle(STYLE_NORMAL, R.style.BaseBottomSheetDialog)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        isCancelable = true
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.li_bottom_sheet_raised_hands,
            container,
            false
        )
        binding.lifecycleOwner = this
        binding.fragment = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addObserver()
        configureRecyclerView()
    }

    private fun addObserver() {
        viewModel.audienceList.observe(requireActivity(),{
            refreshAdapterWithNewList(it)
        })
    }

    private fun refreshAdapterWithNewList(handRaisedList: ArraySet<LiveRoomUser>?) {
        val list = handRaisedList?.filter { it.isSpeaker==false && it.isHandRaised }
        this.raisedHandList = list?.sortedBy { it.sortOrder }
        setVisibilities()
        bottomSheetAdapter?.updateFullList(raisedHandList)

    }

    private fun configureRecyclerView() {
        bottomSheetAdapter = RaisedHandsBottomSheetAdapter()
        binding.raisedHandsList.apply {
            layoutManager = LinearLayoutManager(this.context)
            setHasFixedSize(false)
            adapter = bottomSheetAdapter
            itemAnimator = null
        }
        bottomSheetAdapter?.updateFullList(raisedHandList)
        bottomSheetAdapter?.setOnItemClickListener(object :
            RaisedHandsBottomSheetAdapter.RaisedHandsBottomSheetAction {
            override fun onItemClick(
                liveRoomUser: LiveRoomUser,
                position: Int
            ) {
                listener?.onUserInvitedToSpeak(liveRoomUser)
            }

        })
        setVisibilities()
    }

    private fun setVisibilities() {
        if (raisedHandList == null || raisedHandList?.isEmpty() == true) {
            with(binding) {
                noAuidenceText.visibility = android.view.View.VISIBLE
                raisedHandsList.visibility = android.view.View.GONE
            }
        } else {
            with(binding) {
                noAuidenceText.visibility = android.view.View.GONE
                raisedHandsList.visibility = android.view.View.VISIBLE
            }
        }
    }

    interface HandRaiseSheetListener {
        fun onUserInvitedToSpeak(user: LiveRoomUser)
    }

}