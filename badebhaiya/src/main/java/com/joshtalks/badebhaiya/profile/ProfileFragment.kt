package com.joshtalks.badebhaiya.profile

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.HtmlCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.*
import com.joshtalks.badebhaiya.core.USER_ID
import com.joshtalks.badebhaiya.core.models.PendingPilotEvent
import com.joshtalks.badebhaiya.core.models.PendingPilotEvent.*
import com.joshtalks.badebhaiya.core.models.PendingPilotEventData
import com.joshtalks.badebhaiya.databinding.FragmentProfileBinding
import com.joshtalks.badebhaiya.feed.*
import com.joshtalks.badebhaiya.feed.adapter.FeedAdapter
import com.joshtalks.badebhaiya.feed.model.RoomListResponseItem
import com.joshtalks.badebhaiya.liveroom.LiveRoomFragment
import com.joshtalks.badebhaiya.liveroom.LiveRoomState
import com.joshtalks.badebhaiya.liveroom.OPEN_ROOM
import com.joshtalks.badebhaiya.liveroom.ROOM_EXPAND
import com.joshtalks.badebhaiya.liveroom.model.StartingLiveRoomProperties
import com.joshtalks.badebhaiya.liveroom.viewmodel.LiveRoomViewModel
import com.joshtalks.badebhaiya.profile.request.DeleteReminderRequest
import com.joshtalks.badebhaiya.profile.request.ReminderRequest
import com.joshtalks.badebhaiya.profile.response.ProfileResponse
import com.joshtalks.badebhaiya.repository.model.ConversationRoomResponse
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.signup.SignUpActivity
import com.joshtalks.badebhaiya.utils.*
import com.joshtalks.badebhaiya.signup.UserPicChooserFragment
import com.joshtalks.badebhaiya.utils.SingleDataManager
import com.joshtalks.badebhaiya.utils.Utils
import com.joshtalks.badebhaiya.utils.setUserImageOrInitials
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.activity_feed.*
import kotlinx.android.synthetic.main.base_toolbar.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlinx.android.synthetic.main.why_room.view.*

class ProfileFragment: Fragment(), Call, FeedAdapter.ConversationRoomItemCallback {

    private var isFromDeeplink = false

    private val liveRoomViewModel by lazy {
        ViewModelProvider(requireActivity())[LiveRoomViewModel::class.java]
    }

    private val viewModel by lazy {
        ViewModelProvider(this)[ProfileViewModel::class.java]
    }

    private val feedViewModel by lazy {
        ViewModelProvider(requireActivity())[FeedViewModel::class.java]
    }
    lateinit var binding: FragmentProfileBinding

    private var userId: String? = EMPTY

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        (activity as FeedActivity).swipeRefreshLayout.isEnabled=false
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_profile, container, false)
        super.onCreate(savedInstanceState)
        var mBundle: Bundle? = Bundle()
        mBundle = this.arguments
        userId=mBundle!!.getString("user")
        isFromDeeplink=mBundle!!.getBoolean("deeplink")
        handleIntent()
//        if(isFromDeeplink && User.getInstance().isLoggedIn())
//        {
//            showPopup(room.roomId, User.getInstance().userId)
//        }
        viewModel.getProfileForUser(userId!!, isFromDeeplink)
        feedViewModel.setIsBadeBhaiyaSpeaker()
        binding.handler = this
        binding.viewModel = viewModel
        //addObserver()
        binding.toolbar.iv_back.setOnClickListener{
            activity?.run {
                (activity as FeedActivity).swipeRefreshLayout.isEnabled=true
//                supportFragmentManager.beginTransaction().remove(this@ProfileFragment)
//                    .commitAllowingStateLoss()
                onBackPressed()
            }
        }
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                activity?.run {
                    (activity as FeedActivity).swipeRefreshLayout.isEnabled=true
                    supportFragmentManager.beginTransaction().remove(this@ProfileFragment)
                        .commitAllowingStateLoss()
                }
            }
        })

