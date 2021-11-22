/*
package com.joshtalks.joshskills.ui.lesson.room

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.conversationRoom.liveRooms.ConversationLiveRoomActivity
import com.joshtalks.joshskills.conversationRoom.roomsListing.*
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.custom_ui.FullScreenProgressDialog
import com.joshtalks.joshskills.core.interfaces.ConversationRoomListAction
import com.joshtalks.joshskills.databinding.ActivityConversationsRoomsListingBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.CHAT_TYPE
import com.joshtalks.joshskills.repository.local.entity.QUESTION_STATUS
import com.joshtalks.joshskills.repository.local.eventbus.ConvoRoomPointsEventBus
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.extra.setOnSingleClickListener
import com.joshtalks.joshskills.ui.lesson.LessonActivityListener
import com.joshtalks.joshskills.ui.lesson.LessonViewModel
import com.joshtalks.joshskills.ui.lesson.ROOM_POSITION
import com.joshtalks.joshskills.ui.voip.WebRtcService
import com.joshtalks.joshskills.ui.voip.WebRtcService.Companion.isConversionRoomActive
import com.joshtalks.joshskills.ui.voip.WebRtcService.Companion.isRoomCreatedByUser
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.callbacks.PNCallback
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.objects_api.channel.PNChannelMetadataResult
import com.pubnub.api.models.consumer.objects_api.channel.PNGetAllChannelsMetadataResult
import com.pubnub.api.models.consumer.objects_api.channel.PNGetChannelMetadataResult
import com.pubnub.api.models.consumer.objects_api.channel.PNSetChannelMetadataResult
import com.pubnub.api.models.consumer.objects_api.membership.PNMembershipResult
import com.pubnub.api.models.consumer.objects_api.uuid.PNUUIDMetadataResult
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult
import com.pubnub.api.models.consumer.pubsub.PNSignalResult
import com.pubnub.api.models.consumer.pubsub.files.PNFileEventResult
import com.pubnub.api.models.consumer.pubsub.message_actions.PNMessageActionResult
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.*


class ConversationRoomListingFragment : CoreJoshFragment(),
    ConversationRoomListAction {
    private var pubnub: PubNub? = null
    private var conversationRoomsListAdapter: ConversationRoomsListAdapter? = null
    lateinit var viewModel: ConversationRoomListingViewModel
    var lessonActivityListener: LessonActivityListener? = null

    private val lessonViewModel: LessonViewModel by lazy {
        ViewModelProvider(requireActivity()).get(LessonViewModel::class.java)
    }
    private var questionId: String? = null
    private var conversationRoomQuestionId: Int? = null

    lateinit var binding: ActivityConversationsRoomsListingBinding
    private val compositeDisposable = CompositeDisposable()
    private var internetAvailableFlag: Boolean = true
    private var isBackPressed: Boolean = false
    private var hasSeenpoints: Boolean = false
    var isActivityOpenFromNotification: Boolean = false
    var roomId: String = ""
    var lastRoomId: String? = null
    var handler: Handler? = null
    var runnable: Runnable? = null

    companion object {
        @JvmStatic
        fun getInstance() = ConversationRoomListingFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //PrefManager.put(IS_CONVERSATION_ROOM_ACTIVE, true)
        PrefManager.put(HAS_SEEN_CONVO_ROOM_POINTS, true)
        hasSeenpoints = false
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is LessonActivityListener) {
            lessonActivityListener = context
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.activity_conversations_rooms_listing,
                container,
                false
            )
        binding.lifecycleOwner = this
        binding = ActivityConversationsRoomsListingBinding.inflate(layoutInflater)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requireActivity().window.statusBarColor =
                this.resources.getColor(R.color.conversation_room_color, requireActivity().theme)
        }
        handler = Handler(Looper.getMainLooper())
        initPubNub()
        initViews()
        addObservers()
        openConversationRoomByNotificationIntent()
        return binding.root
    }


    private fun initPubNub() {
        val pnConf = PNConfiguration()
        pnConf.subscribeKey = BuildConfig.PUBNUB_SUB_API_KEY
        pnConf.publishKey = BuildConfig.PUBNUB_PUB_API_KEY
        //pnConf.origin = "com.joshtalks.joshskills"
        pnConf.isSecure = false
        pnConf.uuid = Mentor.getInstance().getId()
        pubnub = PubNub(pnConf)
        pubnub?.hereNow()
            ?.channels(Arrays.asList("channel_id1"))
            ?.includeUUIDs(true)
            ?.async { result, status ->
                Log.d("ABC", "pubnub hereNow() called with: result = $result, status = $status")
                if (status.isError) {
                    //handle error
                } else {
                    //handle result
                }
            }
        pubnub?.addListener(object : SubscribeCallback() {
            override fun status(pubnub: PubNub, pnStatus: PNStatus) {
                Log.d("ABC", "status() called with: pubnub = $pubnub, pnStatus = $pnStatus")
            }

            override fun message(pubnub: PubNub, pnMessageResult: PNMessageResult) {
                Log.d(
                    "ABC",
                    "message() called with: pubnub = $pubnub, pnMessageResult = $pnMessageResult"
                )
            }

            override fun presence(pubnub: PubNub, pnPresenceEventResult: PNPresenceEventResult) {
                Log.d(
                    "ABC",
                    "presence() called with: pubnub = $pubnub, pnPresenceEventResult = $pnPresenceEventResult"
                )
            }

            override fun signal(pubnub: PubNub, pnSignalResult: PNSignalResult) {
                Log.d(
                    "ABC",
                    "signal() called with: pubnub = $pubnub, pnSignalResult = $pnSignalResult"
                )
            }

            override fun uuid(pubnub: PubNub, pnUUIDMetadataResult: PNUUIDMetadataResult) {
                Log.d(
                    "ABC",
                    "uuid() called with: pubnub = $pubnub, pnUUIDMetadataResult = $pnUUIDMetadataResult"
                )
            }

            override fun channel(pubnub: PubNub, pnChannelMetadataResult: PNChannelMetadataResult) {
                Log.d(
                    "ABC",
                    "channel() called with: pubnub = $pubnub, pnChannelMetadataResult = $pnChannelMetadataResult"
                )
            }

            override fun membership(pubnub: PubNub, pnMembershipResult: PNMembershipResult) {
                Log.d(
                    "ABC",
                    "membership() called with: pubnub = $pubnub, pnMembershipResult = $pnMembershipResult"
                )
            }

            override fun messageAction(
                pubnub: PubNub,
                pnMessageActionResult: PNMessageActionResult
            ) {
                Log.d(
                    "ABC",
                    "messageAction() called with: pubnub = $pubnub, pnMessageActionResult = $pnMessageActionResult"
                )
            }

            override fun file(pubnub: PubNub, pnFileEventResult: PNFileEventResult) {
                Log.d(
                    "ABC",
                    "file() called with: pubnub = $pubnub, pnFileEventResult = $pnFileEventResult"
                )
            }
        })

        pubnub?.setChannelMetadata()
            ?.channel("channel_id1")
            ?.description("Some Description")
            ?.includeCustom(true)
            ?.async(object : PNCallback<PNSetChannelMetadataResult>{
                override fun onResponse(result: PNSetChannelMetadataResult?, status: PNStatus) {
                    Log.d("ABC", "setChannelsMetadata() called with: result = $result, status = $status")
                }

            })

        pubnub?.subscribe()
            ?.channels(Arrays.asList("channel_id1"))
            ?.withPresence()
            ?.execute()

        pubnub?.channelMetadata
            ?.channel("c094fe02-12a4-4e3e-ac75-37406a598849")
            ?.includeCustom(true)
            ?.async(object : PNCallback<PNGetChannelMetadataResult>{
            override fun onResponse(result: PNGetChannelMetadataResult?, status: PNStatus) {
                Log.d("ABC", "channelMetadata() called with: result = $result, status = $status")
            }
        })

        pubnub?.allChannelsMetadata
            ?.filter("custom.is_conversation_room == true")
            ?.includeCustom(true)
            ?.async(object : PNCallback<PNGetAllChannelsMetadataResult>{
                override fun onResponse(result: PNGetAllChannelsMetadataResult?, status: PNStatus) {
                    Log.d("ABC", "allChannelsMetadata list () called with: result = ${result?.data}, status = $status")
                    result?.data?.forEach {
                        Log.d(TAG, "allChannelsMetadata result list entries : ${it}")
                    }
                }
            })
    }

    private fun addObservers() {

        viewModel.navigation.observe(viewLifecycleOwner, {
            FullScreenProgressDialog.hideProgressBar(requireActivity())
            when (it) {
                is ConversationRoomListingNavigation.ApiCallError -> showApiCallErrorToast(it.error)
                is ConversationRoomListingNavigation.OpenConversationLiveRoom -> openConversationLiveRoom(
                    it.channelName,
                    it.uid,
                    it.token,
                    it.isRoomCreatedByUser,
                    it.roomId
                )
                ConversationRoomListingNavigation.AtleastOneRoomAvailable -> showRecyclerView()
                ConversationRoomListingNavigation.NoRoomAvailable -> showNoRoomAvailableText()
            }
        })


        viewModel.roomDetailsLivedata.observe(viewLifecycleOwner, { response ->
            if (response.alreadyConversed != null && response.alreadyConversed >= 1) {
                binding.progressContainer.visibility = View.VISIBLE
                binding.progressBar.max = response.duration!!
                binding.progressBar.progress = response.alreadyConversed
                binding.minutesSpoken.text = getString(
                    R.string.convo_room_minutes_spoken,
                    response.alreadyConversed,
                    response.duration!!
                )
            }
            if (response.alreadyConversed != null && (response.alreadyConversed >= response.duration!!)) {
                binding.continueBtn.visibility = View.VISIBLE
                lessonActivityListener?.onQuestionStatusUpdate(
                    QUESTION_STATUS.AT,
                    questionId
                )
                lessonActivityListener?.onSectionStatusUpdate(ROOM_POSITION, true)
            }
        })

        viewModel.points.observe(viewLifecycleOwner, { pointsString ->
            if (pointsString.isNotBlank()) {
                showSnackBar(binding.rootView, Snackbar.LENGTH_LONG, pointsString)
                PrefManager.put(HAS_SEEN_CONVO_ROOM_POINTS, true)
                hasSeenpoints = true
            } else {
                PrefManager.put(HAS_SEEN_CONVO_ROOM_POINTS, true)
                compositeDisposable.add(getPointsDisposable())
            }
        })

        lessonViewModel.lessonQuestionsLiveData.observe(
            viewLifecycleOwner,
            {
                val crQuestion = it.filter { it.chatType == CHAT_TYPE.CR }.getOrNull(0)
                questionId = crQuestion?.id

                crQuestion?.conversation_question_id?.let {
                    this.conversationRoomQuestionId = it
                }
            }
        )
    }

    private fun initViews() {

        viewModel = ConversationRoomListingViewModel()
        getIntentExtras(requireActivity().intent)
        setUpRecyclerView()
        setFlagInWebRtcServie()
        viewModel.makeEnterExitConversationRoom(true)

        with(binding) {
            createRoom.apply {
                clipToOutline = true
                setOnSingleClickListener {
                    if (PermissionUtils.isCallingPermissionWithoutLocationEnabled(requireActivity())) {
                        showAddTopicPopup()
                        return@setOnSingleClickListener
                    }

                    PermissionUtils.onlyCallingFeaturePermission(
                        requireActivity(),
                        object : MultiplePermissionsListener {
                            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                                report?.areAllPermissionsGranted()?.let { flag ->
                                    if (flag) {
                                        showAddTopicPopup()
                                        return
                                    }
                                    if (report.isAnyPermissionPermanentlyDenied) {
                                        PermissionUtils.callingPermissionPermanentlyDeniedDialog(
                                            requireActivity(),
                                            R.string.convo_room_start_permission_message
                                        )
                                        return
                                    }
                                }
                            }

                            override fun onPermissionRationaleShouldBeShown(
                                permissions: MutableList<PermissionRequest>?,
                                token: PermissionToken?
                            ) {
                                token?.continuePermissionRequest()
                            }
                        }
                    )
                }
            }
            continueBtn.apply {
                clipToOutline = true
                setOnSingleClickListener {
                    lessonActivityListener?.onNextTabCall(ROOM_POSITION)
                }
            }
        }

    }

    private fun getIntentExtras(intent: Intent?) {
        isActivityOpenFromNotification =
            intent?.getBooleanExtra("open_from_notification", false) == true
        roomId = intent?.getStringExtra("room_id") ?: ""
    }

    private fun showNoRoomAvailableText() {
        with(binding) {
            noRoomsText.apply {
                visibility = View.VISIBLE
                text =
                    String.format(getString(R.string.no_room_text))
            }
            recyclerView.apply {
                visibility = View.GONE
            }
        }
    }

    private fun showRecyclerView() {
        with(binding) {
            noRoomsText.apply {
                visibility = View.GONE
            }
            recyclerView.apply {
                visibility = View.VISIBLE
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(requireContext())
                adapter = conversationRoomsListAdapter
            }
        }
    }

    private fun openConversationRoomByNotificationIntent() {
        if (isActivityOpenFromNotification && roomId.isNotEmpty()) {
            lastRoomId = roomId
            */
