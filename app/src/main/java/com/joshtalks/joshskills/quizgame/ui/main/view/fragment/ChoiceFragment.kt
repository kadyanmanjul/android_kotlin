package com.joshtalks.joshskills.quizgame.ui.main.view.fragment

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.databinding.FragmentChoiceFragnmentBinding
import com.joshtalks.joshskills.quizgame.StartActivity
import com.joshtalks.joshskills.quizgame.ui.data.model.AddFavouritePartner
import com.joshtalks.joshskills.quizgame.ui.data.network.FirebaseDatabase
import com.joshtalks.joshskills.quizgame.ui.data.network.FirebaseTemp
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.ChoiceViewModel
import com.joshtalks.joshskills.quizgame.util.*
import com.joshtalks.joshskills.repository.local.model.Mentor
import io.agora.rtc.Constants
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.models.ChannelMediaOptions
import kotlinx.android.synthetic.main.fragment_favourite_practice.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class ChoiceFragment : Fragment(), FirebaseTemp.OnNotificationTriggerTemp,
    P2pRtc.WebRtcEngineCallback,
    FirebaseDatabase.OnMakeFriendTrigger {

    val vm by lazy {
        ViewModelProvider(requireActivity())[ChoiceViewModel::class.java]
    }

    var isShowFrag = false
    private lateinit var binding: FragmentChoiceFragnmentBinding
    private var mentorId: String = Mentor.getInstance().getUserId()

    val handler5 = Handler(Looper.getMainLooper())
    val handler9 = Handler(Looper.getMainLooper())
    val handler2 = Handler(Looper.getMainLooper())
    val handler4 = Handler(Looper.getMainLooper())


    private var activityInstance: FragmentActivity? = null
    var userName: String? = Mentor.getInstance().getUser()?.firstName
    var imageUrl: String? = Mentor.getInstance().getUser()?.photo

    private var firebaseDatabase: FirebaseTemp = FirebaseTemp()

    //    private var REQUESTED_PERMISSIONS = arrayOf(
//        Manifest.permission.RECORD_AUDIO,
//        Manifest.permission.CAMERA
//    )
//    private val PERMISSION_REQ_ID = 22
    private var engine: RtcEngine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpViewModel()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_choice_fragnment,
                container,
                false
            )
        binding.vm = vm
        binding.clickHandler = this
        binding.executePendingBindings()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activityInstance = activity
        binding.container.setBackgroundColor(Color.WHITE);

        PrefManager.put(USER_LEAVE_THE_GAME, false)
        playSound(R.raw.compress_background_util_quiz)
        onBackPress()
        try {
            engine = P2pRtc().initEngine(requireActivity())
            P2pRtc().addListener(this)
        } catch (ex: Exception) {
            Timber.d(ex)
        }
        try {
            getFriendRequest()
            firebaseDatabase.getUserDataFromFirestore(mentorId, this)
        } catch (ex: Exception) {
            Timber.d(ex)
        }

        if (isAdded && activityInstance != null) {
            deleteData()
        } else {
            showToast("Crash")
        }
        if (isAdded && activityInstance != null) {
            getAcceptCall()
        } else {
            showToast("Crash")
        }
    }

    override fun onResume() {
        super.onResume()
        isShowFrag = true
    }

    fun deleteData() {
        if (context?.let { UpdateReceiver.isNetworkAvailable(it) } == true)
            firebaseDatabase.getDeclineCall(mentorId, this)
    }

    fun getAcceptCall() {
        if (context?.let { UpdateReceiver.isNetworkAvailable(it) } == true)
            firebaseDatabase.getAcceptCall(mentorId, this)
    }

    fun playSound(sound: Int) {
        if (activity?.application?.let { AudioManagerQuiz.audioRecording.isPlaying() } != true) {
            activity?.application?.let {
                AudioManagerQuiz.audioRecording.startPlaying(
                    it,
                    sound,
                    true
                )
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            ChoiceFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }

    fun onBackPress() {
        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    CustomDialogQuiz(requireActivity()).showDialog(::positiveBtnAction)
                }
            })
    }

    fun positiveBtnAction() {
        vm.homeInactive(mentorId, IN_ACTIVE)
        try {
            vm.homeInactiveResponse.observe(this, {
                if (it.message == CHANGE_USER_STATUS) {
                    requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    PrefManager.put(USER_ACTIVE_IN_GAME, false)
                    AudioManagerQuiz.audioRecording.stopPlaying()
                    firebaseDatabase.deleteRequested(mentorId)
                    firebaseDatabase.deleteDeclineData(mentorId)
                    moveToNewActivity()
                }
            })
        } catch (ex: Exception) {
            Timber.d(ex)
        }
    }

    fun setUpViewModel() {
        try {
            vm.statusChange(mentorId, ACTIVE)
        } catch (ex: Exception) {

        }
    }

    private fun moveToNewActivity() {
        val i = Intent(activity, StartActivity::class.java)
        startActivity(i)
        (activity as Activity?)?.overridePendingTransition(0, 0)
        requireActivity().finish()
    }

    fun initializeAgoraCall(channelName: String) {
        // Check permission
        //if (checkSelfPermiss ion(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID)) {
        CoroutineScope(Dispatchers.IO).launch {
            joinChannel(channelName)
            //   }
            //WebRtcEngine.initLibrary()
        }
    }