//        binding.ivProfilePic.setOnLongClickListener{
//            CoroutineScope(Dispatchers.IO).launch {
//                val resp= CommonRepository().signOutUser()
//                if(resp.isSuccessful) {
//                    User.deleteUserCredentials(true)
//                }
//            }
//            return@setOnLongClickListener true
//        }
        binding.ivProfilePic.setOnClickListener{
            UserPicChooserFragment.showDialog(childFragmentManager, true)

        }
        //setOnClickListener()
        return binding.root

    }

    fun showPopup(roomId: Int, userId: String) {
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(binding.roomFrame.context)
        val inflater = LayoutInflater.from(binding.roomFrame.context)
        val dialogView = inflater.inflate(R.layout.why_room, null)
        dialogBuilder.setView(dialogView)
        val alertDialog: AlertDialog = dialogBuilder.create()
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        alertDialog.show()

        dialogView.findViewById<AppCompatTextView>(R.id.submit).setOnClickListener{
            //TODO:implement the response API
            val msg:String
//            if(!binding.roomFrame.message.toString().isNullOrBlank())
//                 msg=binding.roomFrame.message.toString()

            //apicall(roomId,userId,msg)
            alertDialog.dismiss()
        }
        dialogView.findViewById<AppCompatTextView>(R.id.Skip).setOnClickListener {
            alertDialog.dismiss()
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("THIS IS FCM TOKEN => ${PrefManager.getStringValue(com.joshtalks.badebhaiya.notifications.FCM_TOKEN)}")
        addObserver()
        viewModel.getProfileForUser(userId!!, isFromDeeplink)
        executePendingActions()
    }

    private fun executePendingActions() {
        SingleDataManager.pendingPilotAction?.let {
            when(it){
                FOLLOW -> followPilot()
            }
        }
    }

    private fun followPilot(){
        SingleDataManager.pendingPilotAction = null
        updateFollowStatus()
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
        viewModel.speakerFollowed.observe(requireActivity()) {
            if (it == true) {
                speakerFollowedUIChanges()
            }
            else
                speakerUnfollowedUIChanges()
        }
        feedViewModel.singleLiveEvent.observe(viewLifecycleOwner) {
            Log.d("ABC2", "Data class called with profile data message: ${it.what} bundle : ${it.data}")
            when (it.what) {
                OPEN_ROOM ->{
                    it.data?.let {
                        Log.i("YASHENDRA", "addObserver: ")
                        it.getParcelable<ConversationRoomResponse>(ROOM_DETAILS)?.let { room ->
                            val liveRoomProperties = StartingLiveRoomProperties.createFromRoom(
                                room,
                                it.getString(TOPIC)!!
                            )
                            LiveRoomFragment.launch((requireActivity() as AppCompatActivity), liveRoomProperties, liveRoomViewModel)
                        }
                    }
                }

                ROOM_EXPAND->{
                 liveRoomViewModel.liveRoomState.value=LiveRoomState.EXPANDED
//                    var live=LiveRoomFragment()
//                    live.expandLiveRoom()
                }
            }
        }

    }

    private fun handleSpeakerProfile(profileResponse: ProfileResponse) {
        binding.apply {
            if (profileResponse.isSpeaker) {
                tvProfileBio.text = profileResponse.bioText
                tvFollowers.text = HtmlCompat.fromHtml(getString(R.string.bb_followers, "<big>"+profileResponse.followersCount.toString()+"</big>"),
                    HtmlCompat.FROM_HTML_MODE_LEGACY)
                if (profileResponse.isSpeakerFollowed) {
                    speakerFollowedUIChanges()
                }
                else
                    speakerUnfollowedUIChanges()
            } else {
                tvFollowers.text = HtmlCompat.fromHtml(getString(R.string.bb_following, "<big>"+profileResponse.followingCount.toString()+"</big>"),
                    HtmlCompat.FROM_HTML_MODE_LEGACY)
            }
        }
    }

    fun updateFollowStatus() {
        if (!User.getInstance().isLoggedIn() ){
            userId?.let {
                redirectToSignUp(FOLLOW, PendingPilotEventData(pilotUserId = it))
            }
            return
        }


        viewModel.updateFollowStatus(userId ?: (User.getInstance().userId))
        if(viewModel.speakerFollowed.value == true)
            viewModel.userProfileData.value?.let {
                //is_followed=false
                //binding.tvFollowers.setText("${it.followersCount-1} followers")
                speakerUnfollowedUIChanges()
                binding.tvFollowers.text =HtmlCompat.fromHtml(getString(R.string.bb_followers,
                    ("<big>"+it.followersCount.minus(1)?:0).toString()+"</big>"),
                    HtmlCompat.FROM_HTML_MODE_LEGACY)
            }
        else
            viewModel.userProfileData.value?.let {
                //is_followed=true
                speakerFollowedUIChanges()
                binding.tvFollowers.text =HtmlCompat.fromHtml(getString(R.string.bb_followers,
                    ("<big>"+it.followersCount.plus(1)?:0).toString()+"</big>"),
                    HtmlCompat.FROM_HTML_MODE_LEGACY)
                //binding.tvFollowers.setText("${it.followersCount+1} followers")
            }
        viewModel.getProfileForUser(userId ?: (User.getInstance().userId), isFromDeeplink)
    }

    private fun redirectToSignUp(pendingPilotAction: PendingPilotEvent, pendingPilotEventData: PendingPilotEventData) {
        SingleDataManager.pendingPilotAction = pendingPilotAction
        SingleDataManager.pendingPilotEventData = pendingPilotEventData
        SignUpActivity.start(requireActivity(), isRedirected = true)
        requireActivity().finish()
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
            Intent(context, ProfileFragment::class.java).apply {
                putExtra(USER_ID, userId)
            }.run {
                context.startActivity(this)
            }
        }
        fun getIntent(context: Context, userId: String = EMPTY, isFromDeeplink: Boolean = false): Intent {
            return Intent(context, ProfileFragment::class.java).apply {
                putExtra(USER_ID, userId)
                putExtra(FROM_DEEPLINK, isFromDeeplink)
            }
        }
    }

