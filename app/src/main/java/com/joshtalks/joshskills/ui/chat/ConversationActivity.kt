package com.joshtalks.joshskills.ui.chat

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.View.*
import android.view.animation.*
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.Player
import com.greentoad.turtlebody.mediapicker.MediaPicker
import com.greentoad.turtlebody.mediapicker.core.MediaPickerConfig
import com.joshtalks.joshcamerax.JoshCameraActivity
import com.joshtalks.joshcamerax.utils.ImageQuality
import com.joshtalks.joshcamerax.utils.Options
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.Utils.getCurrentMediaVolume
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.decorator.EndlessRecyclerViewScrollListener
import com.joshtalks.joshskills.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.core.custom_ui.decorator.SmoothScrollingLinearLayoutManager
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioPlayerEventListener
import com.joshtalks.joshskills.core.extension.*
import com.joshtalks.joshskills.core.interfaces.OnDismissWithSuccess
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.notification.HAS_COURSE_REPORT
import com.joshtalks.joshskills.core.playback.PlaybackInfoListener.State.PAUSED
import com.joshtalks.joshskills.core.service.video_download.VideoDownloadController
import com.joshtalks.joshskills.databinding.ActivityConversationBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.DatabaseUtils
import com.joshtalks.joshskills.repository.local.entity.*
import com.joshtalks.joshskills.repository.local.eventbus.*
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.Award
import com.joshtalks.joshskills.repository.server.UserProfileResponse
import com.joshtalks.joshskills.repository.server.chat_message.*
import com.joshtalks.joshskills.ui.assessment.AssessmentActivity
import com.joshtalks.joshskills.ui.certification_exam.CertificationBaseActivity
import com.joshtalks.joshskills.ui.chat.adapter.ConversationAdapter
import com.joshtalks.joshskills.ui.chat.service.DownloadMediaService
import com.joshtalks.joshskills.ui.conversation_practice.ConversationPracticeActivity
import com.joshtalks.joshskills.ui.course_progress_new.CourseProgressActivityNew
import com.joshtalks.joshskills.ui.course_progress_new.CourseProgressTooltip
import com.joshtalks.joshskills.ui.courseprogress.CourseProgressActivity
import com.joshtalks.joshskills.ui.extra.ImageShowFragment
import com.joshtalks.joshskills.ui.groupchat.listeners.StickyHeaderDecoration
import com.joshtalks.joshskills.ui.groupchat.messagelist.CometChatMessageListActivity
import com.joshtalks.joshskills.ui.lesson.LessonActivity
import com.joshtalks.joshskills.ui.pdfviewer.PdfViewerActivity
import com.joshtalks.joshskills.ui.practise.PRACTISE_OBJECT
import com.joshtalks.joshskills.ui.practise.PractiseSubmitActivity
import com.joshtalks.joshskills.ui.referral.ReferralActivity
import com.joshtalks.joshskills.ui.userprofile.UserProfileActivity
import com.joshtalks.joshskills.ui.video_player.*
import com.joshtalks.joshskills.ui.view_holders.*
import com.joshtalks.joshskills.util.ExoAudioPlayer
import com.joshtalks.recordview.CustomImageButton.FIRST_STATE
import com.joshtalks.recordview.CustomImageButton.SECOND_STATE
import com.joshtalks.recordview.OnRecordListener
import com.joshtalks.recordview.OnRecordTouchListener
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.muddzdev.styleabletoast.StyleableToast
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_inbox.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.scheduleAtFixedRate


const val CHAT_ROOM_OBJECT = "chat_room"
const val UPDATED_CHAT_ROOM_OBJECT = "updated_chat_room"
const val CHAT_ROOM_ID = "chat_room_id"

const val IMAGE_SELECT_REQUEST_CODE = 1077
const val VISIBLE_ITEM_PERCENT = 75
const val PRACTISE_SUBMIT_REQUEST_CODE = 1100
const val COURSE_PROGRESS_REQUEST_CODE = 1101
const val VIDEO_OPEN_REQUEST_CODE = 1102
const val CONVERSATION_PRACTISE_REQUEST_CODE = 1105
const val ASSESSMENT_REQUEST_CODE = 1106
const val LESSON_REQUEST_CODE = 1107
const val CERTIFICATION_REQUEST_CODE = 1108
const val COURSE_PROGRESS_NEW_REQUEST_CODE = 1109


const val PRACTISE_UPDATE_MESSAGE_KEY = "practise_update_message_id"
const val FOCUS_ON_CHAT_ID = "focus_on_chat_id"


