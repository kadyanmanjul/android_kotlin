package com.joshtalks.joshskills.quizgame.ui.main.view.fragment

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.lottie.LottieDrawable
import com.google.android.material.card.MaterialCardView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.setUserImageOrInitials
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.FragmentFavouritePracticeBinding
import com.joshtalks.joshskills.quizgame.ui.data.model.AddFavouritePartner
import com.joshtalks.joshskills.quizgame.ui.data.model.ChannelData
import com.joshtalks.joshskills.quizgame.ui.data.model.Favourite
import com.joshtalks.joshskills.quizgame.ui.data.network.FirebaseDatabase
import com.joshtalks.joshskills.quizgame.ui.data.repository.FavouriteRepo
import com.joshtalks.joshskills.quizgame.ui.main.adapter.FavouriteAdapter
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.FavouriteViewModel
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.ViewModelProviderFactory
import com.joshtalks.joshskills.quizgame.util.AudioManagerQuiz
import com.joshtalks.joshskills.quizgame.util.P2pRtc
import com.joshtalks.joshskills.quizgame.util.UpdateReceiver
import com.joshtalks.joshskills.repository.local.model.Mentor
import io.agora.rtc.Constants
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.models.ChannelMediaOptions
import kotlinx.android.synthetic.main.fragment_favourite_practice.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

const val IN_ACTIVE :String = "Inactive"
const val ACTIVE :String = "active"
const val IN_GAME :String = "In game"
const val USER_ALREADY_JOIN:String = "User has already joined the game"
const val USER_LEFT_THE_GAME :String ="User has left the game"
const val PARTNER_LEFT_THE_GAME:String = "Partner has left the game please try again"
const val TEAM_CREATED :String = "Team created successfully"
const val TRUE:String = "true"
const val FALSE :String = "false"
const val STATUS_CHANGE :String = "Status changed successfully"


