package com.joshtalks.joshskills.quizgame.ui.main.view.fragment

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
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
import com.google.android.material.card.MaterialCardView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.USER_ACTIVE_IN_GAME
import com.joshtalks.joshskills.core.setUserImageOrInitials
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.FragmentChoiceFragnmentBinding
import com.joshtalks.joshskills.quizgame.StartActivity
import com.joshtalks.joshskills.quizgame.ui.data.network.FirebaseDatabase
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.ChoiceViewModel
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.ChoiceViewModelProviderFactory
import com.joshtalks.joshskills.quizgame.util.AudioManagerQuiz
import com.joshtalks.joshskills.quizgame.util.P2pRtc
import com.joshtalks.joshskills.quizgame.util.UpdateReceiver
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

class ChoiceFragnment :Fragment(),FirebaseDatabase.OnNotificationTrigger,P2pRtc.WebRtcEngineCallback {
    private lateinit var binding: FragmentChoiceFragnmentBinding
    private var mentorId:String=Mentor.getInstance().getUserId()
    private var activityInstance: FragmentActivity? = null

    val vm by lazy {
        ViewModelProvider(requireActivity())[ChoiceViewModel::class.java]
    }
    private var firebaseDatabase: FirebaseDatabase = FirebaseDatabase()
    private var factory: ChoiceViewModelProviderFactory? = null
    private var REQUESTED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )
    private val PERMISSION_REQ_ID = 22
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
        binding.lifecycleOwner = this
        binding.clickHandler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activityInstance = activity
        PrefManager.put(USER_LEFT_THE_GAME, false)
        playSound(R.raw.compress_background_util_quiz)
        onBackPress()
        try {
            engine = P2pRtc().initEngine(requireActivity())
            P2pRtc().addListener(this)
        } catch (ex: Exception) {
            Timber.d(ex)
        }
        try {
            firebaseDatabase.getUserDataFromFirestore(mentorId, this)
        } catch (ex: Exception) {
            Timber.d(ex)
        }

        if (isAdded && activityInstance != null) {
            getAcceptCall()
        } else {
            showToast("Crash")
        }
    }

    fun getAcceptCall() {
        if (context?.let { UpdateReceiver.isNetworkAvailable(it) } == true)
            firebaseDatabase.getAcceptCall(mentorId ?: "", this)
    }

    fun playSound(sound:Int){
        if (activity?.application?.let { AudioManagerQuiz.audioRecording.isPlaying() } != true){
            activity?.application?.let { AudioManagerQuiz.audioRecording.startPlaying(it,sound,true) }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            ChoiceFragnment().apply {
                arguments = Bundle().apply {

                }
            }
    }

    fun openFavouritePartnerScreen(){
        val fm = activity?.supportFragmentManager
        fm?.beginTransaction()
            ?.replace(R.id.container,
                FavouritePartnerFragment.newInstance(),"Favourite")
            ?.remove(this)
            ?.commit()
    }

    fun openRandomScreen(){
        val fm = activity?.supportFragmentManager
        fm?.beginTransaction()
            ?.replace(R.id.container,
                RandomPartnerFragment.newInstance(),"Random")
            ?.remove(this)
            ?.commit()
    }

    fun onBackPress() {
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
               showDialog()
            }
        })
    }

    fun setUpViewModel(){
        try {
            factory = activity?.application?.let { ChoiceViewModelProviderFactory(it) }
            vm.statusChange(mentorId, ACTIVE)
        }catch (ex:Exception){

        }
    }

    private fun showDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        dialog.setCancelable(false)
        dialog.setContentView(R.layout.custom_dialog)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val yesBtn = dialog.findViewById<MaterialCardView>(R.id.btn_yes)
        val noBtn = dialog.findViewById<MaterialCardView>(R.id.btn_no)
        val btnCancel = dialog.findViewById<ImageView>(R.id.btn_cancel)

        yesBtn.setOnClickListener {
            try {
                if (UpdateReceiver.isNetworkAvailable(requireActivity())){
                    vm.homeInactive(mentorId, IN_ACTIVE)
                    this.let {
                        try {
                            vm.homeInactiveResponse.observe(it, {
                                if (it.message == "User status changed to inactive , data deleted from UserStatusRedis"){
                                    PrefManager.put(USER_ACTIVE_IN_GAME, false)
                                    dialog.dismiss()
                                    AudioManagerQuiz.audioRecording.stopPlaying()
                                    firebaseDatabase.deleteRequested(mentorId?:"")
                                    firebaseDatabase.deleteDeclineData(mentorId?:"")
                                    moveToNewActivity()
                                }
                            })
                        } catch (ex: Exception) {
                            Timber.d(ex)
                        }
                    }
                }
            } catch (e: Exception) {
                showToast(e.message?:"")
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

    private fun moveToNewActivity() {
        val i = Intent(activity, StartActivity::class.java)
        startActivity(i)
        (activity as Activity?)?.overridePendingTransition(0, 0)
        requireActivity().finish()
    }

    override fun onNotificationForInvitePartner(
        channelName: String,
        fromUserId: String,
        fromUserName: String,
        fromUserImage: String
    ) {
        var i = 0
        try {

            visibleView(binding.notificationCard)

            binding.progress.animateProgress()
            binding.userName.text = fromUserName
            val imageUrl = fromUserImage.replace("\n", "")

            binding.userImage.setUserImageOrInitials(imageUrl,fromUserName,30,isRound = true)
        } catch (ex: Exception) {
            Timber.d(ex)
        }

        binding.buttonAccept.setOnClickListener {
            i = 1
            invisibleView(binding.notificationCard)
            vm.getChannelData(mentorId, channelName)
            activity?.let {
                vm.agoraToToken.observe(it, {
                    when {
                        it?.message.equals(TEAM_CREATED) -> {
                            firebaseDatabase.deleteRequested(mentorId ?: "")
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
            mentorId.let { it1 -> firebaseDatabase.deleteUserData(it1, fromUserId) }
        }

        binding.eee.setOnClickListener {
            invisibleView(binding.notificationCard)
            mentorId.let { it1 -> firebaseDatabase.deleteUserData(it1, fromUserId) }
        }


        //we have to do 10 second decline request done from firestore side beacuse is user not in fav screen so
        // request will no delete
        lifecycleScope.launch {
            delay(10000)
            invisibleView(binding.notificationCard)
            mentorId.let { it1 -> firebaseDatabase.deleteUserData(it1,fromUserId) }
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

    override fun onNotificationForPartnerNotAccept(
        userName: String?,
        userImageUrl: String,
        fromUserId: String,
        declinedUserId: String
    ) {
        binding.cancelNotification.setOnClickListener {
            firebaseDatabase.deleteDeclineData(mentorId)
            invisibleView(binding.notificationCardNotPlay)
        }

        val handler = Handler(Looper.getMainLooper())
        try {
            handler.postDelayed({
                firebaseDatabase.deleteDeclineData(mentorId)
                invisibleView(binding.notificationCardNotPlay)
            }, 10000)
        } catch (ex: Exception) {

        }
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

    override fun onShowAnim(
        mentorId: String,
        isCorrect: String,
        choiceAnswer: String,
        marks: String
    ) {

    }

    fun visibleView(viewVisible : View){
        viewVisible.visibility = View.VISIBLE
    }
    fun invisibleView(viewInvisible:View){
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
}