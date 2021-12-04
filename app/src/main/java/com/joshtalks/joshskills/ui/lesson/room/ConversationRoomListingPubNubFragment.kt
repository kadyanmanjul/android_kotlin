package com.joshtalks.joshskills.ui.lesson.room

import android.app.AlertDialog
import android.content.Context
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
import androidx.collection.ArraySet
import androidx.collection.arraySetOf
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.conversationRoom.liveRooms.ConversationLiveRoomActivity
import com.joshtalks.joshskills.conversationRoom.model.RoomListResponseItem
import com.joshtalks.joshskills.conversationRoom.roomsListing.*
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.LogException
import com.joshtalks.joshskills.core.custom_ui.FullScreenProgressDialog
import com.joshtalks.joshskills.core.interfaces.ConversationRoomListAction
import com.joshtalks.joshskills.databinding.ActivityConversationsRoomsListingBinding
import com.joshtalks.joshskills.repository.local.entity.CHAT_TYPE
import com.joshtalks.joshskills.repository.local.entity.QUESTION_STATUS
import com.joshtalks.joshskills.repository.local.eventbus.ConversationRoomListPubNubEventBus
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.extra.setOnSingleClickListener
import com.joshtalks.joshskills.ui.lesson.LessonActivityListener
import com.joshtalks.joshskills.ui.lesson.LessonViewModel
import com.joshtalks.joshskills.ui.lesson.ROOM_POSITION
import com.joshtalks.joshskills.ui.voip.WebRtcService.Companion.isRoomCreatedByUser
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.objects_api.channel.PNChannelMetadataResult
import com.pubnub.api.models.consumer.objects_api.membership.PNMembershipResult
import com.pubnub.api.models.consumer.objects_api.uuid.PNUUIDMetadataResult
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult
import com.pubnub.api.models.consumer.pubsub.PNSignalResult
import com.pubnub.api.models.consumer.pubsub.files.PNFileEventResult
import com.pubnub.api.models.consumer.pubsub.message_actions.PNMessageActionResult
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.ReplaySubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

