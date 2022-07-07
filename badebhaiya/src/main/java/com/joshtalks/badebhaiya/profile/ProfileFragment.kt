package com.joshtalks.badebhaiya.profile

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import androidx.core.view.updateLayoutParams
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.*
import com.joshtalks.badebhaiya.core.USER_ID
import com.joshtalks.badebhaiya.core.models.FormResponse
import com.joshtalks.badebhaiya.core.models.PendingPilotEvent
import com.joshtalks.badebhaiya.core.models.PendingPilotEvent.*
import com.joshtalks.badebhaiya.core.models.PendingPilotEventData
import com.joshtalks.badebhaiya.databinding.FragmentProfileBinding
import com.joshtalks.badebhaiya.databinding.WhyRoomBinding
import com.joshtalks.badebhaiya.feed.*
import com.joshtalks.badebhaiya.feed.adapter.FeedAdapter
import com.joshtalks.badebhaiya.feed.model.RoomListResponseItem
import com.joshtalks.badebhaiya.impressions.Impression
import com.joshtalks.badebhaiya.liveroom.LiveRoomState
import com.joshtalks.badebhaiya.liveroom.ROOM_EXPAND
import com.joshtalks.badebhaiya.liveroom.viewmodel.LiveRoomViewModel
import com.joshtalks.badebhaiya.notifications.NotificationScheduler
import com.joshtalks.badebhaiya.profile.request.ReminderRequest
import com.joshtalks.badebhaiya.profile.response.ProfileResponse
import com.joshtalks.badebhaiya.pubnub.PubNubState
import com.joshtalks.badebhaiya.repository.CommonRepository
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.signup.SignUpActivity
import com.joshtalks.badebhaiya.signup.viewmodel.SignUpViewModel
import com.joshtalks.badebhaiya.utils.SingleDataManager
import com.joshtalks.badebhaiya.utils.Utils
import com.joshtalks.badebhaiya.utils.setUserImageOrInitials
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_feed.*
import kotlinx.android.synthetic.main.base_toolbar.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlinx.coroutines.CoroutineScope
import java.lang.Exception
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment: Fragment(), Call, FeedAdapter.ConversationRoomItemCallback {

    private var isFromDeeplink = false
    private var isFromBBPage=false
    private lateinit var source:String

    private val liveRoomViewModel by lazy {
        ViewModelProvider(requireActivity())[LiveRoomViewModel::class.java]
    }

    private val viewModel by lazy {
        ViewModelProvider(this)[ProfileViewModel::class.java]
    }

    private val feedViewModel by lazy {
        ViewModelProvider(requireActivity())[FeedViewModel::class.java]
    }

    private val signUpViewModel by lazy {
        ViewModelProvider(requireActivity()).get(SignUpViewModel::class.java)
    }

    lateinit var binding: FragmentProfileBinding

    private var userId: String? = EMPTY

    @Inject
    lateinit var notificationScheduler: NotificationScheduler

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        try {
            (activity as FeedActivity).swipeRefreshLayout.isEnabled=false
        } catch (e: Exception){

        }
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_profile, container, false)
        super.onCreate(savedInstanceState)
        var mBundle: Bundle? = Bundle()
        mBundle = this.arguments
        userId=mBundle!!.getString("user")
        isFromDeeplink=mBundle!!.getBoolean("deeplink")
        source= mBundle.getString("source").toString()
        Log.i("IMPRESSION", "onCreateView: $source")
        isFromBBPage= mBundle.getBoolean("BBPage")
        source= mBundle.getString("source").toString()

        handleIntent()
        viewModel.getProfileForUser(userId!!, source)
        feedViewModel.setIsBadeBhaiyaSpeaker()
        binding.handler = this
        binding.viewModel = viewModel

        binding.profileToolbar.iv_back.setOnClickListener{
            activity?.run {
                try {
                    (activity as FeedActivity).swipeRefreshLayout.isEnabled=true
                } catch (e: Exception){

                }
                onBackPressed()
            }
        }
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                activity?.run {
                    if (this is FeedActivity){
                        Timber.d("back from profile and is feed activity")

                        try {
                            (activity as FeedActivity).swipeRefreshLayout.isEnabled=true
                        } catch (e: Exception){

                        }
                        supportFragmentManager.beginTransaction().remove(this@ProfileFragment)
                            .commitAllowingStateLoss()
                    } else  {
                        Timber.d("back from profile")
                        supportFragmentManager.popBackStack()
                    }
                }
            }
        })
        return binding.root

    }

    fun setpadding(){
        binding.rvSpeakerRoomList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (recyclerView.canScrollVertically(1).not()) {
                    recyclerView.setPadding(
                        resources.getDimension(R.dimen._8sdp).toInt(), 0,
                        resources.getDimension(R.dimen._8sdp).toInt(), binding.view.height
                    )
                }
            }
        })
        binding.view.visibility=View.VISIBLE
    }

    fun unsetPadding(){
        binding.view.updateLayoutParams { height=1 }
        binding.rvSpeakerRoomList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (recyclerView.canScrollVertically(1).not()) {
                    recyclerView.setPadding(
                        resources.getDimension(R.dimen._8sdp).toInt(), 0,
                        resources.getDimension(R.dimen._8sdp).toInt(), binding.view.height
                    )
                }
            }
        })
    }

    fun showPopup(roomId: Int, userId: String) {
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        val dialogBinding = WhyRoomBinding.inflate(layoutInflater)
        dialogBuilder.setView(dialogBinding.root)
        val alertDialog: AlertDialog = dialogBuilder.create()
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        alertDialog.show()

        dialogBinding.message.addTextChangedListener {
            if (it.toString().trim().isEmpty()){
                dialogBinding.Skip.isEnabled = true
                dialogBinding.submit.isEnabled = false
            } else {
                dialogBinding.Skip.isEnabled = false
                dialogBinding.submit.isEnabled = true
            }
        }

        dialogBinding.submit.setOnClickListener{
            val msg:String
            if(dialogBinding.message.toString().isNotBlank()) {
                msg = dialogBinding.message.text.toString()
                val obj=FormResponse(userId,msg,roomId)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val resp= CommonRepository().sendMsg(obj)
                        if(resp.isSuccessful)
                            showToast("response Send")
                    } catch (e: Exception){

                    }
                }
                alertDialog.dismiss()
            }
        }
        dialogBinding.Skip.setOnClickListener {
            alertDialog.dismiss()
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("THIS IS FCM TOKEN => ${PrefManager.getStringValue(com.joshtalks.badebhaiya.notifications.FCM_TOKEN)}")
        addObserver()
        executePendingActions()
    }

    private fun executePendingActions( ) {
        SingleDataManager.pendingPilotAction?.let {
            when(it){
                FOLLOW -> followPilot()
            }
        }
    }

    private fun followPilot() {
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
               binding.ivProfilePic.setUserImageOrInitials(it.profilePicUrl,it.firstName.get(0),30)
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

        feedViewModel.isBackPressed.observe(requireActivity()){
            Log.i("LIVEROOMSouRCE", "addObserver: ${feedViewModel.isBackPressed.value}")
            if(it==true) {
                try {
                    (activity as FeedActivity).swipeRefreshLayout.isEnabled=true
                } catch (e: Exception){

                }
                if (isAdded){
                    requireActivity().supportFragmentManager.beginTransaction().remove(this).commit()
                    feedViewModel.isBackPressed.value = false
                }
            }

        }
        liveRoomViewModel.deflate.observe(viewLifecycleOwner){
            if(it)
                setpadding()
            else
                unsetPadding()
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


        viewModel.updateFollowStatus(userId ?: (User.getInstance().userId),isFromBBPage,isFromDeeplink)
        if(viewModel.speakerFollowed.value == true)
            viewModel.userProfileData.value?.let {
                signUpViewModel.unfollowSpeaker()
                viewModel.sendEvent(Impression("PROFILE_SCREEN","CLICKED_UNFOLLOW"))
                speakerUnfollowedUIChanges()
                binding.tvFollowers.text =HtmlCompat.fromHtml(getString(R.string.bb_followers,
                    ("<big>"+it.followersCount.minus(1)?:0).toString()+"</big>"),
                    HtmlCompat.FROM_HTML_MODE_LEGACY)
            }
        else
            viewModel.userProfileData.value?.let {
                signUpViewModel.followSpeaker()
                viewModel.sendEvent(Impression("PROFILE_SCREEN","CLICKED_FOLLOW"))
                speakerFollowedUIChanges()
                binding.tvFollowers.text =HtmlCompat.fromHtml(getString(R.string.bb_followers,
                    ("<big>"+it.followersCount.plus(1)?:0).toString()+"</big>"),
                    HtmlCompat.FROM_HTML_MODE_LEGACY)
            }
        viewModel.getProfileForUser(userId ?: (User.getInstance().userId), source)
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
        const val TAG = "ProfileFragment"
        const val USER = "user"

        const val FROM_DEEPLINK = "from_deeplink"
        fun openProfileActivity(context: Context, userId: String = EMPTY) {
            Intent(context, ProfileFragment::class.java).apply {
                putExtra(USER_ID, userId)
            }.run {
                context.startActivity(this)
            }
        }

        fun open(supportFragmentManager: FragmentManager, @IdRes containerId: Int, userId: String){
            val fragment = ProfileFragment() // replace your custom fragment class

            val bundle = Bundle()
            bundle.putString(USER, userId)
            bundle.putString("source","Unknown")

            fragment.arguments = bundle

            supportFragmentManager
                .beginTransaction()
                .replace(containerId, fragment)
                .addToBackStack(TAG)
                .commit()
        }

        fun openOnTop(supportFragmentManager: FragmentManager, @IdRes containerId: Int, userId: String){
            val fragment = ProfileFragment() // replace your custom fragment class

            val bundle = Bundle()
            bundle.putString(USER, userId)
            bundle.putBoolean("BBPage", true)
            bundle.putString("source","BB_TO_FOLLOW")

            fragment.arguments = bundle

            supportFragmentManager
                .beginTransaction()
                .add(containerId, fragment)
                .addToBackStack(TAG)
                .commit()
        }
    }

    override fun joinRoom(room: RoomListResponseItem, view: View) {
        feedViewModel.source="Profile"
        takePermissions(room.roomId.toString(), room.topic.toString(), room.speakersData?.userId)
    }

    private fun takePermissions(room: String? = null, roomTopic: String, moderatorId: String?) {
        if (PermissionUtils.isCallingPermissionWithoutLocationEnabled(requireContext())) {
            if (room != null) {
                feedViewModel.joinRoom(room, roomTopic, "PROFILE_SCREEN")
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
                                feedViewModel.joinRoom(
                                    room,
                                    roomTopic,
                                    "PROFILE_SCREEN",
                                )
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
        viewModel.sendEvent(Impression("PROFILE_SCREEN", "CLICKED_JOIN"))
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
        showPopup(room.roomId,User.getInstance().userId)
        viewModel.sendEvent(Impression("PROFILE_SCREEN","CLICKED_SET_REMINDER"))
        notificationScheduler.scheduleNotificationAsListener(requireActivity() as AppCompatActivity, room)
            feedViewModel.setReminder(
                ReminderRequest(
                    roomId = room.roomId.toString(),
                    userId = User.getInstance().userId,
                    reminderTime = room.startTimeDate,
                    false
                )
            )
        viewModel.getProfileForUser(userId ?: (User.getInstance().userId),source)

    }

    override fun viewProfile(profile: String?, deeplink: Boolean) {
    }



    override fun viewRoom(room: RoomListResponseItem, view: View,deeplink: Boolean) {

    }

    override fun itemClick(userId: String) {
    }
}