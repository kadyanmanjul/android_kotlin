package com.joshtalks.joshskills.quizgame.ui.main.view.fragment

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.lottie.LottieDrawable
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.setUserImageOrInitials
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.FragmentFavouritePracticeBinding
import com.joshtalks.joshskills.quizgame.analytics.GameAnalytics
import com.joshtalks.joshskills.quizgame.ui.data.model.AddFavouritePartner
import com.joshtalks.joshskills.quizgame.ui.data.model.ChannelData
import com.joshtalks.joshskills.quizgame.ui.data.model.Favourite
import com.joshtalks.joshskills.quizgame.ui.data.network.GameFirebaseDatabase
import com.joshtalks.joshskills.quizgame.ui.data.network.GameNotificationFirebaseData
import com.joshtalks.joshskills.quizgame.ui.main.adapter.FavouriteAdapter
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.FavouriteViewModelGame
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.FppListViewProviderFactory
import com.joshtalks.joshskills.quizgame.util.*
import com.joshtalks.joshskills.repository.local.model.Mentor
import io.agora.rtc.Constants
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.models.ChannelMediaOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber


class GameFavouritePartnerFragmentFpp : Fragment(), FavouriteAdapter.QuizBaseInterface,
    GameNotificationFirebaseData.OnNotificationTriggerTemp,
    P2pRtc.WebRtcEngineCallback, GameFirebaseDatabase.OnMakeFriendTrigger,
    GameFirebaseDatabase.OnLiveStatus {

    private lateinit var binding: FragmentFavouritePracticeBinding
    private var favouriteAdapter: FavouriteAdapter? = null

    private var factory: FppListViewProviderFactory? = null
    private var favouriteViewModel: FavouriteViewModelGame? = null
    private var firebaseDatabase: GameNotificationFirebaseData = GameNotificationFirebaseData()
    private var channelName: String? = null
    private var fromTokenId: String? = null
    private var fromUserId: String? = null
    private var favouriteUserId: String? = null

    private var engine: RtcEngine? = null
    private var activityInstance: FragmentActivity? = null

    private var mentorId: String = Mentor.getInstance().getId()
    private var userName: String? = Mentor.getInstance().getUser()?.firstName?: EMPTY
    private var imageUrl: String? = Mentor.getInstance().getUser()?.photo?: EMPTY
    private val handler = Handler(Looper.getMainLooper())
    private val handler1 = Handler(Looper.getMainLooper())
    private val handler2 = Handler(Looper.getMainLooper())
    private val handler4 = Handler(Looper.getMainLooper())

    private var isActiveFrag = false

    private var arrayList: ArrayList<Favourite>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupViewModel()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_favourite_practice,
                container,
                false
            )
        binding.lifecycleOwner = this
        binding.clickHandler = this
        favouriteAdapter = activity?.applicationContext?.let {
            FavouriteAdapter(
                it,
                ArrayList(),
                this,
                firebaseDatabase
            )
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.container.setBackgroundColor(Color.WHITE)

        activityInstance = activity

        try {
            engine = P2pRtc().initEngine(requireActivity())
            P2pRtc().addListener(this)
        } catch (ex: Exception) {
            Timber.d(ex)
        }

        try {
            getFavouritePracticePartner()
            getFromAgoraToken()
            getFriendRequest()
        } catch (ex: Exception) {
            Timber.d(ex)
        }

        searchFavouritePartner()
        try {
            firebaseDatabase.getUserDataFromFirestore(
                mentorId,
                this
            )
        } catch (ex: Exception) {
            Timber.d(ex)
        }

        if (isAdded && activityInstance != null) {
            deleteData()
        }
        if (isAdded && activityInstance != null) {
            getAcceptCall()
        }
        binding.progress.animateProgress()
        onBackPress()
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            GameFavouritePartnerFragmentFpp().apply {
                arguments = Bundle().apply {

                }
            }
    }

    private fun setupViewModel() {
        try {
            factory = FppListViewProviderFactory(requireActivity().application)
            favouriteViewModel = ViewModelProvider(this, factory!!).get(FavouriteViewModelGame::class.java)

            favouriteViewModel?.fetchFav(mentorId)
        } catch (ex: Exception) {
        }
    }

    private fun initRV(favouriteList: ArrayList<Favourite>?) {
        arrayList = favouriteList
        val activeList: ArrayList<Favourite> = java.util.ArrayList()
        val inGameList: ArrayList<Favourite> = java.util.ArrayList()
        val searchList: ArrayList<Favourite> = java.util.ArrayList()
        val inActiveList: ArrayList<Favourite> = java.util.ArrayList()
        val list: ArrayList<Favourite> = java.util.ArrayList()
        if (favouriteList != null) {
            for (f in favouriteList) {
                if (f.status == ACTIVE) {
                    activeList.add(f)
                }
            }
            for (f1 in favouriteList) {
                if (f1.status == IN_GAME) {
                    inGameList.add(f1)
                }
            }
            for (f2 in favouriteList) {
                if (f2.status == SEARCHING) {
                    searchList.add(f2)
                }
            }
            for (f3 in favouriteList) {
                if (f3.status == IN_ACTIVE) {
                    inActiveList.add(f3)
                }
            }
            list.addAll(activeList)
            list.addAll(inGameList)
            list.addAll(searchList)
            list.addAll(inActiveList)

            arrayList = list
        }

        favouriteAdapter?.addItems(list)
        binding.recycleView.setHasFixedSize(true)
        binding.recycleView.layoutManager = LinearLayoutManager(activity)
        favouriteAdapter = activity?.let { FavouriteAdapter(it, arrayList, this, firebaseDatabase) }
        binding.recycleView.adapter = favouriteAdapter
        for (v in 0 until arrayList?.size!!) {
            firebaseDatabase.statusLive(arrayList?.get(v)?.uuid ?: EMPTY, this)
        }
    }

    private fun fromAgoraToken(channelData: ChannelData?) {
        channelName = channelData?.channelName
        fromTokenId = channelData?.token
        fromUserId = channelData?.userUid
    }

    fun onBack() {
        positiveBtnAction()
    }

    override fun onClickForGetToken(favourite: Favourite?, position: String) {
        favouriteUserId = favourite?.uuid
        favouriteUserId.let {
            firebaseDatabase.createRequest(
                favouriteUserId,
                channelName,
                mentorId
            )
        }
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

    fun deleteData() {
        if (UpdateReceiver.isNetworkAvailable())
            firebaseDatabase.getDeclineCall(mentorId, this)
    }

    fun visibleView(viewVisible: View) {
        binding.img.visibility = View.INVISIBLE
        viewVisible.visibility = View.VISIBLE
    }

    fun invisibleView(viewInvisible: View) {
        binding.img.visibility = View.VISIBLE
        viewInvisible.visibility = View.INVISIBLE
    }

    fun getAcceptCall() {
        if (UpdateReceiver.isNetworkAvailable())
            firebaseDatabase.getAcceptCall(mentorId, this)
    }

    private fun getFavouritePracticePartner() {
        activity?.let {
            try {
                favouriteViewModel?.favData?.observe(it, {
                    if (it.data != null) {
                        if (it.data?.size!! > 0) {
                            initRV(it.data)
                        }
                    }else{
                        lifecycleScope.launch(Dispatchers.Main) {
                            UtilsQuiz.showSnackBar(
                                binding.container,
                                Snackbar.LENGTH_LONG,
                                NO_FPP_FOUND
                            )
                        }
                    }
                })
            } catch (ex: Exception) {

            }
        }
    }

    private fun getFromAgoraToken() {
        activity?.let {
            try {
                favouriteViewModel?.fromTokenData?.observe(it, {
                    fromAgoraToken(it)
                })
            } catch (ex: Exception) {
                Timber.d(ex)
            }
        }
    }

    fun moveFragment(userId: String?, channelName: String?) {
        val fm = activity?.supportFragmentManager
        fm?.beginTransaction()
            ?.replace(
                R.id.container,
                TeamMateFoundFragmentFpp.newInstance(userId ?: EMPTY, channelName ?: EMPTY),
                "FavouritePartnerFragment"
            )
            ?.remove(this)
            ?.commit()
    }

    private fun onBackPress() {
        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    CustomDialogQuiz(requireActivity()).showDialog(::positiveBtnAction)
                }
            })
    }

    fun positiveBtnAction() {
        AudioManagerQuiz.audioRecording.stopPlaying()
        openChoiceScreen()
    }

    private fun openChoiceScreen() {
        val fm = activity?.supportFragmentManager
        fm?.popBackStackImmediate()
        fm?.beginTransaction()
            ?.replace(
                R.id.container,
                ChoiceFragment.newInstance(), "Favourite"
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
            if (isActiveFrag)
                CustomDialogQuiz(requireActivity()).scaleAnimationForNotification(binding.notificationCard)
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
            if (isActiveFrag)
                CustomDialogQuiz(requireActivity()).scaleAnimationForNotificationUpper(binding.notificationCard)
            favouriteViewModel?.addFavouritePracticePartner(
                AddFavouritePartner(
                    fromMentorId,
                    mentorId,
                    mentorId
                )
            )
            activity?.let {
                favouriteViewModel?.fppData?.observe(it, {
                    firebaseDatabase.deleteRequest(mentorId)
                })
            }
        }

        binding.butonDecline.setOnClickListener {
            GameAnalytics.push(GameAnalytics.Event.CLICK_ON_DECLINE_BUTTON)
            tickSound()
            if (isActiveFrag)
                CustomDialogQuiz(requireActivity()).scaleAnimationForNotificationUpper(binding.notificationCard)
            handler2.removeCallbacksAndMessages(null)
            firebaseDatabase.deleteRequest(mentorId)
        }
        try {
            handler2.postDelayed({
                if (isActiveFrag)
                    CustomDialogQuiz(requireActivity()).scaleAnimationForNotificationUpper(binding.notificationCard)
                firebaseDatabase.deleteRequest(mentorId)
            }, 10000)
        } catch (ex: Exception) {
        }

    }

    fun getFriendRequest() {
        firebaseDatabase.getFriendRequests(mentorId, this)
    }

    override fun onPlayAgainNotificationFromApi(userName: String, userImage: String) {

    }

    override fun onPartnerPlayAgainNotification(
        userName: String,
        userImage: String,
        mentorId: String
    ) {

    }

    override fun onPartnerAcceptFriendRequest(userName: String, userImage: String,isAccept: String) {
        //TODO("Not yet implemented")
    }

    fun searchFavouritePartner() {
        binding.inputSearch.setRawInputType(InputType.TYPE_CLASS_TEXT)
        binding.inputSearch.setTextIsSelectable(true)
        binding.inputSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s != null)
                    filter(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {
            }

        })
    }

    fun filter(text: String) {
        val prefixString = text.lowercase()
        val temp: ArrayList<Favourite> = ArrayList()

        if (prefixString.isEmpty()) {
            arrayList?.let { temp.addAll(it) }
        } else {
            if (arrayList != null) {
                for (wp in arrayList!!) {
                    if (wp.name?.lowercase()?.contains(prefixString) == true) {
                        temp.add(wp)
                    }
                }
            }
        }
        //check if length is  < 0 so print toast No data Found
        if (temp.size <= 0) {
            lifecycleScope.launch(Dispatchers.Main) {
                UtilsQuiz.showSnackBar(
                    binding.container,
                    Snackbar.LENGTH_SHORT,
                    NO_MATCHING_USER_FOUND
                )
            }
        }
        favouriteAdapter?.updateListAfterSearch(temp, text)
    }

    override fun onGetLiveStatus(status: String, mentorId: String) {
        try {
            val pos: Int = favouriteAdapter?.getPositionById(mentorId) ?: 0
            val holder: FavouriteAdapter.FavViewHolder =
                binding.recycleView.findViewHolderForAdapterPosition(pos) as FavouriteAdapter.FavViewHolder
            holder.binding.status.text = status
            when (status) {
                IN_ACTIVE -> {
                    arrayList?.get(pos)?.status = IN_ACTIVE
                    holder.binding.clickToken.visibility = View.INVISIBLE
                }
                ACTIVE -> {
                    arrayList?.get(pos)?.status = ACTIVE
                    holder.binding.clickToken.visibility = View.VISIBLE
                    holder.binding.clickToken.setImageResource(R.drawable.ic_plus1)
                    holder.binding.clickToken.setOnClickListener {
                        if (UpdateReceiver.isNetworkAvailable()) {
                            GameAnalytics.push(GameAnalytics.Event.CLICK_ON_INVITE)
                            tickSound()
                            holder.binding.clickToken.speed = 1.5F
                            holder.binding.clickToken.repeatCount = LottieDrawable.INFINITE
                            holder.binding.clickToken.setAnimation("lottie/hourglass_anim.json")
                            holder.binding.clickToken.playAnimation()
                            holder.binding.clickToken.isEnabled = false
                            onClickForGetToken(arrayList?.get(pos), pos.toString())
                        } else {
                            showToast("Seems like your Internet is too slow or not available.")
                        }
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(10000)
                            if (isActiveFrag) {
                                holder.binding.clickToken.setImageResource(R.drawable.ic_plus1)
                                holder.binding.clickToken.isEnabled = true
                            }
                        }
                    }
                    holder.binding.clickToken.isEnabled = true
                }
                IN_GAME -> {
                    arrayList?.get(pos)?.status = IN_GAME
                    holder.binding.clickToken.visibility = View.INVISIBLE
                }
                SEARCHING -> {
                    arrayList?.get(pos)?.status = SEARCHING
                    holder.binding.clickToken.visibility = View.INVISIBLE
                }
            }
        } catch (ex: Exception) {

        }
    }

    override fun onResume() {
        super.onResume()
        isActiveFrag = true
    }

    override fun onDestroy() {
        super.onDestroy()
        isActiveFrag = false
    }

    override fun onNotificationForInvitePartnerTemp(
        channelName: String,
        fromUserId: String,
        fromUserName: String,
        fromUserImage: String,
        mentorId: String
    ) {
        try {
            if (this.mentorId == mentorId){
                handler.removeCallbacksAndMessages(null)
                firebaseDatabase.deleteUserData(mentorId)
                if (isActiveFrag)
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
            handler.removeCallbacksAndMessages(null)
            if (isActiveFrag)
                CustomDialogQuiz(requireActivity()).scaleAnimationForNotificationUpper(binding.notificationCard)
            favouriteViewModel?.getChannelData(mentorId, channelName)
            activity?.let {
                favouriteViewModel?.agoraToToken?.observe(it, {
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
                            binding.userNameForAlready.text = fromUserName
                            if (isActiveFrag)
                                CustomDialogQuiz(requireActivity()).scaleAnimationForNotification(
                                    binding.notificationCardAlready
                                )
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
            if (isActiveFrag)
                CustomDialogQuiz(requireActivity()).scaleAnimationForNotificationUpper(binding.notificationCardAlready)
        }
        binding.butonDecline.setOnClickListener {
            GameAnalytics.push(GameAnalytics.Event.CLICK_ON_DECLINE_BUTTON)
            tickSound()
            handler.removeCallbacksAndMessages(null)
            if (isActiveFrag)
                CustomDialogQuiz(requireActivity()).scaleAnimationForNotificationUpper(binding.notificationCard)
            mentorId.let { it1 -> firebaseDatabase.deleteUserData(it1) }
            firebaseDatabase.createRequestDecline(fromUserId, userName, imageUrl, mentorId)
        }

        binding.eee.setOnClickListener {
            tickSound()
            if (isActiveFrag)
                CustomDialogQuiz(requireActivity()).scaleAnimationForNotificationUpper(binding.notificationCard)
            handler.removeCallbacksAndMessages(null)
            mentorId.let { it1 -> firebaseDatabase.deleteUserData(it1) }
            firebaseDatabase.createRequestDecline(fromUserId, userName, imageUrl, mentorId)
        }
//        try {
//            handler4.postDelayed({
//                UtilsQuiz.scaleAnimationForNotificationUpper(binding.notificationCardAlready)
//                //invisibleView(binding.notificationCardAlready)
//            }, 10000)
//        } catch (ex: Exception) { }

        try {
            if (isActiveFrag) {
                handler.postDelayed({
                    if (isActiveFrag)
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
        try {
            handler1.removeCallbacksAndMessages(null)
            val pos = favouriteAdapter?.getPositionById(declinedUserId)
            val holder: FavouriteAdapter.FavViewHolder =
                binding.recycleView.findViewHolderForAdapterPosition(
                    pos ?: 0
                ) as FavouriteAdapter.FavViewHolder
            if (fromUserId == mentorId) {
                try {
                    firebaseDatabase.deleteDeclineData(mentorId)
                    holder.binding.clickToken.setImageResource(R.drawable.ic_plus1)
                    holder.binding.clickToken.isEnabled = true
                    val image = userImageUrl.replace("\n", EMPTY)
                    if (isActiveFrag)
                        CustomDialogQuiz(requireActivity()).scaleAnimationForNotification(binding.notificationCardNotPlay)
                    binding.userNameForNotPlay.text = userName
                    binding.userImageForNotPaly.setUserImageOrInitials(
                        image,
                        userName ?: EMPTY,
                        30,
                        isRound = true
                    )

                } catch (ex: Exception) {
                    Timber.d(ex)

                }
            }
        } catch (ex: Exception) {
        }

        binding.cancelNotification.setOnClickListener {
            tickSound()
            handler1.removeCallbacksAndMessages(null)
            firebaseDatabase.deleteDeclineData(mentorId)
            if (isActiveFrag)
                CustomDialogQuiz(requireActivity()).scaleAnimationForNotificationUpper(binding.notificationCardNotPlay)
        }

        try {
            if (isActiveFrag) {
                handler1.postDelayed({
                    firebaseDatabase.deleteDeclineData(mentorId)
                    if (isActiveFrag)
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

    fun tickSound() {
        AudioManagerQuiz.audioRecording.tickPlaying(requireActivity())
    }
}