class ConversationRoomListingPubNubFragment : CoreJoshFragment(),
    ConversationRoomListAction {
    private var pubnub: PubNub? = null
    private var conversationRoomsListAdapter: ConversationRoomsListAdapter? = null
    var lessonActivityListener: LessonActivityListener? = null

    private val lessonViewModel: LessonViewModel by lazy {
        ViewModelProvider(requireActivity()).get(LessonViewModel::class.java)
    }
    private val viewModel by lazy {
        ViewModelProvider(requireActivity()).get(
            ConversationRoomListingViewModel::class.java
        )
    }
    private var questionId: String? = null
    private var conversationRoomQuestionId: Int? = null

    lateinit var binding: ActivityConversationsRoomsListingBinding
    private val compositeDisposable = CompositeDisposable()
    private var internetAvailableFlag: Boolean = true
    private var isBackPressed: Boolean = false
    var isActivityOpenFromNotification: Boolean = false
    var roomId: String = ""
    var lastRoomId: String? = null
    var handler: Handler? = null
    var runnable: Runnable? = null
    private var rooomList: ArraySet<RoomListResponseItem>? = arraySetOf()
    private var replaySubject = ReplaySubject.create<Any>()
    private var isPubNubObserverAdded: Boolean = false

    companion object {
        @JvmStatic
        fun getInstance() = ConversationRoomListingPubNubFragment()
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
        //viewModel.makeEnterExitConversationRoom(true)
        initPubNub()
        initViews()
        addObservers()
        openConversationRoomByNotificationIntent()
        PrefManager.put(HAS_SEEN_CONVO_ROOM_POINTS, true)
        //ConvoWebRtcService.initLibrary()
        return binding.root
    }


    private fun initPubNub() {
        val pnConf = PNConfiguration()
        pnConf.subscribeKey = BuildConfig.PUBNUB_SUB_API_KEY
        pnConf.publishKey = BuildConfig.PUBNUB_PUB_API_KEY
        pnConf.uuid = Mentor.getInstance().getId()
        //pnConf.origin = "com.joshtalks.joshskills"
        pnConf.isSecure = false
        pubnub = PubNub(pnConf)

        pubnub?.addListener(object : SubscribeCallback() {
            override fun status(pubnub: PubNub, pnStatus: PNStatus) {}

            override fun message(pubnub: PubNub, pnMessageResult: PNMessageResult) {
                Log.d(
                    "ABCF",
                    "SubscribeCallback  pubnub = [$pubnub], pnMessageResult = [$pnMessageResult]"
                )

                val msg = pnMessageResult.message.asJsonObject
                val act = msg["action"].asString
                try {
                    if (msg != null) {
                        replaySubject.toSerialized()
                            .onNext(ConversationRoomListPubNubEventBus(act, msg))
                    }
                } catch (ex: Exception) {
                    LogException.catchException(ex)
                }
            }

            override fun presence(pubnub: PubNub, pnPresenceEventResult: PNPresenceEventResult) {}

            override fun signal(pubnub: PubNub, pnSignalResult: PNSignalResult) {}

            override fun uuid(pubnub: PubNub, pnUUIDMetadataResult: PNUUIDMetadataResult) {}

            override fun channel(
                pubnub: PubNub,
                pnChannelMetadataResult: PNChannelMetadataResult
            ) {
            }

            override fun membership(pubnub: PubNub, pnMembershipResult: PNMembershipResult) {}

            override fun messageAction(
                pubnub: PubNub,
                pnMessageActionResult: PNMessageActionResult
            ) {
            }

            override fun file(pubnub: PubNub, pnFileEventResult: PNFileEventResult) {}
        })

        pubnub?.subscribe()
            ?.channels(Arrays.asList("conversation_room_broadcast"))
            ?.withPresence()
            ?.execute()
    }

    private fun updateRoom(msg: JsonObject, isUserLeaving: Boolean) {
        Log.d("ABCF", "updateRoom on join or leave isUserLeaving = $isUserLeaving")
        val data = msg["data"].asJsonObject
        val matType = object : TypeToken<RoomListResponseItem>() {}.type
        if (data == null) {
            return
        }
        val room = AppObjectController.gsonMapper.fromJson<RoomListResponseItem>(data, matType)
        CoroutineScope(Dispatchers.Main).launch {
            Log.d(
                "ABCF",
                "updateRoom on join or leave with data room = $room, isUserLeaving = $isUserLeaving"
            )
            updateItemInAdapter(room, isUserLeaving)
        }
    }

    private fun removeRoomFromList(msg: JsonObject) {
        Log.d("ABCF", "removeRoomFromList called with: msg = $msg")
        val data = msg["data"].asJsonObject
        val matType = object : TypeToken<RoomListResponseItem>() {}.type
        if (data == null) {
            return
        }
        val room = AppObjectController.gsonMapper.fromJson<RoomListResponseItem>(data, matType)
        CoroutineScope(Dispatchers.Main).launch {
            Log.d("ABCF", "removeRoomFromList() called room: ${room}")
            removeItemsFromAdapter(room)
        }
    }

    private fun addNewRoomToList(msg: JsonObject) {
        val data = msg["data"].asJsonObject
        Log.d("ABCF", "addNewRoomToList() called with: msg = $data")
        val matType = object : TypeToken<RoomListResponseItem>() {}.type
        if (data == null) {
            return
        }
        val room = AppObjectController.gsonMapper.fromJson<RoomListResponseItem>(data, matType)
        CoroutineScope(Dispatchers.Main).launch {
            showRecyclerView()
            Log.d("ABCF", "addNewRoomToList() with data called $room")
            addNewItemsToAdapter(room)
        }
    }

    private fun addObservers() {
        viewModel.navigation.observe(viewLifecycleOwner, {
            FullScreenProgressDialog.hideProgressBar(requireActivity())
            try {
                when (it) {
                    is ConversationRoomListingNavigation.ApiCallError -> showApiCallErrorToast(it.error)
                    is ConversationRoomListingNavigation.OpenConversationLiveRoom -> openConversationLiveRoom(
                        it.channelName,
                        it.uid,
                        it.token,
                        it.isRoomCreatedByUser,
                        it.roomId,
                        it.startedBy,
                        it.topic ?: EMPTY
                    )
                    ConversationRoomListingNavigation.AtleastOneRoomAvailable -> showRecyclerView()
                    ConversationRoomListingNavigation.NoRoomAvailable -> showNoRoomAvailableText()
                }
            } catch (ex: Exception) {
                LogException.catchException(ex)
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

        viewModel.roomListLiveData.observe(this.viewLifecycleOwner, { rooms ->
            Log.d("ABCF", "API data : rooms = $rooms")
            if (rooms.isNullOrEmpty().not()) {
                showRecyclerView()
                rooomList?.clear()
                rooomList?.addAll(rooms)
                conversationRoomsListAdapter?.addItems(ArrayList(rooomList))
            } else {
                showNoRoomAvailableText()
            }
            addPubNubEventObserver()
        })
        viewModel.points.observe(viewLifecycleOwner, { pointsString ->
            if (pointsString.isNotBlank()) {
                showSnackBar(binding.rootView, Snackbar.LENGTH_LONG, pointsString)
            }
            PrefManager.put(HAS_SEEN_CONVO_ROOM_POINTS, true)
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

    private fun addPubNubEventObserver() {
        isPubNubObserverAdded = true
        compositeDisposable.add(
            replaySubject.ofType(ConversationRoomListPubNubEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    Log.d(
                        "ABCF",
                        "replaySubject disposable  called with event ${it.action} ${it.data}"
                    )
                    try {
                        if (it.data != null) {
                            when (it.action) {
                                "CREATE_ROOM" -> addNewRoomToList(it.data)
                                "LEAVE_ROOM" -> updateRoom(it.data, true)
                                "JOIN_ROOM" -> updateRoom(it.data, false)
                                "END_ROOM" -> removeRoomFromList(it.data)
                            }
                            CoroutineScope(Dispatchers.Main).launch {
                                if (conversationRoomsListAdapter?.isRoomEmpty() == true) {
                                    showNoRoomAvailableText()
                                } else {
                                    showRecyclerView()
                                }
                            }
                        }
                    } catch (ex: Exception) {
                        LogException.catchException(ex)
                    }
                })
    }

    private fun initViews() {

        setUpRecyclerView()
        //setFlagInWebRtcServie()

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
            showNoRoomAvailableText()
            recyclerView.apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(requireContext())
                adapter = conversationRoomsListAdapter
            }
        }

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
            conversationRoomsListAdapter?.clearAllRooms()
        }
    }

    private fun showRecyclerView() {
        with(binding) {
            noRoomsText.apply {
                visibility = View.GONE
            }
            recyclerView.apply {
                visibility = View.VISIBLE
            }
        }
    }

    private fun openConversationRoomByNotificationIntent() {
        if (isActivityOpenFromNotification && roomId.isNotEmpty()) {
            lastRoomId = roomId
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
            }, 200)*/
        }
    }


    override fun onResume() {
        super.onResume()
        observeNetwork()
    }

    private fun observeNetwork() {
        PrefManager.put(PREF_IS_CONVERSATION_ROOM_ACTIVE, false)
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
                Log.d("ABCF", "observeNetwork() called and has seen points")
                conversationRoomQuestionId?.let {
                    viewModel.getPointsForConversationRoom(
                        lastRoomId,
                        conversationRoomQuestionId
                    )
                }
            }
        }
        viewModel.getListRooms()
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
        roomId: Int?,
        moderatorId: Int?,
        topic: String,
    ) {
        Log.d(
            "ABC2",
            "openConversationLiveRoom() called with: channelName = $channelName, uid = $uid, token = $token, isRoomCreatedByUser = $isRoomCreatedByUser, roomId = $roomId, moderatorId = $moderatorId, topic = $topic"
        )
        lastRoomId = roomId.toString()

        this.startActivity(
            ConversationLiveRoomActivity.getIntent(
                context = requireActivity(),
                channelName = channelName,
                uid = uid,
                token = token,
                isRoomCreatedByUser = isRoomCreatedByUser,
                roomId = roomId,
                moderatorId = moderatorId,
                roomQuestionId = conversationRoomQuestionId,
                topicName = topic
            )
        )
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
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        val inflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.alert_label_editor, null)
        dialogBuilder.setView(dialogView)

        val alertDialog: AlertDialog = dialogBuilder.create()
        alertDialog.apply {
            this.show()
            this.window?.let { window ->
                val width = AppObjectController.screenWidth * .91
                val height = ViewGroup.LayoutParams.WRAP_CONTENT
                val wlp: WindowManager.LayoutParams = window.getAttributes()
                wlp.gravity = Gravity.BOTTOM
                window.setAttributes(wlp)
                window.setLayout(width.toInt(), height)
                window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            }
        }

        dialogView.apply {
            this.findViewById<EditText>(R.id.label_field).requestFocus()
            this.findViewById<EditText>(R.id.label_field).isFocusable = true

            this.findViewById<MaterialButton>(R.id.create_room).setOnSingleClickListener {
                if (this.findViewById<EditText>(R.id.label_field).text.toString()
                        .isNotBlank()
                ) {
                    showPartnerChooserPopup(this.findViewById<EditText>(R.id.label_field).text.toString())
                    hideKeyboard(requireActivity())
                    alertDialog.dismiss()
                } else {
                    showToast("Please enter Topic name")
                }
            }
        }

    }

    private fun showPartnerChooserPopup(topic: String) {
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        val inflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.alert_room_picker, null)
        dialogBuilder.setView(dialogView)
        var isP2Pselected = true

        val alertDialog: AlertDialog = dialogBuilder.create()
        alertDialog.apply {
            this.show()
            this.window?.let { window ->
                val width = AppObjectController.screenWidth * .91
                val height = ViewGroup.LayoutParams.WRAP_CONTENT
                val wlp: WindowManager.LayoutParams = window.getAttributes()
                wlp.gravity = Gravity.BOTTOM
                window.setAttributes(wlp)
                window.setLayout(width.toInt(), height)
                window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            }
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
            val temp2 = getString(R.string.convo_room_dialog_desc_favt)
            val sBuilder2 = SpannableStringBuilder(temp2)
            sBuilder2.setSpan(
                StyleSpan(Typeface.BOLD),
                26,
                temp2.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            dialogView.findViewById<TextView>(R.id.tip).setText(
                sBuilder2,
                TextView.BufferType.SPANNABLE
            )
            isP2Pselected = false
        }
    }

    override fun onStart() {
        super.onStart()
        pubnub?.subscribe()?.channels(
            Arrays.asList("conversation_room_broadcast")
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
        replaySubject = ReplaySubject.create<Any>()
        compositeDisposable.clear()
    }

    override fun onDestroy() {
        if (!isBackPressed) {
            //viewModel.makeEnterExitConversationRoom(false)
        }
        super.onDestroy()
        pubnub?.destroy()
        isRoomCreatedByUser = false
    }

    private fun setUpRecyclerView() {
        conversationRoomsListAdapter = ConversationRoomsListAdapter(this)
    }

    private fun addItemsToAdapter(items: ArrayList<RoomListResponseItem>) {
        conversationRoomsListAdapter?.addItems(items)
    }

    private fun addNewItemsToAdapter(items: RoomListResponseItem) {
        conversationRoomsListAdapter?.addSingleItem(items)
    }

    private fun removeItemsFromAdapter(items: RoomListResponseItem) {
        conversationRoomsListAdapter?.removeSingleItem(items)
    }

    private fun updateItemInAdapter(items: RoomListResponseItem, isUserLeaving: Boolean) {
        conversationRoomsListAdapter?.updateItemWithoutPosition(items, isUserLeaving)
    }

    override fun onRoomClick(item: RoomListResponseItem) {
        if (PermissionUtils.isCallingPermissionWithoutLocationEnabled(requireActivity())) {
            openRoom(item)
            return
        }

        PermissionUtils.onlyCallingFeaturePermission(
            requireActivity(),
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            openRoom(item)
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

    private fun openRoom(item: RoomListResponseItem) {
        FullScreenProgressDialog.showProgressBar(requireActivity())
        item.conversationRoomQuestionId = conversationRoomQuestionId
        lastRoomId = (item.roomId).toString()
        viewModel.joinRoom(item)
    }
}
