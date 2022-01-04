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
import com.joshtalks.joshskills.core.setUserImageOrInitials
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.FragmentFavouritePracticeBinding
import com.joshtalks.joshskills.quizgame.ui.data.model.AddFavouritePartner
import com.joshtalks.joshskills.quizgame.ui.data.model.ChannelData
import com.joshtalks.joshskills.quizgame.ui.data.model.Favourite
import com.joshtalks.joshskills.quizgame.ui.data.network.FirebaseDatabase
import com.joshtalks.joshskills.quizgame.ui.data.network.FirebaseTemp
import com.joshtalks.joshskills.quizgame.ui.main.adapter.FavouriteAdapter
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.FavouriteViewModel
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

const val IN_ACTIVE: String = "Inactive"
const val ACTIVE: String = "Active"
const val IN_GAME: String = "In Game"
const val SEARCHING: String = "Searching"
const val USER_ALREADY_JOIN: String = "User has already joined the game"
const val USER_LEFT_THE_GAME: String = "User has left the game"
const val PARTNER_LEFT_THE_GAME: String = "Partner has left the game please try again"
const val TEAM_CREATED: String = "Team created successfully"
const val TRUE: String = "true"
const val FALSE: String = "false"
const val STATUS_CHANGE: String = "Status changed successfully"