class ConversationActivity : BaseConversationActivity(), Player.EventListener,
    ExoAudioPlayer.ProgressUpdateListener, AudioPlayerEventListener, OnDismissWithSuccess,
    CourseProgressTooltip.OnDismissClick {

    companion object {
        fun startConversionActivity(activity: Activity, inboxEntity: InboxEntity) {
            Intent(activity, ConversationActivity::class.java).apply {
                putExtra(CHAT_ROOM_OBJECT, inboxEntity)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }.run {
                activity.startActivity(this)
            }
        }
    }

    private lateinit var conversationViewModel: ConversationViewModel
    private lateinit var utilConversationViewModel: UtilConversationViewModel
    private lateinit var unlockClassViewModel: UnlockClassViewModel
    private val conversationAdapter: ConversationAdapter by lazy {
        ConversationAdapter(
            WeakReference(
                this
            )
        )
    }
    private var userProfileData: UserProfileResponse? = null
    private var currentAudioPosition: Int = -1
    private lateinit var conversationBinding: ActivityConversationBinding
    private lateinit var inboxEntity: InboxEntity
    private lateinit var activityRef: WeakReference<FragmentActivity>
    private lateinit var linearLayoutManager: SmoothScrollingLinearLayoutManager
    private var revealAttachmentView: Boolean = false
    private val readChatList: MutableSet<ChatModel> = mutableSetOf()
    private var readMessageTimerTask: TimerTask? = null
    private var audioPlayerManager: ExoAudioPlayer? = null
    private var isOnlyChat = false
    private var flowFrom: String? = EMPTY
    private var loadingPreviousData = false
    private var isNewMessageShowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        conversationBinding = DataBindingUtil.setContentView(this, R.layout.activity_conversation)
        conversationBinding.handler = this
        activityRef = WeakReference(this)
        initIntentObject()
        init()
    }

    private fun initIntentObject() {
        if (intent.hasExtra(CHAT_ROOM_OBJECT)) {
            flowFrom = "Inbox journey"
            val temp = intent.getParcelableExtra(CHAT_ROOM_OBJECT) as InboxEntity?
            if (temp == null) {
                this@ConversationActivity.finish()
                return
            }
            inboxEntity = temp
        }
        if (intent.hasExtra(UPDATED_CHAT_ROOM_OBJECT)) {
            flowFrom = "Notification"
            val temp = intent.getParcelableExtra(UPDATED_CHAT_ROOM_OBJECT) as InboxEntity?
            if (temp == null) {
                this@ConversationActivity.finish()
                return
            }
            inboxEntity = temp
        }
        if (intent.hasExtra(HAS_COURSE_REPORT)) {
            openCourseProgressListingScreen()
        }
        if (intent.hasExtra(FOCUS_ON_CHAT_ID)) {
            intent.getParcelableExtra<ChatModel>(FOCUS_ON_CHAT_ID)?.chatId?.run {
                scrollToPosition(this)
            }
        }
        conversationViewModel = ViewModelProvider(
            this, ConversationViewModelFactory(this, this.application, inboxEntity)
        ).get(ConversationViewModel::class.java)
        conversationBinding.viewmodel = conversationViewModel
        conversationBinding.lifecycleOwner = this

        utilConversationViewModel = ViewModelProvider(
            this, ConversationViewModelFactory(this, this.application, inboxEntity)
        ).get(UtilConversationViewModel::class.java)
        unlockClassViewModel = ViewModelProvider(
            this, ConversationViewModelFactory(this, this.application, inboxEntity)
        ).get(UnlockClassViewModel::class.java)

        super.processIntent(intent)
    }

    private fun init() {
        initToolbar()
        initRV()
        initView()
        initFuture()
        groupChatHintLogic()
        initCourseProgressTooltip()
        addObservable()
        fetchMessage()
        readMessageDatabaseUpdate()
    }

    private fun initToolbar() {
        try {
            conversationBinding.textMessageTitle.text = inboxEntity.course_name
            conversationBinding.imageViewLogo.setImageWithPlaceholder(inboxEntity.course_icon)
            conversationBinding.imageViewLogo.visibility = VISIBLE
            conversationBinding.imageViewLogo.setOnClickListener {
                openCourseProgressListingScreen()
            }
            conversationBinding.textMessageTitle.setOnClickListener {
                openCourseProgressListingScreen()
            }

            conversationBinding.ivBack.setOnClickListener {
                val resultIntent = Intent()
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }

            conversationBinding.toolbar.inflateMenu(R.menu.conversation_menu)
            conversationBinding.toolbar.setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.menu_referral -> {
                        ReferralActivity.startReferralActivity(
                            this@ConversationActivity,
                            ConversationActivity::class.java.name
                        )
                    }
                    R.id.menu_clear_media -> {
                        clearMediaFromInternal(inboxEntity.conversation_id)
                    }
                    R.id.menu_help -> {
                        openHelpActivity()
                    }
                    R.id.profile_setting -> {
                        openUserProfileActivity(
                            Mentor.getInstance().getId(),
                            USER_PROFILE_FLOW_FROM.MENU.value
                        )
                    }
                    R.id.leaderboard_setting -> {
                        openLeaderBoard()
                    }
                }
                return@setOnMenuItemClickListener true
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun initRV() {
        conversationBinding.chatRv.setHasFixedSize(false)
        linearLayoutManager = SmoothScrollingLinearLayoutManager(this, false)
        linearLayoutManager.stackFromEnd = true
        //linearLayoutManager.isItemPrefetchEnabled = true
        //linearLayoutManager.initialPrefetchItemCount = 10
        linearLayoutManager.isSmoothScrollbarEnabled = true
        conversationBinding.chatRv.apply {
            addItemDecoration(LayoutMarginDecoration(Utils.dpToPx(context, 4f)))
        }
        conversationBinding.chatRv.layoutManager = linearLayoutManager
        conversationBinding.chatRv.itemAnimator = null


        conversationBinding.chatRv.addItemDecoration(StickyHeaderDecoration(conversationAdapter), 0)
        conversationBinding.chatRv.adapter = conversationAdapter


        conversationBinding.chatRv.addOnScrollListener(object :
            EndlessRecyclerViewScrollListener(linearLayoutManager, LoadOnScrollDirection.TOP) {
            override fun onLoadMore(page: Int, totalItemsCount: Int) {
                if (conversationAdapter.itemCount == 0) {
                    return
                }
                if (loadingPreviousData.not()) {
                    conversationViewModel.loadPagingMessage(conversationAdapter.getFirstItem())
                    loadingPreviousData = true
                }
            }
        })
        conversationBinding.chatRv.addOnScrollListener(object :
            EndlessRecyclerViewScrollListener(linearLayoutManager, LoadOnScrollDirection.BOTTOM) {
            override fun onLoadMore(page: Int, totalItemsCount: Int) {
                if (conversationAdapter.itemCount == 0) {
                    return
                }
            }
        })

        conversationBinding.chatRv.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!recyclerView.canScrollVertically(1) && newState == RecyclerView.SCROLL_STATE_IDLE) {
                    conversationBinding.scrollToEndButton.visibility = GONE
                } else if (recyclerView.canScrollVertically(-1) && newState == RecyclerView.SCROLL_STATE_IDLE) {
                    conversationBinding.scrollToEndButton.visibility = VISIBLE
                }
                visibleItem()
            }
        })
    }

    private fun initView() {
        conversationBinding.scrollToEndButton.setOnClickListener {
            scrollToEnd()
        }

        conversationBinding.leaderboardBtnClose.setOnClickListener {
            conversationBinding.userPointContainer.moveViewToScreenCenter(
                conversationBinding.imgGroupChat,
                conversationBinding.txtUnreadCount
            )
        }

        conversationBinding.leaderboardTxt.setOnClickListener {
            openLeaderBoard()
        }
        conversationBinding.points.setOnClickListener {
            openUserProfileActivity(
                Mentor.getInstance().getId(),
                USER_PROFILE_FLOW_FROM.FLOATING_BAR.value
            )
        }

        conversationBinding.imgGroupChat.visibility =
            if (inboxEntity.isGroupActive) VISIBLE else GONE

        conversationBinding.imgGroupChat.setOnClickListener {
            utilConversationViewModel.initCometChat()
        }
        conversationBinding.imgGroupChatOverlay.setOnClickListener {
            conversationBinding.overlayLayout.visibility = GONE
            utilConversationViewModel.initCometChat()
        }
        conversationBinding.refreshLayout.setOnRefreshListener {
            if (internetAvailableFlag) {
                conversationBinding.refreshLayout.isRefreshing = true
                conversationViewModel.refreshChatOnManual()
            } else {
                conversationBinding.refreshLayout.isRefreshing = false
            }
        }

        findViewById<View>(R.id.ll_audio).setOnClickListener {
            AppAnalytics.create(AnalyticsEvent.AUDIO_SELECTED.NAME).push()
            addAttachmentUIUpdate()
            PermissionUtils.storageReadAndWritePermission(activityRef.get()!!,
                object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.areAllPermissionsGranted()?.let { flag ->
                            if (flag) {
                                bottomAudioAttachment()
                                return

                            }
                            if (report.isAnyPermissionPermanentlyDenied) {
                                PermissionUtils.permissionPermanentlyDeniedDialog(
                                    activityRef.get()!!
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
                })

        }

        findViewById<View>(R.id.ll_camera).setOnClickListener {
            AppAnalytics.create(AnalyticsEvent.CAMERA_SELECTED.NAME).push()
            addAttachmentUIUpdate()
            uploadImageByUser()
        }

    }

    private fun initFuture() {
        AppAnalytics.create(AnalyticsEvent.COURSE_OPENED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.FLOW_FROM_PARAM.NAME, flowFrom)
            .push()
        audioPlayerManager = ExoAudioPlayer.getInstance()
        audioPlayerManager?.setProgressUpdateListener(this@ConversationActivity)
        audioPlayerManager?.playerListener = this@ConversationActivity
        onlyChatView()
        initSnackBar()
    }

    private fun groupChatHintLogic() {
        val isGroupChatHintAlreadySeen = PrefManager.getBoolValue(IS_GROUP_CHAT_HINT_SEEN, true)
        if (inboxEntity.isGroupActive && isGroupChatHintAlreadySeen.not()) {
            val lastLesson =
                conversationViewModel.conversationList.lastOrNull { it.lesson != null }
            lastLesson?.lesson?.let {
                if (it.lessonNo > 3 || (it.lessonNo == 3 && it.status != LESSON_STATUS.NO)) {
                    conversationBinding.balloonText.text =
                        AppObjectController.getFirebaseRemoteConfig()
                            .getString(FirebaseRemoteConfigKey.GROUP_CHAT_TAGLINE)
                    conversationBinding.overlayLayout.visibility = VISIBLE
                    PrefManager.put(IS_GROUP_CHAT_HINT_SEEN, true, true)
                }
            }
        }
    }

    private fun initCourseProgressTooltip() {
        CoroutineScope(Dispatchers.Main).launch {
            delay(5000)
            if (PrefManager.getBoolValue(LESSON_TWO_OPENED) && PrefManager.getBoolValue(
                    COURSE_PROGRESS_OPENED
                ).not() && inboxEntity.isCapsuleCourse
            ) {
                showCourseProgressTooltip()
            }
        }
    }


    private fun onlyChatView() {
        inboxEntity.chat_type?.let {
            when {
                it.equals("NC", ignoreCase = true) -> {
                    isOnlyChat = true
                    conversationBinding.flAttachment.visibility = GONE
                    conversationBinding.quickToggle.visibility = GONE
                    conversationBinding.recordButton.visibility = INVISIBLE
                    conversationBinding.attachmentContainer.visibility = GONE
                    conversationBinding.messageButton.visibility = VISIBLE
                    conversationBinding.messageButton.setImageResource(R.drawable.ic_send)
                    conversationBinding.messageButton.setResourceImageDefault(R.drawable.ic_send)
                    initInputUI()
                }
                it.equals("RC", ignoreCase = true) -> {
                    conversationBinding.bottomBar.visibility = GONE
                }
                else -> {
                    initRecordUI()
                    initInputUI()
                }

            }
        }
    }

    private fun initRecordUI() {
        conversationBinding.recordButton.setRecordView(conversationBinding.recordView)
        conversationBinding.recordView.cancelBounds = 2f
        conversationBinding.recordView.setSmallMicColor(Color.parseColor("#c2185b"))
        conversationBinding.recordView.setLessThanSecondAllowed(false)
        conversationBinding.recordView.setSlideToCancelText(getString(R.string.slide_to_cancel))
        conversationBinding.recordView.setCustomSounds(R.raw.record_start, R.raw.record_finished, 0)
        conversationBinding.recordButton.isListenForRecord =
            PermissionUtils.checkPermissionForAudioRecord(this@ConversationActivity)
        conversationBinding.recordView.setOnRecordListener(object : OnRecordListener {
            override fun onStart() {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                AppAnalytics.create(AnalyticsEvent.AUDIO_BUTTON_CLICKED.NAME).push()
                conversationBinding.recordView.visibility = VISIBLE
                conversationViewModel.startRecord(null)
                AppAnalytics.create(AnalyticsEvent.AUDIO_RECORD.NAME).push()
            }

            override fun onCancel() {
                conversationViewModel.stopRecording(true)
            }

            override fun onFinish(recordTime: Long) {
                try {
                    AppAnalytics.create(AnalyticsEvent.AUDIO_SENT.NAME).push()
                    conversationBinding.recordView.visibility = GONE
                    conversationViewModel.stopRecording(false)
                    conversationViewModel.recordFile.let {
                        addRecordedAudioMessage(it.absolutePath)
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }

            override fun onLessThanSecond() {
                conversationBinding.recordView.visibility = GONE
                conversationViewModel.stopRecording(true)
                AppAnalytics.create(AnalyticsEvent.AUDIO_CANCELLED.NAME).push()
            }
        })

        conversationBinding.recordView.setOnBasketAnimationEndListener {
            conversationBinding.recordView.visibility = GONE
            conversationViewModel.stopRecording(true)
            AppAnalytics.create(AnalyticsEvent.AUDIO_CANCELLED.NAME).push()
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        conversationBinding.recordButton.setOnTouchListener(OnRecordTouchListener {
            if (conversationBinding.chatEdit.text.toString()
                    .isEmpty() && it == MotionEvent.ACTION_DOWN
            ) {
                if (PermissionUtils.isAudioAndStoragePermissionEnable(this).not()) {
                    PermissionUtils.audioRecordStorageReadAndWritePermission(activityRef.get()!!,
                        object : MultiplePermissionsListener {
                            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                                report?.areAllPermissionsGranted()?.let { flag ->
                                    if (flag) {
                                        conversationBinding.recordButton.isListenForRecord =
                                            true
                                        return@let
                                    }
                                    if (report.isAnyPermissionPermanentlyDenied) {
                                        PermissionUtils.permissionPermanentlyDeniedDialog(
                                            this@ConversationActivity,
                                            R.string.record_permission_message
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
                        })
                } else {
                    conversationBinding.recordButton.isListenForRecord = true
                }
            }
        })

        conversationBinding.recordButton.setOnRecordClickListener {
            sendTextMessage()
        }

    }

    private fun initInputUI() {
        conversationBinding.chatEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty()) {
                    conversationBinding.recordButton.goToState(FIRST_STATE)
                    conversationBinding.recordButton.isListenForRecord =
                        PermissionUtils.checkPermissionForAudioRecord(this@ConversationActivity)
                    if (isOnlyChat.not()) {
                        conversationBinding.quickToggle.show()
                    }

                } else {
                    conversationBinding.recordButton.goToState(SECOND_STATE)
                    conversationBinding.recordButton.isListenForRecord = false
                    conversationBinding.quickToggle.hide()
                }

            }

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
            }

        })
        conversationBinding.messageButton.setOnClickListener {
            sendTextMessage()
        }
    }

    private fun openUserProfileActivity(id: String, previousPage: String?) {
        UserProfileActivity.startUserProfileActivity(
            this,
            id,
            arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
            null,
            previousPage
        )
    }

    private fun fetchMessage() {
        conversationViewModel.getAllCourseMessage()
    }

    private fun addObservable() {
        lifecycleScope.launchWhenResumed {
            utilConversationViewModel.unreadMessageCount.collectLatest { count ->
                if (inboxEntity.isGroupActive) {
                    conversationBinding.txtUnreadCount.visibility = VISIBLE
                    conversationBinding.imgGroupChat.visibility = VISIBLE
                    when {
                        count in 1..99 -> {
                            conversationBinding.txtUnreadCount.text = String.format("%d", count)
                        }
                        count > 99 -> {
                            conversationBinding.txtUnreadCount.text =
                                getString(R.string.max_unread_count)
                        }
                        else -> {
                            conversationBinding.txtUnreadCount.visibility = GONE
                            conversationBinding.txtUnreadCount.text = EMPTY
                        }
                    }
                } else {
                    conversationBinding.imgGroupChat.visibility = GONE
                    conversationBinding.txtUnreadCount.visibility = GONE
                }

            }
        }

        lifecycleScope.launchWhenResumed {
            utilConversationViewModel.userData.collectLatest { userProfileData ->
                this@ConversationActivity.userProfileData = userProfileData
                if (conversationBinding.courseProgressTooltip.visibility != VISIBLE) {
                    initScoreCardView(userProfileData)
                    profileFeatureActiveView()
                }
            }
        }

        lifecycleScope.launchWhenCreated {
            conversationViewModel.userUnreadCourseChat.collectLatest { items ->
                //  Start Add new Message UI add logic
                if (items.isEmpty()) {
                    return@collectLatest
                }
                if (isNewMessageShowing.not()) {
                    val index = items.indexOfFirst { it.isSeen.not() }
                    if (index > -1) {
                        conversationAdapter.addMessagesList(arrayListOf(getNewMessageObj()))
                        linearLayoutManager.scrollToPositionWithOffset(
                            conversationAdapter.itemCount + index,
                            40
                        )
                        isNewMessageShowing = true
                    }
                }
                //End Logic

                conversationAdapter.addMessagesList(items)
            }
        }
        lifecycleScope.launchWhenCreated {
            conversationViewModel.userReadCourseChat.collectLatest { items ->
                if (items.isNotEmpty()) {
                    conversationAdapter.addMessagesList(items)
                }
            }
        }
        lifecycleScope.launchWhenCreated {
            conversationViewModel.oldMessageCourse.collectLatest { items ->
                conversationAdapter.addMessageAboveMessage(items)
                loadingPreviousData = false
            }
        }
        lifecycleScope.launchWhenCreated {
            conversationViewModel.updateChatMessage.collectLatest { chat ->
                conversationAdapter.updateItem(chat)
                unlockClassViewModel.canWeAddUnlockNextClass(chat.chatId)
            }
        }
        lifecycleScope.launchWhenCreated {
            unlockClassViewModel.unlockNextClass.collectLatest { flag ->
                hideProgressBar()
                if (flag) {
                    val message = getUnlockClassMessage()
                    unlockClassViewModel.insertUnlockClassToDatabase(message)
                    val isAdded = conversationAdapter.addUnlockClassMessage(message)
                    val cPosition = linearLayoutManager.findLastVisibleItemPosition()
                    if (isAdded && cPosition == conversationAdapter.itemCount - 1) {
                        conversationBinding.chatRv.smoothScrollToPosition(conversationAdapter.itemCount - 1)
                    }
                }
            }
        }
        lifecycleScope.launchWhenCreated {
            unlockClassViewModel.batchChange.collectLatest {
                if (it) {
                    conversationViewModel.refreshChatOnManual()
                } else {
                    conversationBinding.refreshLayout.isRefreshing = false
                }
            }
        }
        utilConversationViewModel.userLoginLiveData.observe(this, {
            CometChatMessageListActivity.showGroupChatScreen(this, it)
        })

        utilConversationViewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                showProgressBar()
            } else {
                hideProgressBar()
            }
        }
    }

    private fun bottomAudioAttachment() {
        addAttachmentUIUpdate()
        val pickerConfig = MediaPickerConfig()
            .setUriPermanentAccess(true)
            .setAllowMultiSelection(false)
            .setScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        MediaPicker.with(this, MediaPicker.MediaTypes.AUDIO)
            .setConfig(pickerConfig)
            .onResult()
            .subscribeOn(Schedulers.io())
            .subscribe({
                it?.getOrNull(0)?.path?.let { path ->
                    if (path.isNotBlank()) {
                        AppAnalytics.create(AnalyticsEvent.AUDIO_SENT.NAME).push()
                        addAudioFromBottomBar(Utils.getPathFromUri(path))
                    }
                }
            }, {
            })
    }


    private fun profileFeatureActiveView() {
        if (PrefManager.getBoolValue(IS_PROFILE_FEATURE_ACTIVE)) {
            conversationBinding.toolbar.menu.findItem(R.id.leaderboard_setting).isVisible = true
            conversationBinding.toolbar.menu.findItem(R.id.leaderboard_setting).isEnabled = true
            conversationBinding.toolbar.menu.findItem(R.id.profile_setting).isVisible = true
            conversationBinding.toolbar.menu.findItem(R.id.profile_setting).isEnabled = true
        } else {
            conversationBinding.toolbar.menu.findItem(R.id.leaderboard_setting).isVisible = false
            conversationBinding.toolbar.menu.findItem(R.id.leaderboard_setting).isEnabled = false
            conversationBinding.toolbar.menu.findItem(R.id.profile_setting).isVisible = false
            conversationBinding.toolbar.menu.findItem(R.id.profile_setting).isEnabled = false
        }
    }

    private fun initScoreCardView(userData: UserProfileResponse) {
        userData.isPointsActive?.let { isLeaderBoardActive ->
            if (isLeaderBoardActive) {
                conversationBinding.userPointContainer.visibility = VISIBLE
                conversationBinding.points.text = userData.points.toString().plus(" Points")
                conversationBinding.imgGroupChat.shiftGroupChatIconDown(conversationBinding.txtUnreadCount)
            } else {
                conversationBinding.userPointContainer.visibility = GONE
                conversationBinding.imgGroupChat.shiftGroupChatIconUp(conversationBinding.txtUnreadCount)
            }
        }
        val unseenAwards: ArrayList<Award> = ArrayList()
        userData.awardCategory?.parallelStream()?.forEach { ac ->
            ac.awards?.filter { it.isSeen == false && it.is_achieved }
                ?.let { unseenAwards.addAll(it) }
        }
        if (unseenAwards.isNotEmpty()) {
            showAward(unseenAwards)
        }
    }

    private fun showCourseProgressTooltip() {
        if (AppObjectController.getFirebaseRemoteConfig()
                .getBoolean(FirebaseRemoteConfigKey.COURSE_PROGRESS_TOOLTIP_VISIBILITY)
        ) {
            conversationBinding.courseProgressTooltip.setDismissListener(this)
            conversationBinding.courseProgressTooltip.visibility = VISIBLE
            conversationBinding.shader.visibility = VISIBLE

            if (conversationBinding.userPointContainer.visibility == VISIBLE) {
                conversationBinding.userPointContainer.visibility = GONE
                conversationBinding.imgGroupChat.shiftGroupChatIconUp(conversationBinding.txtUnreadCount)
            }
        }
    }

    private fun hideCourseProgressTooltip() {
        if (conversationBinding.courseProgressTooltip.visibility == VISIBLE) {
            conversationBinding.courseProgressTooltip.moveViewToScreenCenter(
                conversationBinding.imgGroupChat,
                conversationBinding.txtUnreadCount
            )
        }
        conversationBinding.courseProgressTooltip.visibility = GONE
        conversationBinding.shader.visibility = GONE

        userProfileData?.let {
            initScoreCardView(it)
            profileFeatureActiveView()
        }
    }


    private fun subscribeRXBus() {
        compositeDisposable.add(
            RxBus2.listen(DBInsertion::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe({
                    val time = try {
                        conversationAdapter.getLastItem().created.time
                    } catch (ex: Exception) {
                        0L
                    }
                    conversationViewModel.addNewMessages(time)
                }, {
                    it.printStackTrace()
                })
        )
        compositeDisposable.add(
            RxBus2.listen(PlayVideoEvent::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    conversationViewModel.setMRefreshControl(false)
                    VideoPlayerActivity.startConversionActivity(
                        this,
                        it.chatModel,
                        inboxEntity.course_name,
                        inboxEntity.duration
                    )
                }, {
                    it.printStackTrace()
                })
        )
        compositeDisposable.add(
            RxBus2.listen(ImageShowEvent::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        Utils.fileUrl(it.localPath, it.serverPath)?.run {
                            ImageShowFragment.newInstance(this, inboxEntity.course_name, it.imageId)
                                .show(supportFragmentManager, "ImageShow")
                        }
                    },
                    {
                        it.printStackTrace()
                    })
        )
        compositeDisposable.add(
            RxBus2.listen(PdfOpenEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    PdfViewerActivity.startPdfActivity(
                        activityRef.get()!!,
                        it.pdfObject.id,
                        inboxEntity.course_name,
                        it.chatId
                    )
                }, {
                    it.printStackTrace()
                })
        )
        compositeDisposable.add(
            RxBus2.listen(MediaProgressEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (it.state == com.google.android.exoplayer2.offline.Download.STATE_COMPLETED) {
                        refreshViewAtPos(
                            AppObjectController.gsonMapperForLocal.fromJson(
                                it.id,
                                ChatModel::class.java
                            )
                        )
                    }

                }, {
                    it.printStackTrace()
                })
        )

        compositeDisposable.add(
            RxBus2.listen(DownloadMediaEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { it ->
                    when (it.downloadStatus) {
                        DOWNLOAD_STATUS.DOWNLOADED -> {
                            conversationViewModel.refreshMessageObject(it.id)
                        }
                        DOWNLOAD_STATUS.DOWNLOADING -> {
                            DownloadMediaService.addDownload(it.chatModel, it.url)
                        }
                        DOWNLOAD_STATUS.REQUEST_DOWNLOADING -> {
                            PermissionUtils.storageReadAndWritePermission(this,
                                object : MultiplePermissionsListener {
                                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                                        report?.areAllPermissionsGranted()?.let { flag ->
                                            if (flag && internetAvailableFlag.not()) {
                                                showToast(getString(R.string.internet_not_available_msz))
                                                return@let
                                            }
                                            val chatModel = it.chatModel
                                            chatModel?.downloadStatus = DOWNLOAD_STATUS.DOWNLOADING
                                            chatModel?.let {
                                                conversationAdapter.updateItem(it)
                                            }
                                            if (it.type == BASE_MESSAGE_TYPE.PD || it.type == BASE_MESSAGE_TYPE.AU) {
                                                DownloadMediaService.addDownload(
                                                    it.chatModel,
                                                    it.url
                                                )
                                            } else if (it.type == BASE_MESSAGE_TYPE.VI) {
                                                AppObjectController.videoDownloadTracker.download(
                                                    it.chatModel,
                                                    Uri.parse(it.url),
                                                    VideoDownloadController.getInstance()
                                                        .buildRenderersFactory(true)
                                                )
                                            }
                                            return
                                        }
                                        report?.isAnyPermissionPermanentlyDenied?.let {
                                            PermissionUtils.permissionPermanentlyDeniedDialog(
                                                activityRef.get()!!
                                            )
                                            return
                                        }
                                    }

                                    override fun onPermissionRationaleShouldBeShown(
                                        permissions: MutableList<PermissionRequest>?,
                                        token: PermissionToken?
                                    ) {
                                        token?.continuePermissionRequest()
                                    }
                                })
                        }
                        else -> {

                        }
                    }
                })

        compositeDisposable.add(RxBus2.listen(DownloadCompletedEventBus::class.java)
            .subscribeOn(Schedulers.computation())
            .subscribe {
                CoroutineScope(Dispatchers.IO).launch {
                    val obj = AppObjectController.appDatabase.chatDao()
                        .getUpdatedChatObjectViaId(it.chatModel.chatId)
                    refreshViewAtPos(obj)
                }

            })
        compositeDisposable.add(RxBus2.listen(VideoDownloadedBus::class.java)
            .subscribeOn(Schedulers.computation())
            .subscribe {
                CoroutineScope(Dispatchers.IO).launch {
                    val chatObj = AppObjectController.appDatabase.chatDao()
                        .getUpdatedChatObjectViaId(it.messageObject.chatId)
                    refreshViewAtPos(chatObj)
                }
            })
        compositeDisposable.add(
            RxBus2.listen(ChatModel::class.java)
                .subscribeOn(Schedulers.computation())
                .subscribe {
                    visibleItem()
                })


        //  Start Block for swipe to refresh and get chat
        compositeDisposable.add(
            RxBus2.listen(MessageCompleteEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (conversationBinding.refreshLayout.isRefreshing) {
                        val message: String = if (it.flag) {
                            getString(R.string.new_message_arrive)
                        } else {
                            getString(R.string.no_new_message_arrive)
                        }
                        StyleableToast.Builder(this).gravity(Gravity.BOTTOM)
                            .text(message).cornerRadius(16).length(Toast.LENGTH_LONG)
                            .solidBackground().show()
                    }
                    if (it.flag.not()) {
                        hideProgressBar()
                    }
                    conversationBinding.refreshLayout.isRefreshing = false
                })
        //End

        compositeDisposable.add(
            RxBus2.listen(AudioPlayEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (getCurrentMediaVolume(applicationContext) <= 0) {
                        StyleableToast.Builder(applicationContext).gravity(Gravity.BOTTOM)
                            .text(getString(R.string.volume_up_message)).cornerRadius(16)
                            .length(Toast.LENGTH_LONG)
                            .solidBackground().show()
                    }
                    if (it.state == PAUSED) {
                        audioPlayerManager?.onPause()
                        return@subscribe
                    }
                    setCurrentItemPosition(it.chatModel.chatId)
                    AppObjectController.currentPlayingAudioObject?.let { chatModel ->
                        refreshViewAtPos(chatModel)
                    }
                    analyticsAudioPlayed(it.audioType)
                    currentAudioPosition =
                        conversationAdapter.getMessagePositionById(it.chatModel.chatId)
                    if (AppObjectController.currentPlayingAudioObject != null && ExoAudioPlayer.LAST_ID == it?.chatModel?.chatId) {
                        audioPlayerManager?.resumeOrPause()
                    } else {
                        AppObjectController.currentPlayingAudioObject = it.chatModel
                        audioPlayerManager?.onPause()
                        setPlayProgress(it.chatModel.playProgress)
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        audioPlayerManager?.play(
                            it.audioType!!.audio_url,
                            it.chatModel.chatId
                        )

                    }
                    DatabaseUtils.updateLastUsedModification(it.chatModel.chatId)
                },
                    {
                        it.printStackTrace()
                    })
        )
        compositeDisposable.add(
            RxBus2.listen(InternalSeekBarProgressEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    audioPlayerManager?.seekTo(it.progress.toLong())
                }
        )
        compositeDisposable.add(
            RxBus2.listen(PractiseSubmitEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    AppAnalytics.create(AnalyticsEvent.PRACTICE_OPENED.NAME)
                        .addBasicParam()
                        .addUserDetails()
                        .addParam(AnalyticsEvent.COURSE_NAME.NAME, inboxEntity.course_name)
                        .addParam(
                            AnalyticsEvent.PRACTICE_SOLVED.NAME,
                            (it.chatModel.question != null) && (it.chatModel.question!!.practiceEngagement.isNullOrEmpty()
                                .not())
                        )
                        .addParam("chatId", it.chatModel.chatId)
                        .push()
                    PractiseSubmitActivity.startPractiseSubmissionActivity(
                        activityRef.get()!!,
                        PRACTISE_SUBMIT_REQUEST_CODE,
                        it.chatModel
                    )
                }, {
                    it.printStackTrace()
                })
        )
        compositeDisposable.add(
            RxBus2.listen(GotoChatEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .subscribe({
                    scrollToPosition(it.chatId)
                }, {
                    it.printStackTrace()
                })
        )

        compositeDisposable.add(
            RxBus2.listen(AssessmentStartEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    logAssessmentEvent(it.assessmentId)
                    AssessmentActivity.startAssessmentActivity(
                        this,
                        requestCode = ASSESSMENT_REQUEST_CODE,
                        assessmentId = it.assessmentId
                    )
                }, {
                    it.printStackTrace()
                })
        )


        compositeDisposable.add(
            RxBus2.listen(UnlockNextClassEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    logUnlockCardEvent()
                    //conversationBinding.chatRv.removeView(it.viewHolder)
                    conversationAdapter.removeUnlockMessage()
                    unlockClassViewModel.updateBatchChangeRequest()
                }, {
                    it.printStackTrace()
                })
        )

        compositeDisposable.add(
            RxBus2.listen(ConversationPractiseEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    ConversationPracticeActivity.startConversationPracticeActivity(
                        this,
                        CONVERSATION_PRACTISE_REQUEST_CODE,
                        it.id,
                        it.pImage
                    )
                }, {
                    it.printStackTrace()
                })
        )

        compositeDisposable.add(
            RxBus2.listenWithoutDelay(StartCertificationExamEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .subscribe({
                    startActivityForResult(
                        CertificationBaseActivity.certificationExamIntent(
                            this,
                            conversationId = it.conversationId,
                            chatMessageId = it.messageId,
                            certificationId = it.certificationExamId,
                            cExamStatus = it.examStatus,
                            lessonInterval = it.lessonInterval
                        ), CERTIFICATION_REQUEST_CODE
                    )
                }, {
                    it.printStackTrace()
                })
        )

        compositeDisposable.add(
            RxBus2.listenWithoutDelay(OpenUserProfile::class.java)
                .subscribeOn(Schedulers.computation())
                .subscribe({
                    it.id?.let { id ->
                        openUserProfileActivity(id, USER_PROFILE_FLOW_FROM.BEST_PERFORMER.value)
                    }
                }, {
                    it.printStackTrace()
                })
        )

        compositeDisposable.add(
            RxBus2.listenWithoutDelay(LessonItemClickEventBus::class.java)
                .subscribeOn(Schedulers.computation())
                .subscribe({
                    startActivityForResult(
                        LessonActivity.getActivityIntent(this, it.lessonId),
                        LESSON_REQUEST_CODE
                    )
                }, {
                    it.printStackTrace()
                })
        )

    }


    private fun setCurrentItemPosition(chatId: String) {
        /* val currentChatid = chatId.toLowerCase(Locale.getDefault())
         CoroutineScope(Dispatchers.IO).launch {
             try {
                 var tempView: BaseChatViewHolder
                 conversationBinding.chatRv.allViewResolvers?.let {
                     it.forEachIndexed { index, view ->
                         if (view is BaseChatViewHolder) {
                             tempView = view
                             if (currentChatid == tempView.message.chatId.toLowerCase(
                                     Locale.getDefault()
                                 )
                             ) {
                                 currentAudioPosition = index
                             }
                         }
                     }
                 }
             } catch (ex: Exception) {
                 FirebaseCrashlytics.getInstance().recordException(ex)
                 ex.printStackTrace()
             }
         }*/
    }

    private fun logAssessmentEvent(assessmentId: Int) {
        AppAnalytics.create(AnalyticsEvent.QUIZ_TEST_OPENED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.COURSE_NAME.NAME, inboxEntity.course_name)
            .addParam(AnalyticsEvent.ASSESSMENT_ID.NAME, assessmentId.toString())
            .push()
    }

    private fun logUnlockCardEvent() {
        AppAnalytics.create(AnalyticsEvent.UNLOCK_CARD_CLICKED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.COURSE_NAME.NAME, inboxEntity.course_name)
            .push()
    }

    private fun analyticsAudioPlayed(
        audioType: AudioType?
    ) {
        AppAnalytics.create(AnalyticsEvent.AUDIO_PLAYED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.COURSE_NAME.NAME, inboxEntity.course_name)
            .addParam(AnalyticsEvent.AUDIO_ID.NAME, audioType?.id)
            .addParam(AnalyticsEvent.AUDIO_DURATION.NAME, audioType?.duration.toString())
            .addParam(AnalyticsEvent.AUDIO_DOWNLOAD_STATUS.NAME, "Downloaded")
            .addParam(AnalyticsEvent.AUDIO_LOCAL_PATH.NAME, audioType?.downloadedLocalPath)
            .addParam(
                AnalyticsEvent.FLOW_FROM_PARAM.NAME,
                this@ConversationActivity.javaClass.simpleName
            ).push()
    }


    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        try {
            if (requestCode == IMAGE_SELECT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
                data?.let { intent ->
                    when {
                        intent.hasExtra(JoshCameraActivity.IMAGE_RESULTS) -> {
                            intent.getStringArrayListExtra(JoshCameraActivity.IMAGE_RESULTS)
                                ?.getOrNull(0)?.let {
                                    if (it.isNotBlank()) {
                                        addImageMessage(it)
                                    }
                                }
                        }
                        intent.hasExtra(JoshCameraActivity.VIDEO_RESULTS) -> {
                            val videoPath = intent.getStringExtra(JoshCameraActivity.VIDEO_RESULTS)
                            videoPath?.let {
                                addVideoMessage(it)
                            }
                        }
                        else -> return
                    }
                }
            } else if (requestCode == PRACTISE_SUBMIT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
                showToast(getString(R.string.answer_submitted))
                (data?.getParcelableExtra(PRACTISE_OBJECT) as ChatModel?)?.let {
                    conversationViewModel.refreshMessageObject(it.chatId)
                }
            } else if (requestCode == COURSE_PROGRESS_REQUEST_CODE && data != null) {
                if (data.hasExtra(FOCUS_ON_CHAT_ID)) {
                    scrollToPosition(data.getStringExtra(FOCUS_ON_CHAT_ID)!!)
                }
            } else if (requestCode == VIDEO_OPEN_REQUEST_CODE) {
                (data?.getParcelableExtra(VIDEO_OBJECT) as ChatModel?)?.let {
                    unlockClassViewModel.canWeAddUnlockNextClass(it.chatId)
                }
            } else if (resultCode == Activity.RESULT_OK) {
                when (requestCode) {
                    ASSESSMENT_REQUEST_CODE,
                    LESSON_REQUEST_CODE,
                    CERTIFICATION_REQUEST_CODE -> {
                        data?.getStringExtra(CHAT_ROOM_ID)?.let {
                            conversationViewModel.refreshMessageObject(it)
                        }
                    }
                    COURSE_PROGRESS_NEW_REQUEST_CODE -> {
                        data?.getIntExtra(COURSE_ID, -1)?.let {
                            conversationViewModel.refreshLesson(it)
                        }
                    }
                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun uploadImageByCameraOrGallery() {
        AppAnalytics.create(AnalyticsEvent.CAMERA_CLICKED.NAME).push()
        uploadImageByUser()
    }

    private fun uploadImageByUser() {
        PermissionUtils.cameraRecordStorageReadAndWritePermission(this,
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            val options = Options.init()
                                .setRequestCode(IMAGE_SELECT_REQUEST_CODE)
                                .setCount(1)
                                .setFrontfacing(false)
                                .setPath(AppDirectory.getTempPath())
                                .setImageQuality(ImageQuality.HIGH)
                                .setScreenOrientation(Options.SCREEN_ORIENTATION_PORTRAIT)

                            JoshCameraActivity.startJoshCameraxActivity(
                                this@ConversationActivity,
                                options
                            )
                            return

                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            PermissionUtils.cameraStoragePermissionPermanentlyDeniedDialog(
                                activityRef.get()!!
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
            })

    }

    override fun onResume() {
        super.onResume()
        subscribeRXBus()
        if (inboxEntity.isGroupActive) {
            utilConversationViewModel.getCometChatUnreadMessageCount(inboxEntity.conversation_id)
        }
        if (inboxEntity.isCapsuleCourse) {
            utilConversationViewModel.getProfileData(Mentor.getInstance().getId())
        }
    }

    override fun onPause() {
        super.onPause()
        audioPlayerManager?.onPause()
        compositeDisposable.clear()
    }


    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
        readMessageTimerTask?.cancel()
        uiHandler.removeCallbacksAndMessages(null)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        AppObjectController.uiHandler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        AppObjectController.currentPlayingAudioObject = null
        audioPlayerManager?.release()
    }

    override fun onBackPressed() {
        audioPlayerManager?.onPause()
        if (conversationBinding.overlayLayout.visibility == VISIBLE) {
            conversationBinding.overlayLayout.visibility = GONE
        } else {
            val resultIntent = Intent()
            setResult(Activity.RESULT_OK, resultIntent)
            this@ConversationActivity.finishAndRemoveTask()
        }
    }

    private fun openCourseProgressListingScreen() {
        val isLessonTypeChat = conversationAdapter.isLessonType()
        if (isLessonTypeChat) {
            startActivityForResult(
                CourseProgressActivityNew.getCourseProgressActivityNew(
                    this,
                    inboxEntity.courseId.toInt()
                ), COURSE_PROGRESS_NEW_REQUEST_CODE
            )
            hideCourseProgressTooltip()

        } else {
            AppAnalytics.create(AnalyticsEvent.COURSE_PROGRESS_OVERVIEW.NAME).push()
            CourseProgressActivity.startCourseProgressActivity(
                this,
                COURSE_PROGRESS_REQUEST_CODE,
                inboxEntity
            )
        }
    }

    fun visibleItem() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val firstPosition = linearLayoutManager.findFirstVisibleItemPosition()
                val lastPosition = linearLayoutManager.findLastVisibleItemPosition()

                val globalVisibleRect = Rect()
                conversationBinding.chatRv.getGlobalVisibleRect(globalVisibleRect)
                var rowRect: Rect

                for (x in firstPosition..lastPosition step 1) {
                    rowRect = Rect()
                    linearLayoutManager.findViewByPosition(x)!!.getGlobalVisibleRect(rowRect)

                    var percentFirst: Int?
                    percentFirst = if (rowRect.bottom >= globalVisibleRect.bottom) {
                        val visibleHeightFirst = globalVisibleRect.bottom - rowRect.top
                        (visibleHeightFirst * 100) / linearLayoutManager.findViewByPosition(
                            x
                        )!!.height
                    } else {
                        val visibleHeightFirst = rowRect.bottom - globalVisibleRect.top
                        (visibleHeightFirst * 100) / linearLayoutManager.findViewByPosition(
                            x
                        )!!.height
                    }
                    if (percentFirst > VISIBLE_ITEM_PERCENT) {
                        val chatModel = conversationAdapter.getItemAtPosition(lastPosition)
                        chatModel.status = MESSAGE_STATUS.SEEN_BY_USER
                        readChatList.add(chatModel)
                    }
                }
            } catch (ex: Exception) {
//  ex.printStackTrace()
            }
        }
    }


    private fun readMessageDatabaseUpdate() {
        readMessageTimerTask = Timer("VisibleMessage", false).scheduleAtFixedRate(5000, 1500) {
            utilConversationViewModel.updateInDatabaseReadMessage(readChatList)
        }
    }

    fun isAudioPlaying(): Boolean {
        return audioPlayerManager?.isPlaying()!!
    }

    private fun setPlayProgress(progress: Int) {
        AppObjectController.currentPlayingAudioObject?.playProgress = progress
        if (currentAudioPosition != -1) {
            conversationAdapter.notifyItemChanged(currentAudioPosition)
        }
    }


    private fun refreshView(chatId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val chatObj = AppObjectController.appDatabase.chatDao()
                    .getUpdatedChatObjectViaId(chatId)
                refreshViewAtPos(chatObj)
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }


    private fun refreshViewAtPos(chatObj: ChatModel) {
        CoroutineScope(Dispatchers.Main).launch {
            delay(500)
            conversationAdapter.updateItem(chatObj)
        }
    }

    override fun onCourseProgressTooltipDismiss() {
        hideCourseProgressTooltip()
    }

    override fun onProgressUpdate(progress: Long) {
        setPlayProgress(progress.toInt())
    }

    override fun onPlayerPause() {
        if (currentAudioPosition != -1) {
            conversationBinding.chatRv.adapter?.notifyItemChanged(
                currentAudioPosition
            )
        }
    }

    override fun onPlayerResume() {
    }

    override fun onCurrentTimeUpdated(lastPosition: Long) {
    }

    override fun onTrackChange(tag: String?) {
    }

    override fun onPositionDiscontinuity(lastPos: Long, reason: Int) {

    }

    override fun onPositionDiscontinuity(reason: Int) {

    }

    override fun onPlayerReleased() {
    }

    override fun onPlayerEmptyTrack() {
    }

    override fun complete() {
        audioPlayerManager?.seekTo(0)
        audioPlayerManager?.onPause()
        setPlayProgress(0)
    }

    override fun onSuccessDismiss() {
    }

    override fun onDismiss() {
    }


    override fun onDurationUpdate(duration: Long?) {

    }


    private fun scrollToEnd() {
        CoroutineScope(Dispatchers.Main).launch {
            linearLayoutManager.scrollToPosition(conversationAdapter.itemCount - 1)
            conversationBinding.scrollToEndButton.visibility = GONE
        }
    }

    private fun scrollToPosition(chatId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val index = conversationAdapter.getMessagePositionById(chatId)
            linearLayoutManager.scrollToPositionWithOffset(index, 40)
        }
    }

    fun addAttachmentUIUpdate() {
        AttachmentUtil.revealAttachments(revealAttachmentView, conversationBinding)
        this.revealAttachmentView = !revealAttachmentView
        AppAnalytics.create(AnalyticsEvent.ATTACHMENT_CLICKED.NAME).push()
    }

    private fun sendTextMessage() {
        if (conversationBinding.chatEdit.text.isNullOrEmpty()) {
            return
        }
        if (conversationBinding.chatEdit.text.toString().length > MESSAGE_CHAT_SIZE_LIMIT) {
            showToast(getString(R.string.message_size_limit))
            return
        }
        val message = getTextMessage(conversationBinding.chatEdit.text.toString())
        conversationAdapter.addMessage(message)
        conversationViewModel.sendTextMessage(
            TChatMessage(conversationBinding.chatEdit.text.toString()),
            chatModel = message
        )
        conversationBinding.chatEdit.setText(EMPTY)
        scrollToEnd()
        AppAnalytics.create(AnalyticsEvent.CHAT_ENTERED.NAME)
            .addUserDetails()
            .addBasicParam()
            .addParam(
                AnalyticsEvent.CHAT_TEXT.NAME,
                conversationBinding.chatEdit.text.toString()
            )
            .addParam(
                AnalyticsEvent.CHAT_LENGTH.NAME,
                conversationBinding.chatEdit.text.toString().length
            )
    }

    private fun addAudioFromBottomBar(audioFilePath: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val recordUpdatedPath = AppDirectory.getAudioSentFile(audioFilePath).absolutePath
            AppDirectory.copy(audioFilePath, recordUpdatedPath)
            addAudioAttachment(recordUpdatedPath)
        }
    }

    private fun addRecordedAudioMessage(mediaPath: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val recordUpdatedPath = AppDirectory.getAudioSentFile(null).absolutePath
            AppDirectory.copy(mediaPath, recordUpdatedPath)
            addAudioAttachment(recordUpdatedPath)
        }
    }

    private fun addAudioAttachment(recordUpdatedPath: String) {
        val tAudioMessage = TAudioMessage(recordUpdatedPath, recordUpdatedPath)
        val message = getAudioMessage(tAudioMessage)
        uiHandler.post {
            conversationAdapter.addMessage(message)
        }
        scrollToEnd()
        conversationViewModel.sendMediaMessage(recordUpdatedPath, tAudioMessage, message)
    }


    private fun addImageMessage(imagePath: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val imageUpdatedPath = AppDirectory.getImageSentFilePath()
            AppDirectory.copy(imagePath, imageUpdatedPath)
            val tImageMessage = TImageMessage(imageUpdatedPath, imageUpdatedPath)
            val message = getImageMessage(tImageMessage)
            uiHandler.post {
                conversationAdapter.addMessage(message)
            }
            scrollToEnd()
            conversationViewModel.sendMediaMessage(imageUpdatedPath, tImageMessage, message)
        }

    }

    private fun addVideoMessage(videoPath: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val videoSentFile = AppDirectory.videoSentFile()
            AppDirectory.copy(videoPath, videoSentFile.absolutePath)
            val tVideoMessage =
                TVideoMessage(videoSentFile.absolutePath, videoSentFile.absolutePath)
            val message = getVideoMessage(tVideoMessage)
            uiHandler.post {
                conversationAdapter.addMessage(message)
            }
            scrollToEnd()
            conversationViewModel.sendMediaMessage(
                videoSentFile.absolutePath,
                tVideoMessage,
                message
            )
        }
    }
}