//    override fun joinRoom(room: RoomListResponseItem, view: View   ) {
//        feedViewModel.joinRoom(room, OPEN_ROOM_PROFILE)
//    }

    override fun joinRoom(room: RoomListResponseItem, view: View) {
        takePermissions(room.roomId.toString(), room.topic.toString())
    }

    private fun takePermissions(room: String? = null, roomTopic: String) {
        if (PermissionUtils.isCallingPermissionWithoutLocationEnabled(requireContext())) {
            if (room != null) {
                feedViewModel.joinRoom(room, roomTopic)
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
                                feedViewModel.joinRoom(room, roomTopic)
                            }
                            return
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            Toast.makeText(
                                requireContext(),
                                "Permission Denied ",
                                Toast.LENGTH_SHORT
                            ).show()
                            PermissionUtils.callingPermissionPermanentlyDeniedDialog(requireActivity())
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
        if (!User.getInstance().isLoggedIn()){

            userId?.let {
                redirectToSignUp(SET_REMINDER, PendingPilotEventData(roomId = room.roomId, pilotUserId = it))
            }
            return
        }
        if(isFromDeeplink)
            showPopup(room.roomId,User.getInstance().userId)

        lifecycleScope.launch(Dispatchers.IO) {


        val speakerBitmap = room.speakersData?.photoUrl?.urlToBitmap()

        val alarmManager = activity?.applicationContext?.getSystemService(ALARM_SERVICE) as AlarmManager
        val notificationIntent = context?.let {
            NotificationHelper.getNotificationIntent(
                it, Notification(
                    title = room.topic ?: "Conversation Room Reminder",
                    body = room.speakersData?.name ?: "Conversation Room Reminder",
                    id = room.startedBy ?: 0,
                    userId = room.speakersData?.userId ?: "",
                    type = NotificationType.LIVE,
                    roomId = room.roomId.toString(),
                    speakerPicture = speakerBitmap
                )
            )
        }
        val pendingIntent =
            notificationIntent?.let {
                PendingIntent.getBroadcast(
                    requireActivity().applicationContext,
                    0,
                    it,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + room.startTimeDate.minus(5 * 60 * 1000), pendingIntent)
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

    }

    override fun viewProfile(profile: String?, deeplink: Boolean) {
    }



    override fun viewRoom(room: RoomListResponseItem, view: View) {

    }

    override fun itemClick(userId: String) {
    }
}