//    private fun checkSelfPermission(permission: String, requestCode: Int): Boolean {
//        if (activity?.let { ContextCompat.checkSelfPermission(it, permission) } !=
//            PackageManager.PERMISSION_GRANTED
//        ) {
//            activity?.let {
//                ActivityCompat.requestPermissions(
//                    it,
//                    REQUESTED_PERMISSIONS,
//                    requestCode
//                )
//            }
//            return false
//        }
//        return true
//    }

    private fun joinChannel(channelId: String) {
        engine?.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
        engine?.setClientRole(IRtcEngineEventHandler.ClientRole.CLIENT_ROLE_BROADCASTER)
        var accessToken: String? =
            "006569a477f372a454b8101fc89ec6161e6IADYmZ3FSiuZkvYIVERzwHjKvcLGDMAC5LxQ8lh7CxFfjhQWp0e379yDIgAkhwQAweFmYQQAAQBB4YpjAgBB4YpjAwBB4YpjBABB4Ypj"
        if (TextUtils.equals(accessToken, "") || TextUtils.equals(
                accessToken,
                "006569a477f372a454b8101fc89ec6161e6IADYmZ3FSiuZkvYIVERzwHjKvcLGDMAC5LxQ8lh7CxFfjhQWp0e379yDIgAkhwQAweFmYQQAAQBB4YpjAgBB4YpjAwBB4YpjBABB4Ypj"
            )
        ) {
            accessToken = null
        }
        engine?.enableAudioVolumeIndication(1000, 3, true)
        val option = ChannelMediaOptions()
        option.autoSubscribeAudio = true
        option.autoSubscribeVideo = true
        val res = engine?.joinChannel(accessToken, channelId, "Extra Optional Data", 0, option)
        if (res != 0) {
            return
        }
    }

    override fun onNotificationForInvitePartnerTemp(
        channelName: String,
        fromUserId: String,
        fromUserName: String,
        fromUserImage: String
    ) {
        var i = 0
        handler5.removeCallbacksAndMessages(null)
        try {
            visibleView(binding.notificationCard)

            binding.progress.animateProgress()
            binding.userName.text = fromUserName
            val imageUrl = fromUserImage.replace("\n", "")

            binding.userImage.setUserImageOrInitials(imageUrl, fromUserName, 30, isRound = true)
        } catch (ex: Exception) {
            Timber.d(ex)
        }

        binding.buttonAccept.setOnClickListener {
            handler5.removeCallbacksAndMessages(null)
            i = 1
            invisibleView(binding.notificationCard)
            vm.getChannelData(mentorId, channelName)
            activity?.let {
                vm.agoraToToken.observe(it, {
                    when {
                        it?.message.equals(TEAM_CREATED) -> {
                            firebaseDatabase.deleteRequested(mentorId)
                            firebaseDatabase.acceptRequest(
                                fromUserId,
                                TRUE,
                                fromUserName,
                                channelName,
                                mentorId
                            )
                            initializeAgoraCall(channelName)
                            moveFragment(fromUserId, channelName)
                        }
                        it?.message.equals(USER_ALREADY_JOIN) -> {
                            binding.userImageForAlready.setUserImageOrInitials(
                                imageUrl,
                                fromUserName,
                                30,
                                isRound = true
                            )
                            binding.userNameForAlready.text = fromUserName
                            visibleView(binding.notificationCardAlready)
                        }
                        it?.message.equals(USER_LEFT_THE_GAME) -> {
                            showToast(PARTNER_LEFT_THE_GAME)
                        }
                    }
                })
            }
        }

        binding.alreadyNotification.setOnClickListener {
            handler4.removeCallbacksAndMessages(null)
            invisibleView(binding.notificationCardAlready)
        }
        binding.butonDecline.setOnClickListener {
            invisibleView(binding.notificationCard)
            handler5.removeCallbacksAndMessages(null)
            firebaseDatabase.createRequestDecline(fromUserId, userName, imageUrl, mentorId)
            mentorId.let { it1 -> firebaseDatabase.deleteUserData(it1, fromUserId) }
        }

        binding.eee.setOnClickListener {
            invisibleView(binding.notificationCard)
            handler5.removeCallbacksAndMessages(null)
            firebaseDatabase.createRequestDecline(fromUserId, userName, imageUrl, mentorId)
            mentorId.let { it1 -> firebaseDatabase.deleteUserData(it1, fromUserId) }
        }

        try {
            handler4.postDelayed({
                invisibleView(binding.notificationCardAlready)
            }, 10000)
        } catch (ex: Exception) {
        }
        try {
            if (isShowFrag){
                handler5.postDelayed({
                    invisibleView(binding.notificationCard)
                    mentorId.let { it1 -> firebaseDatabase.deleteUserData(it1, fromUserId) }
                    firebaseDatabase.createRequestDecline(fromUserId, userName, imageUrl, mentorId)
                }, 10000)
            }
        } catch (ex: Exception) {
        }
    }

    override fun onNotificationForPartnerNotAcceptTemp(
        userName: String?,
        userImageUrl: String,
        fromUserId: String,
        declinedUserId: String
    ) {
        handler9.removeCallbacksAndMessages(null)
        val image = userImageUrl.replace("\n", "")
        visibleView(binding.notificationCardNotPlay)
        binding.userNameForNotPlay.text = userName
        binding.userImageForNotPaly.setUserImageOrInitials(
            image,
            userName ?: "",
            30,
            isRound = true
        )
        binding.cancelNotification.setOnClickListener {
            handler9.removeCallbacksAndMessages(null)
            firebaseDatabase.deleteDeclineData(mentorId)
            invisibleView(binding.notificationCardNotPlay)
        }

        try {
            if (isShowFrag){
                handler9.postDelayed({
                    firebaseDatabase.deleteDeclineData(mentorId)
                    invisibleView(binding.notificationCardNotPlay)
                }, 10000)
            }
        } catch (ex: Exception) {
        }
    }

    override fun onNotificationForPartnerAcceptTemp(
        channelName: String?,
        timeStamp: String,
        isAccept: String,
        opponentMemberId: String,
        mentorId: String
    ) {
        if (isAccept == TRUE) {
            firebaseDatabase.deleteDataAcceptRequest(opponentMemberId)
            firebaseDatabase.deleteRequested(mentorId)
            channelName?.let { initializeAgoraCall(it) }
            moveFragment(mentorId, channelName)
        }
    }

    fun visibleView(viewVisible: View) {
        viewVisible.visibility = View.VISIBLE
    }

    fun invisibleView(viewInvisible: View) {
        viewInvisible.visibility = View.INVISIBLE
    }

    fun moveFragment(userId: String?, channelName: String?) {
        val fm = activity?.supportFragmentManager
        fm?.beginTransaction()
            ?.replace(
                R.id.container,
                TeamMateFoundFragnment.newInstance(userId ?: "", channelName ?: ""),
                "TeamMateFoundFragnment"
            )
            ?.remove(this)
            ?.commit()
    }

    override fun onSentFriendRequest(
        fromMentorId: String,
        fromUserName: String,
        fromImageUrl: String,
        isAccept: String
    ) {
        try {
            handler2.removeCallbacksAndMessages(null)
            visibleView(binding.notificationCard)
            binding.progress.animateProgress()
            binding.userName.text = fromUserName
            val imageUrl = fromImageUrl.replace("\n", "")
            binding.userImage.setUserImageOrInitials(imageUrl, fromUserName, 30, isRound = true)
        } catch (ex: Exception) {
            Timber.d(ex)
        }

        binding.buttonAccept.setOnClickListener {
            invisibleView(binding.notificationCard)
            handler2.removeCallbacksAndMessages(null)
            vm.addFavouritePracticePartner(AddFavouritePartner(fromMentorId, mentorId))
            firebaseDatabase.deleteRequest(mentorId)
        }

        binding.butonDecline.setOnClickListener {
            invisibleView(binding.notificationCard)
            handler2.removeCallbacksAndMessages(null)
            firebaseDatabase.deleteRequested(mentorId)
        }

        try {
            handler2.postDelayed({
                invisibleView(binding.notificationCard)
                firebaseDatabase.deleteRequested(mentorId)
            }, 10000)
        } catch (ex: Exception) {
        }
    }

    fun getFriendRequest() {
        firebaseDatabase.getFriendRequests(mentorId, this)
    }

    override fun onPlayAgainNotificationFromApi(userName: String, userImage: String) {
        TODO("Not yet implemented")
    }

    override fun onPartnerPlayAgainNotification(
        userName: String,
        userImage: String,
        mentorId: String
    ) {
        TODO("Not yet implemented")
    }

    fun openFavouriteScreen() {
        val fm = activity?.supportFragmentManager
        fm?.beginTransaction()
            ?.replace(
                R.id.container,
                FavouritePartnerFragment.newInstance(), FAVOURITE_FRAGMENT
            )
            ?.remove(this)
            ?.commit()
    }

    fun openRandomScreen() {
        val fm = activity?.supportFragmentManager
        fm?.beginTransaction()
            ?.replace(
                R.id.container,
                RandomPartnerFragment.newInstance(), RANDOM_PARTNER_FRAGMENT
            )
            ?.remove(this)
            ?.commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        isShowFrag = false
        handler2.removeCallbacksAndMessages(null)
        handler4.removeCallbacksAndMessages(null)
        handler5.removeCallbacksAndMessages(null)
        handler9.removeCallbacksAndMessages(null)
    }
}