/*Handler(Looper.getMainLooper()).postDelayed({
                notebookRef.document(roomId).get().addOnSuccessListener {
                    viewModel.joinRoom(
                        ConversationRoomsListingItem(
                            it["channel_name"]?.toString() ?: "",
                            it["topic"]?.toString(),
                            it["started_by"]?.toString()?.toInt(),
                            roomId.toInt(),
                            conversationRoomQuestionId
                        )
                    )
                }
            }, 200)*//*

        }
    }

    private fun setFlagInWebRtcServie() {
        val intent = Intent(requireActivity(), WebRtcService::class.java)
        isConversionRoomActive = true
        requireActivity().startService(intent)
    }

    override fun onResume() {
        super.onResume()
        observeNetwork()
    }

    private fun observeNetwork() {
        PrefManager.put(PREF_IS_CONVERSATION_ROOM_ACTIVE, false)
        hasSeenpoints = false
        compositeDisposable.add(
            ReactiveNetwork.observeNetworkConnectivity(AppObjectController.joshApplication)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { connectivity ->
                    internetAvailableFlag = connectivity.available()
                    if (internetAvailableFlag) {
                        internetAvailable()
                    } else {
                        internetNotAvailable()
                    }
                }
        )
        conversationRoomQuestionId?.let {
            viewModel.getConvoRoomDetails(it)

            if (PrefManager.getBoolValue(HAS_SEEN_CONVO_ROOM_POINTS, defValue = false).not()) {
                compositeDisposable.remove(getPointsDisposable())
                viewModel.getPointsForConversationRoom(lastRoomId, it)
                hasSeenpoints = true
                //PrefManager.put(HAS_SEEN_CONVO_ROOM_POINTS, true)
            } else {
                compositeDisposable.add(getPointsDisposable())
            }
        }
    }

    fun getPointsDisposable(): Disposable {
        return RxBus2.listen(ConvoRoomPointsEventBus::class.java)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (hasSeenpoints.not()) {
                    conversationRoomQuestionId?.let {
                        viewModel.getPointsForConversationRoom(
                            lastRoomId,
                            conversationRoomQuestionId
                        )
                    }
                }
            }
    }

    private fun internetNotAvailable() {
        binding.notificationBar.apply {
            visibility = View.VISIBLE
            hideActionLayout()
            loadAnimationSlideDown()
            setHeading("The Internet connection appears to be offline")
            setBackgroundColor(false)
            startSound()
        }
    }

    private fun internetAvailable() {
        binding.notificationBar.apply {
            visibility = View.GONE
            endSound()
        }
    }

    private fun takePermissions() {
        if (PermissionUtils.isCallingPermissionWithoutLocationEnabled(requireActivity())) {
            return
        }

        PermissionUtils.onlyCallingFeaturePermission(
            requireActivity(),
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            return
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            PermissionUtils.callingPermissionPermanentlyDeniedDialog(
                                requireActivity(),
                                R.string.convo_room_start_permission_message
                            )
                            return
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            }
        )
    }

    private fun openConversationLiveRoom(
        channelName: String?,
        uid: Int?,
        token: String?,
        isRoomCreatedByUser: Boolean,
        roomId: Int?
    ) {
        WebRtcService.isRoomCreatedByUser = true
        isConversionRoomActive = true
        lastRoomId = roomId.toString()
        if (isRoomCreatedByUser) {
            SearchingRoomPartnerActivity.startUserForPractiseOnPhoneActivity(
                requireActivity(),
                channelName,
                uid,
                token,
                isRoomCreatedByUser,
                roomId,
                conversationRoomQuestionId
            )
        } else {
            ConversationLiveRoomActivity.startConversationLiveRoomActivity(
                requireActivity(),
                channelName,
                uid,
                token,
                isRoomCreatedByUser,
                roomId,
                conversationRoomQuestionId
            )

        }
    }

    private fun showApiCallErrorToast(error: String) {
        if (error.isNotEmpty()) {
            binding.notificationBar.apply {
                visibility = View.VISIBLE
                hideActionLayout()
                setHeading(error)
                setBackgroundColor(false)
                loadAnimationSlideDown()
                startSound()
                hideNotificationAfter4seconds()
            }
        } else {
            Toast.makeText(requireActivity(), "Something Went Wrong !!!", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun hideNotificationAfter4seconds() {
        if (runnable == null) {
            setRunnable()
            handler?.postDelayed(runnable!!, 4000)
        } else {
            handler?.removeCallbacks(runnable!!)
            setRunnable()
            handler?.postDelayed(runnable!!, 4000)
        }
    }

    private fun setRunnable() {
        runnable = Runnable {
            binding.notificationBar.loadAnimationSlideUp()
            binding.notificationBar.endSound()
        }
    }


    private fun showAddTopicPopup() {
        var topic = ""
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        val inflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.alert_label_editor, null)
        dialogBuilder.setView(dialogView)

        val alertDialog: AlertDialog = dialogBuilder.create()
        alertDialog.show()
        alertDialog.window?.let { window ->
            val width = AppObjectController.screenWidth * .91
            val height = ViewGroup.LayoutParams.WRAP_CONTENT
            val wlp: WindowManager.LayoutParams = window.getAttributes()
            wlp.gravity = Gravity.BOTTOM
            //wlp.flags = wlp.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv()
            window.setAttributes(wlp)
            window.setLayout(width.toInt(), height)
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            //window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }

        dialogView.findViewById<EditText>(R.id.label_field).requestFocus()
        dialogView.findViewById<EditText>(R.id.label_field).isFocusable = true

        dialogView.findViewById<MaterialButton>(R.id.create_room).setOnSingleClickListener {
            if (dialogView.findViewById<EditText>(R.id.label_field).text.toString()
                    .isNotBlank()
            ) {
                showPatnerChooserPopup(dialogView.findViewById<EditText>(R.id.label_field).text.toString())
                hideKeyboard(requireActivity())
                alertDialog.dismiss()
            } else {
                showToast("Please enter Topic name")
            }
        }
    }

    private fun showPatnerChooserPopup(topic: String) {
        var topic = topic
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        val inflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.alert_room_picker, null)
        dialogBuilder.setView(dialogView)
        var isP2Pselected = true

        val alertDialog: AlertDialog = dialogBuilder.create()
        alertDialog.show()
        alertDialog.window?.let { window ->
            val width = AppObjectController.screenWidth * .91
            val height = ViewGroup.LayoutParams.WRAP_CONTENT
            val wlp: WindowManager.LayoutParams = window.getAttributes()
            wlp.gravity = Gravity.BOTTOM
            window.setAttributes(wlp)
            window.setLayout(width.toInt(), height)
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        }

        if (viewModel.roomDetailsLivedata.value?.is_favourite_practice_partner_available == true) {
            dialogView.findViewById<MaterialCardView>(R.id.favt_container).visibility =
                View.VISIBLE
        } else {
            dialogView.findViewById<MaterialCardView>(R.id.favt_container).visibility =
                View.GONE
        }

        val temp = getString(R.string.convo_room_dialog_desc)
        val sBuilder = SpannableStringBuilder(temp)
        sBuilder.setSpan(
            StyleSpan(Typeface.BOLD),
            26,
            temp.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        dialogView.findViewById<TextView>(R.id.tip).setText(
            sBuilder,
            TextView.BufferType.SPANNABLE
        )

        dialogView.findViewById<MaterialButton>(R.id.create_room).setOnClickListener {
            FullScreenProgressDialog.showProgressBar(requireActivity())
            viewModel.createRoom(topic, isP2Pselected.not(), conversationRoomQuestionId)
            */
