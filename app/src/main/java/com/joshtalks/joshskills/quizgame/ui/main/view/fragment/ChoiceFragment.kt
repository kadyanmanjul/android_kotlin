package com.joshtalks.joshskills.quizgame.ui.main.view.fragment

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.databinding.FragmentChoiceFragnmentBinding
import com.joshtalks.joshskills.quizgame.StartActivity
import com.joshtalks.joshskills.quizgame.analytics.GameAnalytics
import com.joshtalks.joshskills.quizgame.ui.data.model.AddFavouritePartner
import com.joshtalks.joshskills.quizgame.ui.data.network.GameFirebaseDatabase
import com.joshtalks.joshskills.quizgame.ui.data.network.GameNotificationFirebaseData
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.ChoiceViewModelGame
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.ChoiceViewModelProviderFactory
import com.joshtalks.joshskills.quizgame.util.*
import com.joshtalks.joshskills.repository.local.model.Mentor
import io.agora.rtc.Constants
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.models.ChannelMediaOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class ChoiceFragment : Fragment(), GameNotificationFirebaseData.OnNotificationTriggerTemp,
    P2pRtc.WebRtcEngineCallback,
    GameFirebaseDatabase.OnMakeFriendTrigger {

    private var factory: ChoiceViewModelProviderFactory? = null
    private var choiceViewModel: ChoiceViewModelGame? = null

    var isShowFrag = false
    private lateinit var binding: FragmentChoiceFragnmentBinding
    private var mentorId: String = Mentor.getInstance().getId()

    val handler5 = Handler(Looper.getMainLooper())
    val handler9 = Handler(Looper.getMainLooper())
    val handler2 = Handler(Looper.getMainLooper())
    val handler4 = Handler(Looper.getMainLooper())


    private var activityInstance: FragmentActivity? = null
    var userName: String? = Mentor.getInstance().getUser()?.firstName?: EMPTY
    var imageUrl: String? = Mentor.getInstance().getUser()?.photo?: EMPTY
    private var firebaseDatabase: GameNotificationFirebaseData = GameNotificationFirebaseData()
    private var mainGameFirebaseDatabase: GameFirebaseDatabase = GameFirebaseDatabase()

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
        binding.vm = choiceViewModel
        binding.clickHandler = this
        binding.executePendingBindings()
        return binding.root
    }

    init {
        firebaseDatabase.deleteRequested(mentorId)
        firebaseDatabase.deleteDataAcceptRequest(mentorId)
        mainGameFirebaseDatabase.deleteAcceptFppRequestNotification(mentorId)
        firebaseDatabase.deleteDeclineData(mentorId)

        mainGameFirebaseDatabase.deleteMuteUnmute(mentorId)
        mainGameFirebaseDatabase.deleteAllData(mentorId)
        mainGameFirebaseDatabase.deleteRoomData(mentorId)
        mainGameFirebaseDatabase.deleteAnimUser(mentorId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activityInstance = activity
        binding.container.setBackgroundColor(Color.WHITE)

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
        }
        if (isAdded && activityInstance != null) {
            getAcceptCall()
        }
    }

    override fun onResume() {
        super.onResume()
        isShowFrag = true
    }

    fun deleteData() {
        if (UpdateReceiver.isNetworkAvailable())
            firebaseDatabase.getDeclineCall(mentorId, this)
    }

    fun getAcceptCall() {
        if (UpdateReceiver.isNetworkAvailable())
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

    fun tickSound() {
        AudioManagerQuiz.audioRecording.tickPlaying(requireActivity())
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
        choiceViewModel?.homeInactive(mentorId, IN_ACTIVE)
        try {
            choiceViewModel?.homeInactiveResponse?.observe(this, {
                if (it.message == CHANGE_USER_STATUS) {
                    requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    AudioManagerQuiz.audioRecording.stopPlaying()
                    firebaseDatabase.deleteRequested(mentorId)
                    firebaseDatabase.deleteDeclineData(mentorId)
                    firebaseDatabase.changeUserStatus(mentorId, IN_ACTIVE)
                    moveToNewActivity()
                }
            })
        } catch (ex: Exception) {
            Timber.d(ex)
        }
    }

    fun setUpViewModel() {
        try {
            factory = activity?.application?.let { ChoiceViewModelProviderFactory(it) }
            choiceViewModel = ViewModelProvider(this, factory!!).get(ChoiceViewModelGame::class.java)
            choiceViewModel?.statusChange(mentorId, ACTIVE)
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
        CoroutineScope(Dispatchers.IO).launch {
            joinChannel(channelName)
        }
    }

    private fun joinChannel(channelId: String) {
        engine?.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
        engine?.setClientRole(IRtcEngineEventHandler.ClientRole.CLIENT_ROLE_BROADCASTER)
        var accessToken: String? =
            "006569a477f372a454b8101fc89ec6161e6IADYmZ3FSiuZkvYIVERzwHjKvcLGDMAC5LxQ8lh7CxFfjhQWp0e379yDIgAkhwQAweFmYQQAAQBB4YpjAgBB4YpjAwBB4YpjBABB4Ypj"
        if (TextUtils.equals(accessToken, EMPTY) || TextUtils.equals(
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
        fromUserImage: String,
        mentorId: String
    ) {
        //handler5.removeCallbacksAndMessages(null)
        try {
            if (this.mentorId == mentorId) {
                handler5.removeCallbacksAndMessages(null)
                firebaseDatabase.deleteUserData(mentorId)
                if (isShowFrag)
                    CustomDialogQuiz(requireActivity()).scaleAnimationForNotification(binding.notificationCard)
                binding.progress.animateProgress()
                binding.userName.text = fromUserName
                val imageUrl = fromUserImage.replace("\n", EMPTY)
                binding.userImage.setUserImageOrInitials(imageUrl, fromUserName, 30, isRound = true)
            }
        } catch (ex: Exception) {
            Timber.d(ex)
        }

        binding.buttonAccept.setOnClickListener {
            firebaseDatabase.deleteUserData(mentorId)
            GameAnalytics.push(GameAnalytics.Event.CLICK_ON_ACCEPT_BUTTON)
            tickSound()
            handler5.removeCallbacksAndMessages(null)
            if (isShowFrag)
                CustomDialogQuiz(requireActivity()).scaleAnimationForNotificationUpper(binding.notificationCard)
            choiceViewModel?.getChannelData(mentorId, channelName)
            activity?.let {
                choiceViewModel?.agoraToToken?.observe(it, {
                    when {
                        it?.message.equals(TEAM_CREATED) -> {
                            firebaseDatabase.deleteRequested(mentorId)
                            firebaseDatabase.deleteDataAcceptRequest(mentorId)
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
                            if (isShowFrag)
                                CustomDialogQuiz(requireActivity()).scaleAnimationForNotification(
                                    binding.notificationCardAlready
                                )
                            binding.userNameForAlready.text = fromUserName
                            //visibleView(binding.notificationCardAlready)
                        }
                        it?.message.equals(USER_LEFT_THE_GAME) -> {
                            showToast(PARTNER_LEFT_THE_GAME)
                        }
                    }
                })
            }
        }

        binding.alreadyNotification.setOnClickListener {
            tickSound()
            handler4.removeCallbacksAndMessages(null)
            if (isShowFrag)
                CustomDialogQuiz(requireActivity()).scaleAnimationForNotificationUpper(binding.notificationCardAlready)
        }
        binding.butonDecline.setOnClickListener {
            GameAnalytics.push(GameAnalytics.Event.CLICK_ON_DECLINE_BUTTON)

            tickSound()
            if (isShowFrag)
                CustomDialogQuiz(requireActivity()).scaleAnimationForNotificationUpper(binding.notificationCard)
            handler5.removeCallbacksAndMessages(null)
            firebaseDatabase.createRequestDecline(fromUserId, userName, imageUrl, mentorId)
            mentorId.let { it1 -> firebaseDatabase.deleteUserData(it1) }
        }

        binding.ignoreNotification.setOnClickListener {
            GameAnalytics.push(GameAnalytics.Event.CLICK_ON_DECLINE_BUTTON)
            tickSound()
            if (isShowFrag)
                CustomDialogQuiz(requireActivity()).scaleAnimationForNotificationUpper(binding.notificationCard)
            handler5.removeCallbacksAndMessages(null)
            firebaseDatabase.createRequestDecline(fromUserId, userName, imageUrl, mentorId)
            mentorId.let { it1 -> firebaseDatabase.deleteUserData(it1) }
        }

//        try {
//            handler4.postDelayed({
//                CustomDialogQuiz(requireActivity()).scaleAnimationForNotificationUpper(binding.notificationCardAlready)
//                //invisibleView(binding.notificationCardAlready)
//            }, 10000)
//        } catch (ex: Exception) {
//        }
        try {
            if (isShowFrag) {
                handler5.postDelayed({
                    if (isShowFrag)
                        CustomDialogQuiz(requireActivity()).scaleAnimationForNotificationUpper(
                            binding.notificationCard
                        )
                    mentorId.let { it1 -> firebaseDatabase.deleteUserData(it1) }
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
        firebaseDatabase.deleteDeclineData(mentorId)
        val image = userImageUrl.replace("\n", EMPTY)
        if (isShowFrag)
            CustomDialogQuiz(requireActivity()).scaleAnimationForNotification(binding.notificationCardNotPlay)
        binding.userNameForNotPlay.text = userName
        binding.userImageForNotPaly.setUserImageOrInitials(
            image,
            userName ?: EMPTY,
            30,
            isRound = true
        )
        binding.cancelNotification.setOnClickListener {
            tickSound()
            handler9.removeCallbacksAndMessages(null)
            firebaseDatabase.deleteDeclineData(mentorId)
            if (isShowFrag)
                CustomDialogQuiz(requireActivity()).scaleAnimationForNotificationUpper(binding.notificationCardNotPlay)
        }

        try {
            if (isShowFrag) {
                handler9.postDelayed({
                    firebaseDatabase.deleteDeclineData(mentorId)
                    if (isShowFrag)
                        CustomDialogQuiz(requireActivity()).scaleAnimationForNotificationUpper(
                            binding.notificationCardNotPlay
                        )
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
                TeamMateFoundFragmentFpp.newInstance(userId ?: EMPTY, channelName ?: EMPTY),
                "TeamMateFoundFragment"
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
            if (isShowFrag)
                CustomDialogQuiz(requireActivity()).scaleAnimationForNotification(binding.notificationCard)
            binding.txtMsg2.text = getString(R.string.friend_request)
            binding.progress.animateProgress()
            binding.userName.text = fromUserName
            val imageUrl = fromImageUrl.replace("\n", EMPTY)
            binding.userImage.setUserImageOrInitials(imageUrl, fromUserName, 30, isRound = true)
        } catch (ex: Exception) {
            Timber.d(ex)
        }

        binding.buttonAccept.setOnClickListener {
            GameAnalytics.push(GameAnalytics.Event.CLICK_ON_ACCEPT_BUTTON)
            tickSound()
            if (isShowFrag)
                CustomDialogQuiz(requireActivity()).scaleAnimationForNotificationUpper(binding.notificationCard)
            handler2.removeCallbacksAndMessages(null)
            choiceViewModel?.addFavouritePracticePartner(AddFavouritePartner(fromMentorId, mentorId))
            firebaseDatabase.deleteRequest(mentorId)
        }

        binding.butonDecline.setOnClickListener {
            GameAnalytics.push(GameAnalytics.Event.CLICK_ON_DECLINE_BUTTON)
            tickSound()
            if (isShowFrag)
                CustomDialogQuiz(requireActivity()).scaleAnimationForNotificationUpper(binding.notificationCard)
            handler2.removeCallbacksAndMessages(null)
            firebaseDatabase.deleteRequest(mentorId)
        }

        binding.ignoreNotification.setOnClickListener {
            GameAnalytics.push(GameAnalytics.Event.CLICK_ON_DECLINE_BUTTON)
            tickSound()
            if (isShowFrag)
                CustomDialogQuiz(requireActivity()).scaleAnimationForNotificationUpper(binding.notificationCard)
            handler2.removeCallbacksAndMessages(null)
            firebaseDatabase.deleteRequest(mentorId)
        }

        try {
            handler2.postDelayed({
                if (isShowFrag)
                    CustomDialogQuiz(requireActivity()).scaleAnimationForNotificationUpper(binding.notificationCard)
                firebaseDatabase.deleteRequested(mentorId)
            }, 10000)
        } catch (ex: Exception) {
        }
    }

    fun getFriendRequest() {
        firebaseDatabase.getFriendRequests(mentorId, this)
    }

    override fun onPlayAgainNotificationFromApi(userName: String, userImage: String) {}

    override fun onPartnerPlayAgainNotification(
        userName: String,
        userImage: String,
        mentorId: String
    ) {
    }

    override fun onPartnerAcceptFriendRequest(userName: String, userImage: String,isAccept: String) {
       // TODO("Not yet implemented")
    }

    fun openFavouriteScreen() {
        tickSound()
        if (UpdateReceiver.isNetworkAvailable()) {
            GameAnalytics.push(GameAnalytics.Event.OPEN_FPP)
            val fm = activity?.supportFragmentManager
            fm?.beginTransaction()
                ?.replace(
                    R.id.container,
                    GameFavouritePartnerFragmentFpp.newInstance(), FAVOURITE_FRAGMENT
                )
                ?.remove(this)
                ?.commit()
        } else {
            showToast("Seems like your Internet is too slow or not available.")
        }
    }

    fun openRandomScreen() {
        tickSound()
        if (UpdateReceiver.isNetworkAvailable()) {
            GameAnalytics.push(GameAnalytics.Event.OPEN_RANDOM)
            val fm = activity?.supportFragmentManager
            fm?.beginTransaction()
                ?.replace(
                    R.id.container,
                    RandomPartnerFragment.newInstance(), RANDOM_PARTNER_FRAGMENT
                )
                ?.remove(this)
                ?.commit()
        } else {
            showToast("Seems like your Internet is too slow or not available.")
        }
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