class FavouritePartnerFragment : Fragment(), FavouriteAdapter.QuizBaseInterface,
    FirebaseDatabase.OnNotificationTrigger,
    P2pRtc.WebRtcEngineCallback, FirebaseDatabase.OnMakeFriendTrigger,
    FirebaseDatabase.OnLiveStatus {

    private lateinit var binding: FragmentFavouritePracticeBinding
    private var favouriteAdapter: FavouriteAdapter? = null
    private var favouriteViewModel: FavouriteViewModel? = null
    private var firebaseDatabase: FirebaseDatabase = FirebaseDatabase()
    private var channelName: String? = null
    private var fromTokenId: String? = null
    private var fromUserId: String? = null
    private var favouriteUserId: String? = null
    private val PERMISSION_REQ_ID = 22
    private var engine: RtcEngine? = null
    private var activityInstance: FragmentActivity? = null
    private var repository: FavouriteRepo? = null
    private var factory: ViewModelProviderFactory? = null
    private var mentorId: String? = null

    private var arrayList: ArrayList<Favourite>? = null
    private var REQUESTED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mentorId = Mentor.getInstance().getUserId()
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
                mentorId ?: "",
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
            repository = FavouriteRepo()
            factory = activity?.application?.let { ViewModelProviderFactory(it, repository!!) }
            favouriteViewModel = factory?.let {
                ViewModelProvider(this, it).get(FavouriteViewModel::class.java)
            }
        } catch (ex: Exception) {

        }
        try {
            favouriteViewModel?.fetchFav(mentorId ?: "")
           // favouriteViewModel?.statusChange(mentorId, ACTIVE)
        } catch (ex: Exception) {
            showToast(ex.message?:"")
        }
    }

    private fun initRV(favouriteList: ArrayList<Favourite>?) {
        arrayList = favouriteList
        favouriteAdapter?.addItems(favouriteList)
        binding.recycleView.setHasFixedSize(true)
        binding.recycleView.layoutManager = LinearLayoutManager(activity)
        favouriteAdapter =
            activity?.let { FavouriteAdapter(it, favouriteList, this, firebaseDatabase) }
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
        showDialog()
    }

    override fun onClickForGetToken(favourite: Favourite?, position: String) {
        favouriteUserId = favourite?.uuid
        favouriteUserId.let {
            firebaseDatabase.createRequest(
                favouriteUserId,
                channelName,
                mentorId!!
            )
        }
    }

    fun initializeAgoraCall(channelName: String) {
        // Check permission
        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID)) {
            CoroutineScope(Dispatchers.IO).launch {
                joinChannel(channelName)
            }
            //WebRtcEngine.initLibrary()
        }
    }

    private fun checkSelfPermission(permission: String, requestCode: Int): Boolean {
        if (activity?.let { ContextCompat.checkSelfPermission(it, permission) } !=
            PackageManager.PERMISSION_GRANTED
        ) {
            activity?.let {
                ActivityCompat.requestPermissions(
                    it,
                    REQUESTED_PERMISSIONS,
                    requestCode
                )
            }
            return false
        }
        return true
    }

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
    override fun onNotificationForInvitePartner(
        channelName: String,
        fromUserId: String,
        fromUserName: String,
        fromUserImageUrl: String
    ) {
        var i = 0
        try {
            visibleView(binding.notificationCard)

            binding.progress.animateProgress()
            binding.userName.text = fromUserName
            val imageUrl = fromUserImageUrl.replace("\n", "")

            binding.userImage.setUserImageOrInitials(imageUrl,fromUserName,30,isRound = true)
        } catch (ex: Exception) {
            Timber.d(ex)
        }

        binding.buttonAccept.setOnClickListener {
            i = 1
            invisibleView(binding.notificationCard)
            favouriteViewModel?.getChannelData(mentorId, channelName)
            activity?.let {
                favouriteViewModel?.agoraToToken?.observe(it, {
                    when {
                        it?.message.equals(TEAM_CREATED) -> {
                            firebaseDatabase.deleteRequested(mentorId ?: "")
                            firebaseDatabase.acceptRequest(
                                fromUserId,
                                TRUE,
                                fromUserName,
                                channelName,
                                mentorId!!
                            )
                            initializeAgoraCall(channelName)
                            moveFragment(fromUserId, channelName)
                        }
                        it?.message.equals(USER_ALREADY_JOIN) -> {
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
            invisibleView(binding.notificationCardAlready)

        }
        binding.butonDecline.setOnClickListener {
            invisibleView(binding.notificationCard)
            mentorId?.let { it1 -> firebaseDatabase.deleteUserData(it1, fromUserId) }
        }

        binding.eee.setOnClickListener {
            invisibleView(binding.notificationCard)
            mentorId?.let { it1 -> firebaseDatabase.deleteUserData(it1, fromUserId) }
        }

        //we have to do 10 second decline request done from firestore side beacuse is user not in fav screen so
        // request will no delete
        lifecycleScope.launch {
                delay(10000)
                invisibleView(binding.notificationCard)
               // mentorId?.let { it1 -> firebaseDatabase.deleteUserData(it1,fromUserId) }
            }
//        if (i!=1){
//            lifecycleScope.launch {
//                delay(10000)
//                invisibleView(binding.notificationCard)
//                mentorId?.let { it1 -> firebaseDatabase.deleteUserData(it1,fromUserId) }
//            }
//        }
//        if (i!=1){
//            val handler = Handler(Looper.getMainLooper())
//            try {
//                handler.postDelayed({
//                    invisibleView(binding.notificationCard)
//                    mentorId?.let { it1 -> firebaseDatabase.deleteUserData(it1,fromUserId) }
//                }, 10000)
//            }catch (ex:Exception){}
//        }
    }

    fun deleteData() {
        if (context?.let { UpdateReceiver.isNetworkAvailable(it) } == true)
            firebaseDatabase.getDeclineCall(mentorId ?: "", this)
    }

    fun visibleView(viewVisible : View){
        binding.img.visibility = View.INVISIBLE
        viewVisible.visibility = View.VISIBLE
    }
    fun invisibleView(viewInvisible:View){
        binding.img.visibility = View.VISIBLE
        viewInvisible.visibility = View.INVISIBLE
    }
    fun getAcceptCall() {
        if (context?.let { UpdateReceiver.isNetworkAvailable(it) } == true)
            firebaseDatabase.getAcceptCall(mentorId ?: "", this)
    }

    override fun onNotificationForPartnerNotAccept(
        userName: String?,
        userImageUrl: String,
        fromUserId: String,
        declinedUserId: String
    ) {
        if (fromUserId == mentorId) {
            try {
                val pos = favouriteAdapter?.getPositionById(declinedUserId)
                val holder: FavouriteAdapter.FavViewHolder =
                    binding.recycleView.findViewHolderForAdapterPosition(
                        pos ?: 0
                    ) as FavouriteAdapter.FavViewHolder
                holder.binding.clickToken.setImageDrawable(context?.resources?.getDrawable(R.drawable.ic_plus1))
                holder.binding.clickToken.isEnabled = true

                val image = userImageUrl.replace("\n", "")
                //binding.notificationCardNotPlay.visibility = View.VISIBLE
                visibleView(binding.notificationCardNotPlay)
                binding.userNameForNotPlay.text = userName
//
                binding.userImageForNotPaly.setUserImageOrInitials(image,userName?:"",30,isRound = true)

            } catch (ex: Exception) {
                Timber.d(ex)

            }
        }

        binding.cancelNotification.setOnClickListener {
            firebaseDatabase.deleteDeclineData(mentorId ?: "")
            //  binding.notificationCardNotPlay.visibility = View.INVISIBLE
            invisibleView(binding.notificationCardNotPlay)
        }

        val handler = Handler(Looper.getMainLooper())
        try {
            handler.postDelayed({
                firebaseDatabase.deleteDeclineData(mentorId ?: "")
               // binding.notificationCardNotPlay.visibility = View.INVISIBLE
                invisibleView(binding.notificationCardNotPlay)
            }, 10000)
        } catch (ex: Exception) {

        }

//        lifecycleScope.launch {
//            delay(10000)
//            firebaseDatabase.deleteDeclineData(mentorId?:"")
//            binding.notificationCardNotPlay.visibility = View.INVISIBLE
//        }
    }

    override fun onNotificationForPartnerAccept(
        channelName: String?,
        timeStamp: String,
        isAccept: String,
        opponentMemberId: String,
        mentorIdIdAcceptedUser: String
    ) {
        if (isAccept == TRUE) {
            firebaseDatabase.deleteDataAcceptRequest(opponentMemberId)
            firebaseDatabase.deleteRequested(mentorIdIdAcceptedUser)
            channelName?.let { initializeAgoraCall(it) }
            moveFragment(mentorIdIdAcceptedUser, channelName)
        }
    }

    override fun onGetRoomId(currentUserRoomID: String?, mentorId: String) {

    }

    override fun onShowAnim(mentorId: String, isCorrect: String, choiceAnswer: String, marks: String) {

    }

    private fun getFavouritePracticePartner() {
        activity?.let {
            try {
                favouriteViewModel?.favData?.observe(it, {
                    initRV(it.data)
                })
            } catch (ex: Exception) {
                showToast(ex.message?:"")
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

//    private fun changeStatus() {
//        activity?.let {
//            try {
//                favouriteViewModel?.statusResponse?.observe(it, {
//                })
//            } catch (ex: Exception) {
//                Timber.d(ex)
//            }
//        }
//    }

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
                    showDialog()
                }
            })
    }

    private fun showDialog() {
        val dialog = Dialog(requireActivity())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        dialog.setCancelable(false)
        dialog.setContentView(R.layout.custom_dialog)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val yesBtn = dialog.findViewById<MaterialCardView>(R.id.btn_yes)
        val noBtn = dialog.findViewById<MaterialCardView>(R.id.btn_no)
        val btnCancel = dialog.findViewById<ImageView>(R.id.btn_cancel)

        yesBtn.setOnClickListener {
            try {
                dialog.dismiss()
                AudioManagerQuiz.audioRecording.stopPlaying()
                openChoiceScreen()
            }catch (ex:Exception){
                showToast(ex.message?:"")
            }
        }
        noBtn.setOnClickListener {
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun openChoiceScreen() {
        val fm = activity?.supportFragmentManager
        fm?.popBackStackImmediate()
        fm?.beginTransaction()
            ?.replace(
                R.id.container,
                ChoiceFragnment.newInstance(), "Favourite"
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
            //binding.notificationCard.visibility = View.VISIBLE
            visibleView(binding.notificationCard)
            binding.progress.animateProgress()
            binding.userName.text = fromUserName
            val imageUrl = fromImageUrl.replace("\n", "")
            binding.userImage.setUserImageOrInitials(imageUrl,fromUserName,30,isRound = true)

        } catch (ex: Exception) {
            Timber.d(ex)
        }

        binding.buttonAccept.setOnClickListener {
            //binding.notificationCard.visibility = View.INVISIBLE
            invisibleView(binding.notificationCard)
            favouriteViewModel?.addFavouritePracticePartner(
                AddFavouritePartner(
                    fromMentorId,
                    mentorId
                )
            )
            activity?.let {
                favouriteViewModel?.fppData?.observe(it, {
                })
            }
        }

        binding.butonDecline.setOnClickListener {
            //binding.notificationCard.visibility = View.INVISIBLE
            invisibleView(binding.notificationCard)
            firebaseDatabase.deleteRequest(mentorId ?: "")
        }


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
                if (s!=null)
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
            if (arrayList!=null){
                for (wp in arrayList!!) {
                    if (wp.name?.toLowerCase()?.contains(prefixString) == true) {
                        temp.add(wp)
                    }
                }
            }
        }
        favouriteAdapter?.updateList(temp, text)
    }

    override fun onGetLiveStatus(status: String, mentorId: String) {
        try {
            val pos:Int = favouriteAdapter?.getPositionById(mentorId)?:0
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
            }
        }catch (ex:Exception){

        }
    }
}