/*val data = JsonObject()
            data.addProperty("text", topic)
            pubnub!!.publish()
                .channel("channel_id1")
                .message(data)
                .async(object : PNCallback<PNPublishResult> {
                    override fun onResponse(result: PNPublishResult?, status: PNStatus) {
                        Log.d("ABC", "onResponse() called with: result = $result, status = $status")
                    }
                })*//*

            alertDialog.dismiss()
        }
        dialogView.findViewById<MaterialCardView>(R.id.p2p_container).setOnClickListener {
            dialogView.findViewById<MaterialCardView>(R.id.p2p_container)
                .setCardBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.artboard_color
                    )
                )
            dialogView.findViewById<MaterialCardView>(R.id.favt_container)
                .setCardBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.white
                    )
                )
            dialogView.findViewById<MaterialCardView>(R.id.p2p_container).setStrokeColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.artboard_stroke_color
                )
            )
            dialogView.findViewById<MaterialCardView>(R.id.favt_container).setStrokeColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                )
            )
            val temp = getString(R.string.convo_room_dialog_desc)
            val sBuilder = SpannableStringBuilder(temp)
            sBuilder.setSpan(
                StyleSpan(Typeface.BOLD),
                26,
                temp.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            dialogView.findViewById<TextView>(R.id.tip).setText(
                sBuilder,
                TextView.BufferType.SPANNABLE
            )
            isP2Pselected = true
        }
        dialogView.findViewById<MaterialCardView>(R.id.favt_container).setOnClickListener {
            dialogView.findViewById<MaterialCardView>(R.id.p2p_container)
                .setCardBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.white
                    )
                )
            dialogView.findViewById<MaterialCardView>(R.id.favt_container)
                .setCardBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.artboard_color
                    )
                )
            dialogView.findViewById<MaterialCardView>(R.id.p2p_container).setStrokeColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                )
            )
            dialogView.findViewById<MaterialCardView>(R.id.favt_container).setStrokeColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.artboard_stroke_color
                )
            )
            val temp = getString(R.string.convo_room_dialog_desc_favt)
            val sBuilder = SpannableStringBuilder(temp)
            sBuilder.setSpan(
                StyleSpan(Typeface.BOLD),
                26,
                temp.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            dialogView.findViewById<TextView>(R.id.tip).setText(
                sBuilder,
                TextView.BufferType.SPANNABLE
            )
            isP2Pselected = false
        }
    }

    override fun onStart() {
        super.onStart()
        pubnub?.subscribe()?.channels(
            Arrays.asList("channel_id1")
        )?.withPresence()
            ?.execute()
    }

    override fun onStop() {
        super.onStop()
        pubnub?.unsubscribeAll()
        compositeDisposable.clear()
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    override fun onDestroy() {
        if (!isBackPressed) {
            viewModel.makeEnterExitConversationRoom(false)
        }
        super.onDestroy()
        pubnub?.destroy()
        isConversionRoomActive = false
        isRoomCreatedByUser = false
    }

    private fun setUpRecyclerView() {
        conversationRoomsListAdapter = ConversationRoomsListAdapter(this)
    }

    private fun addItemsToAdapter(items: List<ConversationRoomsListingItem>) {
        conversationRoomsListAdapter?.addItems(items)
    }

    override fun onRoomClick(item: ConversationRoomsListingItem) {
        if (PermissionUtils.isCallingPermissionWithoutLocationEnabled(requireActivity())) {
            FullScreenProgressDialog.showProgressBar(requireActivity())
            item.conversationRoomQuestionId = conversationRoomQuestionId
            lastRoomId = (item.room_id ?: lastRoomId).toString()
            viewModel.joinRoom(item)
            return
        }

        PermissionUtils.onlyCallingFeaturePermission(
            requireActivity(),
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            FullScreenProgressDialog.showProgressBar(requireActivity())
                            item.conversationRoomQuestionId = conversationRoomQuestionId
                            lastRoomId = (item.room_id ?: lastRoomId).toString()
                            viewModel.joinRoom(item)
                            return
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            PermissionUtils.callingPermissionPermanentlyDeniedDialog(
                                requireActivity(),
                                R.string.convo_room_start_permission_message
                            )
                            return
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            }
        )
    }
}
*/
