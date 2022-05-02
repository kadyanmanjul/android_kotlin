package com.joshtalks.badebhaiya.profile

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.text.HtmlCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.*
import com.joshtalks.badebhaiya.databinding.ActivityProfileBinding
import com.joshtalks.badebhaiya.feed.FeedActivity
import com.joshtalks.badebhaiya.feed.FeedViewModel
import com.joshtalks.badebhaiya.feed.ROOM_DETAILS
import com.joshtalks.badebhaiya.feed.adapter.FeedAdapter
import com.joshtalks.badebhaiya.feed.model.RoomListResponseItem
import com.joshtalks.badebhaiya.liveroom.OPEN_PROFILE
import com.joshtalks.badebhaiya.liveroom.OPEN_ROOM
import com.joshtalks.badebhaiya.profile.request.DeleteReminderRequest
import com.joshtalks.badebhaiya.profile.request.ReminderRequest
import com.joshtalks.badebhaiya.profile.response.ProfileResponse
import com.joshtalks.badebhaiya.repository.model.ConversationRoomResponse
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.utils.Utils
import com.joshtalks.badebhaiya.utils.setUserImageOrInitials
import kotlinx.android.synthetic.main.base_toolbar.view.*
import java.util.*

class ProfileActivity: Fragment(), FeedAdapter.ConversationRoomItemCallback {

//    private val binding by lazy<ActivityProfileBinding> {
//        DataBindingUtil.setContentView(FeedActivity(), R.layout.activity_profile)
//    }

    private var isFromDeeplink = false

    private val viewModel by lazy {
        ViewModelProvider(this)[ProfileViewModel::class.java]
    }

    private val feedViewModel by lazy {
        ViewModelProvider(this)[FeedViewModel::class.java]
    }
    lateinit var binding:ActivityProfileBinding

    private var userId: String? = EMPTY

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.activity_profile, container, false)

        super.onCreate(savedInstanceState)

        //userId= arguments?.getString("user")
//        arguments?.let{
//            it.getString("user")?.let{
//                userId=it
//            }
//        }

        var mBundle: Bundle? = Bundle()
        mBundle = this.arguments
        userId=mBundle!!.getString("user")
        showToast("${userId}")
        handleIntent()
        //showToast("${userId}")
        viewModel.getProfileForUser(userId!!, isFromDeeplink)
        feedViewModel.setIsBadeBhaiyaSpeaker()
        //binding.lifecycleOwner = FeedActivity()
        binding.handler = this
        binding.viewModel = viewModel
