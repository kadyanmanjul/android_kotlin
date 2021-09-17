package com.joshtalks.joshskills.ui.lesson.room

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.conversationRoom.liveRooms.ConversationLiveRoomActivity
import com.joshtalks.joshskills.conversationRoom.roomsListing.ConversationRoomListingNavigation
import com.joshtalks.joshskills.conversationRoom.roomsListing.ConversationRoomListingViewModel
import com.joshtalks.joshskills.conversationRoom.roomsListing.ConversationRoomsListingAdapter
import com.joshtalks.joshskills.conversationRoom.roomsListing.ConversationRoomsListingItem
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CONVO_ROOM_POINTS
import com.joshtalks.joshskills.core.CoreJoshFragment
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.HAS_SEEN_CONVO_ROOM_POINTS
import com.joshtalks.joshskills.core.IS_CONVERSATION_ROOM_ACTIVE
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.custom_ui.FullScreenProgressDialog
import com.joshtalks.joshskills.core.hideKeyboard
import com.joshtalks.joshskills.core.interfaces.ConversationRoomListAction
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.ActivityConversationsRoomsListingBinding
import com.joshtalks.joshskills.repository.local.entity.CHAT_TYPE
import com.joshtalks.joshskills.repository.local.entity.QUESTION_STATUS
import com.joshtalks.joshskills.ui.extra.setOnSingleClickListener
import com.joshtalks.joshskills.ui.lesson.LessonActivityListener
import com.joshtalks.joshskills.ui.lesson.LessonViewModel
import com.joshtalks.joshskills.ui.lesson.ROOM_POSITION
import com.joshtalks.joshskills.ui.voip.WebRtcService
import com.joshtalks.joshskills.ui.voip.WebRtcService.Companion.isConversionRoomActive
import com.joshtalks.joshskills.ui.voip.WebRtcService.Companion.isRoomCreatedByUser
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class ConversationRoomListingFragment : CoreJoshFragment(),
    ConversationRoomListAction {

    private val db = FirebaseFirestore.getInstance()
    private val notebookRef = db.collection("conversation_rooms")
    private var conversationRoomsListingAdapter: ConversationRoomsListingAdapter? = null
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
    var isActivityOpenFromNotification: Boolean = false
    var roomId: String = ""
    var lastRoomId: String? = null
    var handler: Handler? = null
    var runnable: Runnable? = null

    companion object {
        var CONVERSATION_ROOM_VISIBLE_TRACK_FLAG: Boolean = true

        @JvmStatic
        fun getInstance() = ConversationRoomListingFragment()
    }

    /* override fun onNewIntent(intent: Intent?) {
         super.onNewIntent(intent)
         getIntentExtras(intent)
         openConversationRoomByNotificationIntent()
     }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PrefManager.put(IS_CONVERSATION_ROOM_ACTIVE, true)
        PrefManager.put(HAS_SEEN_CONVO_ROOM_POINTS, true)
        PrefManager.put(CONVO_ROOM_POINTS, EMPTY)
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
        initViews()
        addObservers()
        openConversationRoomByNotificationIntent()
        // showTooltip()
        return binding.root
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
            if (response.alreadyConversed != null && response.alreadyConversed > 1) {
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
                    showAddTopicPopup()
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
                adapter = conversationRoomsListingAdapter
            }
        }
    }

    private fun openConversationRoomByNotificationIntent() {
        if (isActivityOpenFromNotification && roomId.isNotEmpty()) {
            lastRoomId = roomId
            Handler(Looper.getMainLooper()).postDelayed({
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
            }, 200)
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
                val point = PrefManager.getStringValue(CONVO_ROOM_POINTS)
                if (point.isNotBlank()) {
                    showSnackBar(binding.rootView, Snackbar.LENGTH_LONG,point)
                    PrefManager.put(CONVO_ROOM_POINTS, EMPTY)
                } else {
                    viewModel.getPointsForConversationRoom(lastRoomId,it)
                }
                PrefManager.put(HAS_SEEN_CONVO_ROOM_POINTS, true)
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

    private fun openConversationLiveRoom(
        channelName: String?,
        uid: Int?,
        token: String?,
        isRoomCreatedByUser: Boolean,
        roomId: Int?
    ) {
        CONVERSATION_ROOM_VISIBLE_TRACK_FLAG = false
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
            Toast.makeText(requireActivity(), "Something Went Wrong !!!", Toast.LENGTH_SHORT).show()
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
            if (dialogView.findViewById<EditText>(R.id.label_field).text.toString().isNotBlank()) {
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
            dialogView.findViewById<MaterialCardView>(R.id.favt_container).visibility = View.VISIBLE
        } else {
            dialogView.findViewById<MaterialCardView>(R.id.favt_container).visibility = View.GONE
        }

        dialogView.findViewById<MaterialButton>(R.id.create_room).setOnClickListener {
            FullScreenProgressDialog.showProgressBar(requireActivity())
            viewModel.createRoom(topic, isP2Pselected.not(), conversationRoomQuestionId)
            alertDialog.dismiss()
        }
        dialogView.findViewById<MaterialCardView>(R.id.p2p_container).setOnClickListener {
            dialogView.findViewById<MaterialCardView>(R.id.p2p_container).setCardBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.artboard_color
                )
            )
            dialogView.findViewById<MaterialCardView>(R.id.favt_container).setCardBackgroundColor(
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
            isP2Pselected = true
        }
        dialogView.findViewById<MaterialCardView>(R.id.favt_container).setOnClickListener {
            dialogView.findViewById<MaterialCardView>(R.id.p2p_container).setCardBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                )
            )
            dialogView.findViewById<MaterialCardView>(R.id.favt_container).setCardBackgroundColor(
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
            isP2Pselected = false
        }
    }

    override fun onStart() {
        super.onStart()
        conversationRoomsListingAdapter?.startListening()
    }

    override fun onStop() {
        super.onStop()
        conversationRoomsListingAdapter?.stopListening()
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
        PrefManager.put(IS_CONVERSATION_ROOM_ACTIVE, false)
        isConversionRoomActive = false
        isRoomCreatedByUser = false
    }

    /*override fun onBackPressed() {
        super.onBackPressed()
        isBackPressed = true
        viewModel.makeEnterExitConversationRoom(false)
    }*/

    private fun setUpRecyclerView() {
        val options: FirestoreRecyclerOptions<ConversationRoomsListingItem> =
            FirestoreRecyclerOptions.Builder<ConversationRoomsListingItem>()
                .setQuery(notebookRef, ConversationRoomsListingItem::class.java)
                .build()
        conversationRoomsListingAdapter = ConversationRoomsListingAdapter(options, this)
        notebookRef.addSnapshotListener { value, error ->
            if (error != null) {
                return@addSnapshotListener
            } else {
                viewModel.checkRoomsAvailableOrNot(value)
            }
        }
    }

    override fun onRoomClick(item: ConversationRoomsListingItem) {
        item.conversationRoomQuestionId = conversationRoomQuestionId
        viewModel.joinRoom(item)
    }
}
