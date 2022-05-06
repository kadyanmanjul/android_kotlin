package com.joshtalks.badebhaiya.profile

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.text.HtmlCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.*
import com.joshtalks.badebhaiya.core.USER_ID
import com.joshtalks.badebhaiya.databinding.ActivityProfileBinding
import com.joshtalks.badebhaiya.feed.*
import com.joshtalks.badebhaiya.feed.adapter.FeedAdapter
import com.joshtalks.badebhaiya.feed.model.RoomListResponseItem
import com.joshtalks.badebhaiya.liveroom.LiveRoomFragment
import com.joshtalks.badebhaiya.liveroom.OPEN_ROOM
import com.joshtalks.badebhaiya.liveroom.model.StartingLiveRoomProperties
import com.joshtalks.badebhaiya.liveroom.viewmodel.LiveRoomViewModel
import com.joshtalks.badebhaiya.profile.request.DeleteReminderRequest
import com.joshtalks.badebhaiya.profile.request.ReminderRequest
import com.joshtalks.badebhaiya.profile.response.ProfileResponse
import com.joshtalks.badebhaiya.repository.model.ConversationRoomResponse
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.utils.Utils
import com.joshtalks.badebhaiya.utils.setUserImageOrInitials
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.activity_feed.*
import kotlinx.android.synthetic.main.base_toolbar.view.*

class ProfileActivity: Fragment(), Call, FeedAdapter.ConversationRoomItemCallback {

//    private val binding by lazy<ActivityProfileBinding> {
//        DataBindingUtil.setContentView(FeedActivity(), R.layout.activity_profile)
//    }

    private var isFromDeeplink = false

    private val liveRoomViewModel by lazy {
        ViewModelProvider(this)[LiveRoomViewModel::class.java]
    }

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
        (activity as FeedActivity).swipeRefreshLayout.isEnabled=false
        binding = DataBindingUtil.inflate(inflater, R.layout.activity_profile, container, false)
        super.onCreate(savedInstanceState)
        var mBundle: Bundle? = Bundle()
        mBundle = this.arguments
        userId=mBundle!!.getString("user")
        handleIntent()
        viewModel.getProfileForUser(userId!!, isFromDeeplink)
        feedViewModel.setIsBadeBhaiyaSpeaker()
        binding.handler = this
        binding.viewModel = viewModel
        //addObserver()
        binding.toolbar.iv_back.setOnClickListener{
            activity?.run {
                (activity as FeedActivity).swipeRefreshLayout.isEnabled=true
                supportFragmentManager.beginTransaction().remove(this@ProfileActivity)
                    .commitAllowingStateLoss()
            }
        }
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                activity?.run {
                    (activity as FeedActivity).swipeRefreshLayout.isEnabled=true
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
        addObserver()
        viewModel.getProfileForUser(userId!!, isFromDeeplink)
    }

    private fun handleIntent() {
        if (userId.isNullOrEmpty()) userId=User.getInstance().userId
    }

    private fun addObserver() {
        viewModel.userProfileData.observe(viewLifecycleOwner) {
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
        feedViewModel.singleLiveEvent.observe(viewLifecycleOwner) {
            Log.d("ABC2", "Data class called with data message: ${it.what} bundle : ${it.data}")
            when (it.what) {
                OPEN_ROOM ->{
                    it.data?.let {
                        it.getParcelable<ConversationRoomResponse>(ROOM_DETAILS)?.let { room ->
                            val liveRoomProperties = StartingLiveRoomProperties.createFromRoom(
                                room,
                                it.getString(TOPIC)!!
                            )
                            LiveRoomFragment.launch((requireActivity() as AppCompatActivity), liveRoomProperties, liveRoomViewModel)
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

//    override fun joinRoom(room: RoomListResponseItem, view: View   ) {
//        feedViewModel.joinRoom(room, OPEN_ROOM_PROFILE)
//    }

    override fun joinRoom(room: RoomListResponseItem, view: View) {
        takePermissions(room)
    }

    private fun takePermissions(room: RoomListResponseItem? = null) {
        if (PermissionUtils.isCallingPermissionWithoutLocationEnabled(requireContext())) {
            if (room != null) {
                feedViewModel.joinRoom(room)
            }
            return
        }

        PermissionUtils.onlyCallingFeaturePermission(
            (activity as AppCompatActivity),
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            if (room != null) {
                                feedViewModel.joinRoom(room)
                            }
                            return
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            Toast.makeText(
                                requireContext(),
                                "Permission Denied ",
                                Toast.LENGTH_SHORT
                            ).show()
                            PermissionUtils.callingPermissionPermanentlyDeniedDialog(FeedActivity())
                            return
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: MutableList<com.karumi.dexter.listener.PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            }
        )
    }

    fun takePermissionsXml() {
        PermissionUtils.onlyCallingFeaturePermission(
            (requireActivity() as AppCompatActivity),
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (report.isAnyPermissionPermanentlyDenied) {
                            Toast.makeText(
                                requireContext(),
                                "Permission Denied ",
                                Toast.LENGTH_SHORT
                            ).show()
                            PermissionUtils.callingPermissionPermanentlyDeniedDialog((activity as AppCompatActivity))
                            return
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: MutableList<com.karumi.dexter.listener.PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            }
        )
    }

    override fun setReminder(room: RoomListResponseItem, view: View) {
        val alarmManager = activity?.applicationContext?.getSystemService(ALARM_SERVICE) as AlarmManager
        val notificationIntent = context?.let {
            NotificationHelper.getNotificationIntent(
                it, Notification(
                    title = room.topic ?: "Conversation Room Reminder",
                    body = room.speakersData?.name ?: "Conversation Room Reminder",
                    id = room.startedBy ?: 0,
                    userId = room.speakersData?.userId ?: "",
                    type = NotificationType.REMINDER
                )
            )
        }
        val pendingIntent =
            notificationIntent?.let {
                PendingIntent.getBroadcast(
                    FeedActivity(),
                    0,
                    it,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 2000, pendingIntent)
            .also {
                //room.isScheduled = true
                viewModel.setReminder(
                    ReminderRequest(
                        roomId = room.roomId.toString(),
                        userId = User.getInstance().userId,
                        reminderTime = room.startTimeDate.minus(5 * 60 * 1000),
                        isFromDeeplink,
                    )
                )
            }
        viewModel.getProfileForUser(userId ?: (User.getInstance().userId),isFromDeeplink)
    }

    override fun deleteReminder(room: RoomListResponseItem, view: View) {
        viewModel.deleteReminder(
            DeleteReminderRequest(
                roomId=room.roomId.toString(),
                userId=User.getInstance().userId
            )
        )
        viewModel.getProfileForUser(userId ?: (User.getInstance().userId),isFromDeeplink)
    }

    override fun viewProfile(profile: String?) {
    }


    override fun viewRoom(room: RoomListResponseItem, view: View) {

    }

    override fun itemClick(userId: String) {
    }
}