//        var activity:FeedActivity= activity as FeedActivity
//        var userId=activity.userid()
        addObserver()
        binding.toolbar.iv_back.setOnClickListener{
            activity?.run {
                supportFragmentManager.beginTransaction().remove(this@ProfileActivity)
                    .commitAllowingStateLoss()
            }
        }
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                //showToast("Back Pressed")
                activity?.run {
                    supportFragmentManager.beginTransaction().remove(this@ProfileActivity)
                        .commitAllowingStateLoss()
                }
            }
        })
        //setOnClickListener()
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        var user:String?
//        if (savedInstanceState != null) {
//            user= savedInstanceState.getString("user")
//        }



    }

    private fun handleIntent() {
        var activity:FeedActivity= activity as FeedActivity
         //userId=activity.userid()
        //userId = intent.getStringExtra(USER_ID)
        //isFromDeeplink = intent.getBooleanExtra(FROM_DEEPLINK, false)
        if (userId.isNullOrEmpty()) userId=User.getInstance().userId
    }

    private fun addObserver() {
        viewModel.userProfileData.observe(FeedActivity()) {
            binding.apply {
                handleSpeakerProfile(it)
                if (it.profilePicUrl.isNullOrEmpty().not()) Utils.setImage(ivProfilePic, it.profilePicUrl.toString())
                else
                    Utils.setImage(ivProfilePic, it.firstName.toString())
               binding.ivProfilePic.setUserImageOrInitials(it.profilePicUrl,it.firstName.toString(),30)
                tvUserName.text = getString(R.string.full_name_concatenated, it.firstName, it.lastName)
            }
        }
        viewModel.speakerFollowed.observe(FeedActivity()) {
            if (it == true) {
                speakerFollowedUIChanges()
            }
            else
                speakerUnfollowedUIChanges()
        }
        viewModel.singleLiveEvent.observe(FeedActivity()) {
            Log.d("ABC2", "Data class called with data message: ${it.what} bundle : ${it.data}")
            when (it.what) {
                OPEN_PROFILE ->{

                }
                OPEN_ROOM ->{
                    it.data?.let {
                        it.getParcelable<ConversationRoomResponse>(ROOM_DETAILS)?.let { room->
                            // TODO: Launch Live Room
//                            ConversationLiveRoomFragment.startRoomActivity(
//                                activity = this@ProfileActivity,
//                                channelName = room.channelName,
//                                uid = room.uid,
//                                token =room.token,
//                                isRoomCreatedByUser = room.moderatorId == room.uid,
//                                roomId = room.roomId,
//                                moderatorId = room.moderatorId,
//                                topicName = it.getString(TOPIC),
//                                flags = arrayOf()
//                            )
                        }
                    }
                }
            }
        }

    }

    private fun handleSpeakerProfile(profileResponse: ProfileResponse) {
        binding.apply {
            if (profileResponse.isSpeaker) {
                tvProfileBio.text = profileResponse.bioText
                tvFollowers.text = HtmlCompat.fromHtml(getString(R.string.bb_followers, profileResponse.followersCount.toString()),
                    HtmlCompat.FROM_HTML_MODE_LEGACY)
                if (profileResponse.isSpeakerFollowed) {
                    speakerFollowedUIChanges()
                }
                else
                    speakerUnfollowedUIChanges()
            } else {
                tvFollowers.text = HtmlCompat.fromHtml(getString(R.string.bb_following, profileResponse.followingCount.toString()),
                    HtmlCompat.FROM_HTML_MODE_LEGACY)
            }
        }
    }

    fun updateFollowStatus() {
        viewModel.updateFollowStatus(userId ?: (User.getInstance().userId))
        if(viewModel.speakerFollowed.value == true)
            viewModel.userProfileData.value?.let {
                //is_followed=false
                //binding.tvFollowers.setText("${it.followersCount-1} followers")
                binding.tvFollowers.text =HtmlCompat.fromHtml(getString(R.string.bb_followers,
                    (it.followersCount.minus(1)?:0).toString()),
                    HtmlCompat.FROM_HTML_MODE_LEGACY)
            }
        else
            viewModel.userProfileData.value?.let {
                //is_followed=true
                binding.tvFollowers.text =HtmlCompat.fromHtml(getString(R.string.bb_followers,
                    (it.followersCount.plus(1)?:0).toString()),
                    HtmlCompat.FROM_HTML_MODE_LEGACY)
                //binding.tvFollowers.setText("${it.followersCount+1} followers")
            }
        viewModel.getProfileForUser(userId ?: (User.getInstance().userId), isFromDeeplink)
    }

    private fun speakerFollowedUIChanges() {
        binding.apply {
            btnFollow.text = getString(R.string.following)
            btnFollow.setTextColor(resources.getColor(R.color.white))
            btnFollow.background = AppCompatResources.getDrawable(requireContext(),
                R.drawable.following_button_background)
        }
    }
    private fun speakerUnfollowedUIChanges() {
        binding.apply {
            btnFollow.text = getString(R.string.follow)
            btnFollow.setTextColor(resources.getColor(R.color.follow_button_stroke))
            btnFollow.background = AppCompatResources.getDrawable(requireContext(),
                R.drawable.follow_button_background)
        }
    }

    companion object {
        const val FROM_DEEPLINK = "from_deeplink"
        fun openProfileActivity(context: Context, userId: String = EMPTY) {
            Intent(context, ProfileActivity::class.java).apply {
                putExtra(USER_ID, userId)
            }.run {
                context.startActivity(this)
            }
        }
        fun getIntent(context: Context, userId: String = EMPTY, isFromDeeplink: Boolean = false): Intent {
            return Intent(context, ProfileActivity::class.java).apply {
                putExtra(USER_ID, userId)
                putExtra(FROM_DEEPLINK, isFromDeeplink)
            }
        }
    }

    override fun joinRoom(room: RoomListResponseItem, view: View) {
        feedViewModel.joinRoom(room)
    }

    override fun setReminder(room: RoomListResponseItem, view: View) {
        val alarmManager = activity as AlarmManager
        val notificationIntent = NotificationHelper.getNotificationIntent(
            requireContext(), Notification(
                title = room.topic ?: "Conversation Room Reminder",
                body = room.speakersData?.name ?: "Conversation Room Reminder",
                id = room.startedBy ?: 0,
                userId = room.speakersData?.userId ?: "",
                type = NotificationType.REMINDER
            )
        )
        val pendingIntent =
            PendingIntent.getBroadcast(
                requireContext(),
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 2000, pendingIntent)
            .also {
                //room.isScheduled = true
                viewModel.setReminder(
                    ReminderRequest(
                        roomId = room.roomId.toString(),
                        userId = User.getInstance().userId,
                        reminderTime = room.startTimeDate.minus(5 * 60 * 1000)
                    )
                )
            }
        viewModel.getProfileForUser(userId ?: (User.getInstance().userId),isFromDeeplink)
    }

    override fun deleteReminder(room: RoomListResponseItem, view: View) {
        //showToast("Schedule Deleted")
        //room.isScheduled=false
        viewModel.deleteReminder(
            DeleteReminderRequest(
                roomId=room.roomId.toString(),
                userId=User.getInstance().userId
            )
        )
    }

    override fun viewProfile(profile: String?) {

    }


    override fun viewRoom(room: RoomListResponseItem, view: View) {

    }
}