class FavouritePartnerFragment : Fragment(), FavouriteAdapter.QuizBaseInterface,
    FirebaseTemp.OnNotificationTriggerTemp,
    P2pRtc.WebRtcEngineCallback, FirebaseDatabase.OnMakeFriendTrigger,
    FirebaseDatabase.OnLiveStatus {

    private lateinit var binding: FragmentFavouritePracticeBinding
    private var favouriteAdapter: FavouriteAdapter? = null
    private val favouriteViewModel by lazy {
        ViewModelProvider(requireActivity())[FavouriteViewModel::class.java]
    }
    private var firebaseDatabase: FirebaseTemp = FirebaseTemp()
    private var channelName: String? = null
    private var fromTokenId: String? = null
    private var fromUserId: String? = null
    private var favouriteUserId: String? = null

    //private val PERMISSION_REQ_ID = 22
    private var engine: RtcEngine? = null
    private var activityInstance: FragmentActivity? = null
    private var mentorId: String = Mentor.getInstance().getUserId()
    var userName: String? = Mentor.getInstance().getUser()?.firstName
    var imageUrl: String? = Mentor.getInstance().getUser()?.photo
    val handler = Handler(Looper.getMainLooper())
    val handler1 = Handler(Looper.getMainLooper())
    val handler2 = Handler(Looper.getMainLooper())
    val handler4 = Handler(Looper.getMainLooper())

    var isActiveFrag = false

    private var arrayList: ArrayList<Favourite>? = null

//    private var REQUESTED_PERMISSIONS = arrayOf(
//        Manifest.permission.RECORD_AUDIO,
//        Manifest.permission.CAMERA
//    )

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
        binding.container.setBackgroundColor(Color.WHITE);

        activityInstance = activity

        try {
            engine = P2pRtc().initEngine(requireActivity())
            P2pRtc().addListener(this)
        } catch (ex: Exception) {
            Timber.d(ex)
        }

        try {
            //This is use for get favourite practice partner list
            getFavouritePracticePartner()
            //It's is use for get current user channel data further use for join in the agora call
            getFromAgoraToken()
            getFriendRequest()
            //This is use for change user status
            // changeStatus()
        } catch (ex: Exception) {
            Timber.d(ex)
        }

        searchFavouritePartner()
        // addToTeam()

        //This is use for when another use want to connect with current user so they will create entry for
        //own channel id and we will subscribe this id if any changes then create notification
        try {
            firebaseDatabase.getUserDataFromFirestore(
                mentorId,
                this
            )//yaha par mentor id pass karna hai
        } catch (ex: Exception) {
            Timber.d(ex)
        }

        //it's use for delete notification data when user decline call
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
        binding.progress.animateProgress()
        onBackPress()

    }

    companion object {
        @JvmStatic
        fun newInstance() =
            FavouritePartnerFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }

    private fun setupViewModel() {
        try {
            favouriteViewModel.fetchFav(mentorId ?: "")
        } catch (ex: Exception) {
            showToast(ex.message ?: "")
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
        favouriteAdapter =
            activity?.let { FavouriteAdapter(it, list, this, firebaseDatabase) }
        recycle_view.adapter = favouriteAdapter
        for (v in 0 until arrayList?.size!!) {
            firebaseDatabase.statusLive(arrayList?.get(v)?.uuid ?: "", this)
        }
    }

    private fun fromAgoraToken(channelData: ChannelData?) {
        channelName = channelData?.channelName
        fromTokenId = channelData?.token
        fromUserId = channelData?.userUid
    }

    fun onBack() {
        //showDialog()
        positiveBtnAction()
        // CustomDialogQuiz(requireActivity()).showDialog(::positiveBtnAction)
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
        // Check permission
        // if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID)) {
        CoroutineScope(Dispatchers.IO).launch {
            joinChannel(channelName)
            //  }
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

//    override fun onNotificationForInvitePartner(
//        channelName: String,
//        fromUserId: String,
//        fromUserName: String,
//        fromUserImageUrl: String
//    ) {
//        var i = 0
//        try {
//            visibleView(binding.notificationCard)
//
//            binding.progress.animateProgress()
//            binding.userName.text = fromUserName
//            val imageUrl = fromUserImageUrl.replace("\n", "")
//
//            binding.userImage.setUserImageOrInitials(imageUrl, fromUserName, 30, isRound = true)
//        } catch (ex: Exception) {
//            Timber.d(ex)
//        }
//
//        binding.buttonAccept.setOnClickListener {
//            i = 1
//            invisibleView(binding.notificationCard)
//            favouriteViewModel?.getChannelData(mentorId, channelName)
//            activity?.let {
//                favouriteViewModel?.agoraToToken?.observe(it, {
//                    when {
//                        it?.message.equals(TEAM_CREATED) -> {
//                            firebaseDatabase.deleteRequested(mentorId ?: "")
//                            firebaseDatabase.acceptRequest(
//                                fromUserId,
//                                TRUE,
//                                fromUserName,
//                                channelName,
//                                mentorId
//                            )
//                            initializeAgoraCall(channelName)
//                            moveFragment(fromUserId, channelName)
//                        }
//                        it?.message.equals(USER_ALREADY_JOIN) -> {
//                            visibleView(binding.notificationCardAlready)
//                        }
//                        it?.message.equals(USER_LEFT_THE_GAME) -> {
//                            showToast(PARTNER_LEFT_THE_GAME)
//                        }
//                    }
//                })
//            }
//        }
//
//        binding.alreadyNotification.setOnClickListener {
//            invisibleView(binding.notificationCardAlready)
//
//        }
//        binding.butonDecline.setOnClickListener {
//            invisibleView(binding.notificationCard)
//            mentorId.let { it1 -> firebaseDatabase.deleteUserData(it1, fromUserId) }
//        }
//
//        binding.eee.setOnClickListener {
//            invisibleView(binding.notificationCard)
//            mentorId.let { it1 -> firebaseDatabase.deleteUserData(it1, fromUserId) }
//        }
//
//        lifecycleScope.launch {
//            delay(10000)
//            invisibleView(binding.notificationCard)
//            firebaseDatabase.deleteUserData(mentorId,fromUserId)
//        }
//    }

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

//    override fun onNotificationForPartnerNotAccept(
//        userName: String?,
//        userImageUrl: String,
//        fromUserId: String,
//        declinedUserId: String
//    ) {
//        val pos = favouriteAdapter?.getPositionById(declinedUserId)
//        val holder: FavouriteAdapter.FavViewHolder =
//            binding.recycleView.findViewHolderForAdapterPosition(
//                pos ?: 0
//            ) as FavouriteAdapter.FavViewHolder
//        if (fromUserId == mentorId) {
//            try {
//                holder.binding.clickToken.setImageDrawable(context?.resources?.getDrawable(R.drawable.ic_plus1))
//                holder.binding.clickToken.isEnabled = true
//
//                val image = userImageUrl.replace("\n", "")
//                visibleView(binding.notificationCardNotPlay)
//                binding.userNameForNotPlay.text = userName
//                binding.userImageForNotPaly.setUserImageOrInitials(
//                    image,
//                    userName ?: "",
//                    30,
//                    isRound = true
//                )
//
//            } catch (ex: Exception) {
//                Timber.d(ex)
//
//            }
//        }
//
//        binding.cancelNotification.setOnClickListener {
//            firebaseDatabase.deleteDeclineData(mentorId)
//            invisibleView(binding.notificationCardNotPlay)
//        }
//
//        lifecycleScope.launch {
//            delay(10000)
//            firebaseDatabase.deleteDeclineData(mentorId)
//            invisibleView(binding.notificationCardNotPlay)
//        }
//    }

    private fun getFavouritePracticePartner() {
        activity?.let {
            try {
                favouriteViewModel.favData.observe(it, {
                    initRV(it.data)
                })
            } catch (ex: Exception) {
                showToast(ex.message ?: "")
            }
        }
    }

    private fun getFromAgoraToken() {
        activity?.let {
            try {
                favouriteViewModel.fromTokenData.observe(it, {
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
                TeamMateFoundFragnment.newInstance(userId ?: "", channelName ?: ""),
                "TeamMateFoundFragnment"
            )
            ?.remove(this)
            ?.commit()
    }

    private fun onBackPress() {
        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    //showDialog()
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

            //binding.notificationCard.visibility = View.VISIBLE
            visibleView(binding.notificationCard)
            binding.progress.animateProgress()
            binding.userName.text = fromUserName
            val imageUrl = fromImageUrl.replace("\n", "")
            binding.userImage.setUserImageOrInitials(imageUrl, fromUserName, 30, isRound = true)

        } catch (ex: Exception) {
            Timber.d(ex)
        }

        binding.buttonAccept.setOnClickListener {
            //binding.notificationCard.visibility = View.INVISIBLE
            invisibleView(binding.notificationCard)
            favouriteViewModel.addFavouritePracticePartner(
                AddFavouritePartner(
                    fromMentorId,
                    mentorId
                )
            )
            activity?.let {
                favouriteViewModel.fppData.observe(it, {
                    firebaseDatabase.deleteRequest(mentorId)
                })
            }
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

    }

    override fun onPartnerPlayAgainNotification(
        userName: String,
        userImage: String,
        mentorId: String
    ) {

    }

    fun searchFavouritePartner() {
        binding.inputSearch.setRawInputType(InputType.TYPE_CLASS_TEXT)
        binding.inputSearch.setTextIsSelectable(true);
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
        val prefixString = text.toLowerCase()
        val temp: ArrayList<Favourite> = ArrayList()

        if (prefixString.isEmpty()) {
            arrayList?.let { temp.addAll(it) }
        } else {
            if (arrayList != null) {
                for (wp in arrayList!!) {
                    if (wp.name?.toLowerCase()?.contains(prefixString) == true) {
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
        favouriteAdapter?.updateList(temp, text)
    }
    override fun onGetLiveStatus(status: String, mentorId: String) {
        try {
            val pos: Int = favouriteAdapter?.getPositionById(mentorId) ?: 0
            val holder: FavouriteAdapter.FavViewHolder =
                binding.recycleView.findViewHolderForAdapterPosition(
                    pos
                ) as FavouriteAdapter.FavViewHolder
            holder.binding.status.text = status
            when (status) {
                IN_ACTIVE -> {
                    holder.binding.clickToken.visibility = View.INVISIBLE
                }
                ACTIVE -> {
                    holder.binding.clickToken.visibility = View.VISIBLE
                    holder.binding.clickToken.setImageResource(R.drawable.ic_plus1)
                    holder.binding.clickToken.setOnClickListener {
                        context?.let { it1 ->
                            AudioManagerQuiz.audioRecording.startPlaying(
                                it1,
                                R.raw.tick_animation,
                                false
                            )
                        }
                        holder.binding.clickToken.speed = 1.5F // How fast does the animation play
                        holder.binding.clickToken.repeatCount = LottieDrawable.INFINITE
                        holder.binding.clickToken.setAnimation("lottie/hourglass_anim.json")
                        holder.binding.clickToken.playAnimation()
                        holder.binding.clickToken.isEnabled = false
                        onClickForGetToken(arrayList?.get(pos), pos.toString())
                    }
                    holder.binding.clickToken.isEnabled = true
                }
                IN_GAME -> {
                    holder.binding.clickToken.visibility = View.INVISIBLE
                }
                SEARCHING -> {
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
        fromUserImage: String
    ) {
        var i = 0
        handler.removeCallbacksAndMessages(null)
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
            handler.removeCallbacksAndMessages(null)
            i = 1
            invisibleView(binding.notificationCard)
            favouriteViewModel.getChannelData(mentorId, channelName)
            activity?.let {
                favouriteViewModel.agoraToToken.observe(it, {
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
            handler.removeCallbacksAndMessages(null)
            mentorId.let { it1 -> firebaseDatabase.deleteUserData(it1, fromUserId) }
            firebaseDatabase.createRequestDecline(fromUserId, userName, imageUrl, mentorId)
        }

        binding.eee.setOnClickListener {
            invisibleView(binding.notificationCard)
            handler.removeCallbacksAndMessages(null)
            mentorId.let { it1 -> firebaseDatabase.deleteUserData(it1, fromUserId) }
            firebaseDatabase.createRequestDecline(fromUserId, userName, imageUrl, mentorId)
        }
        try {
            handler4.postDelayed({
                invisibleView(binding.notificationCardAlready)
            }, 10000)
        } catch (ex: Exception) {
        }

        try {
            if (isActiveFrag) {
                handler.postDelayed({
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
        handler1.removeCallbacksAndMessages(null)
        val pos = favouriteAdapter?.getPositionById(declinedUserId)
        val holder: FavouriteAdapter.FavViewHolder =
            binding.recycleView.findViewHolderForAdapterPosition(
                pos ?: 0
            ) as FavouriteAdapter.FavViewHolder
        if (fromUserId == mentorId) {
            try {
                holder.binding.clickToken.setImageDrawable(context?.resources?.getDrawable(R.drawable.ic_plus1))
                holder.binding.clickToken.isEnabled = true

                val image = userImageUrl.replace("\n", "")
                visibleView(binding.notificationCardNotPlay)
                binding.userNameForNotPlay.text = userName
                binding.userImageForNotPaly.setUserImageOrInitials(
                    image,
                    userName ?: "",
                    30,
                    isRound = true
                )

            } catch (ex: Exception) {
                Timber.d(ex)

            }
        }

        binding.cancelNotification.setOnClickListener {
            handler1.removeCallbacksAndMessages(null)
            firebaseDatabase.deleteDeclineData(mentorId)
            invisibleView(binding.notificationCardNotPlay)
        }

        try {
            if (isActiveFrag) {
                handler1.postDelayed({
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
}