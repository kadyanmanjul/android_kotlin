package com.joshtalks.joshskills.ui.chat

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.ActivityConversationBinding
import com.joshtalks.joshskills.emoji.PageTransformer
import com.joshtalks.recordview.CustomImageButton.FIRST_STATE
import com.joshtalks.recordview.CustomImageButton.SECOND_STATE
import com.joshtalks.recordview.OnRecordListener
import com.vanniktech.emoji.EmojiPopup
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.crashlytics.android.Crashlytics
import com.google.android.material.appbar.MaterialToolbar
import com.greentoad.turtlebody.mediapicker.MediaPicker
import com.greentoad.turtlebody.mediapicker.core.MediaPickerConfig
import com.joshtalks.appcamera.pix.JoshCameraActivity
import com.joshtalks.appcamera.pix.Options
import com.joshtalks.appcamera.utility.ImageQuality
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.audioplayer.JcPlayerManager
import com.joshtalks.joshskills.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.messaging.MessageBuilderFactory
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.DOWNLOAD_STATUS
import com.joshtalks.joshskills.repository.local.eventbus.*
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.server.chat_message.TAudioMessage
import com.joshtalks.joshskills.repository.server.chat_message.TChatMessage
import com.joshtalks.joshskills.repository.server.chat_message.TImageMessage
import com.joshtalks.joshskills.ui.extra.ImageShowFragment
import com.joshtalks.joshskills.ui.pdfviewer.PdfViewerActivity
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity
import com.joshtalks.joshskills.ui.view_holders.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import com.joshtalks.joshskills.repository.server.chat_message.TVideoMessage
import com.joshtalks.joshskills.repository.service.EngagementNetworkHelper
import de.hdodenhof.circleimageview.CircleImageView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.*

const val CHAT_ROOM_OBJECT = "chat_room"
const val IMAGE_SELECT_REQUEST_CODE = 1077

class ConversationActivity() : BaseActivity() {

    companion object {
        fun startConversionActivity(context: Context, inboxEntity: InboxEntity) {
            val intent = Intent(context, ConversationActivity::class.java)
            intent.putExtra(CHAT_ROOM_OBJECT, inboxEntity)
            context.startActivity(intent)
        }
    }

    private lateinit var conversationViewModel: ConversationViewModel
    private lateinit var conversationBinding: ActivityConversationBinding
    private lateinit var inboxEntity: InboxEntity
    private lateinit var emojiPopup: EmojiPopup
    private var editTextStatus: Boolean = false
    private val cMessageType: BASE_MESSAGE_TYPE = BASE_MESSAGE_TYPE.TX
    private val compositeDisposable = CompositeDisposable()
    private var revealAttachmentView: Boolean = false
    private lateinit var activityRef: WeakReference<FragmentActivity>
    private lateinit var linearLayoutManager: LinearLayoutManager
    private var conversationList = linkedSetOf<ChatModel>()
    private var removingConversationList = linkedSetOf<ChatModel>()
    private var replySuggestion: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.hasExtra(CHAT_ROOM_OBJECT)) {
            inboxEntity = intent.getSerializableExtra(CHAT_ROOM_OBJECT) as InboxEntity
        } else {
            this@ConversationActivity.finish()
        }
        try {
            conversationViewModel =
                ViewModelProviders.of(this).get(ConversationViewModel::class.java)
            conversationViewModel.inboxEntity = inboxEntity
        } catch (ex: UninitializedPropertyAccessException) {
            startActivity(getInboxActivityIntent())
            this.finishAndRemoveTask()
        }
        conversationBinding = DataBindingUtil.setContentView(this, R.layout.activity_conversation)
        conversationBinding.viewmodel = conversationViewModel
        conversationBinding.handler = this
        activityRef = WeakReference(this)
        initChat()

    }

    private fun initChat() {
        setToolbar()
        initRV()
        setUpEmojiPopup()
        addListenerObservable()
        initView()
        conversationViewModel.getAllUserMessage()
        AppObjectController.uiHandler.postDelayed({
            checkAudioPermission(null)
        }, 1000)
        AppAnalytics.create(AnalyticsEvent.CHAT_SCREEN.NAME).push()
        processIntent(intent)
        // initSuggestionView()
    }


    override fun onNewIntent(mIntent: Intent?) {
        super.onNewIntent(mIntent)
        processIntent(mIntent)
        mIntent?.hasExtra(CHAT_ROOM_OBJECT)?.let {
            if (it) {
                val temp = mIntent.getSerializableExtra(CHAT_ROOM_OBJECT) as InboxEntity?
                temp?.let { inboxObj ->
                    if (inboxEntity.conversation_id != inboxObj.conversation_id) {
                        inboxEntity = inboxObj
                        conversationViewModel.inboxEntity = inboxEntity
                        initChat()
                    }
                }
            }
        }
    }

    private fun setToolbar() {
        try {
            findViewById<View>(R.id.iv_back).visibility = VISIBLE
            findViewById<CircleImageView>(R.id.image_view_logo).setImageResource(R.drawable.ic_josh_course)
            findViewById<View>(R.id.image_view_logo).visibility = VISIBLE
            findViewById<AppCompatImageView>(R.id.iv_back).setOnClickListener {
                finish()
            }

            if (Utils.isHelplineOnline()) {
                findViewById<MaterialToolbar>(R.id.toolbar).inflateMenu(R.menu.main_menu)
                findViewById<MaterialToolbar>(R.id.toolbar).setOnMenuItemClickListener {
                    if (it?.itemId == R.id.menu_call_helpline) {
                        callHelpLine()
                    }
                    return@setOnMenuItemClickListener true
                }
            }
            findViewById<AppCompatTextView>(R.id.text_message_title).text = inboxEntity.course_name

        } catch (ex: Exception) {

        }

    }

    private fun initRV() {
        linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.stackFromEnd = true
        linearLayoutManager.isSmoothScrollbarEnabled = true
        conversationBinding.chatRv.builder
            .setHasFixedSize(false)
            .setLayoutManager(linearLayoutManager)
        conversationBinding.chatRv.itemAnimator = null
        conversationBinding.chatRv.addItemDecoration(
            LayoutMarginDecoration(
                com.vanniktech.emoji.Utils.dpToPx(
                    this,
                    4f
                )
            )
        )
        conversationBinding.chatRv.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!recyclerView.canScrollVertically(1) && newState == RecyclerView.SCROLL_STATE_IDLE) {
                    conversationBinding.scrollToEndButton.visibility = GONE
                } else if (recyclerView.canScrollVertically(-1) && newState == RecyclerView.SCROLL_STATE_IDLE) {
                    conversationBinding.scrollToEndButton.visibility = VISIBLE
                }

            }
        })
    }

    private fun setUpEmojiPopup() {
        emojiPopup = EmojiPopup.Builder.fromRootView(conversationBinding.root)
            .setOnEmojiBackspaceClickListener {
            }
            .setOnEmojiClickListener { _, _ ->
                AppAnalytics.create(AnalyticsEvent.EMOJI_CLICKED.NAME).push()
            }
            .setOnEmojiPopupShownListener { conversationBinding.ivEmoji.setImageResource(R.drawable.ic_keyboard) }
            .setOnSoftKeyboardOpenListener { ignore -> }
            .setOnEmojiPopupDismissListener { conversationBinding.ivEmoji.setImageResource(R.drawable.happy_face) }
            .setOnSoftKeyboardCloseListener { }
            .setKeyboardAnimationStyle(R.style.emoji_fade_animation_style)
            .setPageTransformer(PageTransformer())
            .setBackgroundColor(
                ContextCompat.getColor(
                    applicationContext,
                    R.color.emoji_bg_color
                )
            )
            .setIconColor(
                ContextCompat.getColor(
                    applicationContext,
                    R.color.emoji_icon_color
                )
            )
            .build(conversationBinding.chatEdit)
    }

    private fun initView() {
        conversationBinding.recordButton.setRecordView(conversationBinding.recordView)
        conversationBinding.recordView.cancelBounds = 2f
        conversationBinding.recordView.setSmallMicColor(Color.parseColor("#c2185b"))
        conversationBinding.recordView.setLessThanSecondAllowed(false)
        conversationBinding.recordView.setSlideToCancelText("Slide To Cancel")
        conversationBinding.recordView.setCustomSounds(
            R.raw.record_start,
            R.raw.record_finished,
            0
        )
        conversationBinding.recordButton.isListenForRecord = false
        conversationBinding.recordView.setOnRecordListener(object : OnRecordListener {
            override fun onStart() {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                AppAnalytics.create(AnalyticsEvent.AUDIO_BUTTON_CLICKED.NAME).push()
                conversationBinding.recordView.visibility = VISIBLE
                conversationViewModel.startRecord()
            }

            override fun onCancel() {
                conversationViewModel.stopRecording()
            }

            override fun onFinish(recordTime: Long) {
                AppAnalytics.create(AnalyticsEvent.AUDIO_SENT.NAME).push()
                conversationBinding.recordView.visibility = GONE
                conversationViewModel.stopRecording()
                conversationViewModel.recordFile.let {
                    addUploadAudioMedia(it.absolutePath)
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            }

            override fun onLessThanSecond() {
                conversationBinding.recordView.visibility = GONE
                conversationViewModel.stopRecording()
                AppAnalytics.create(AnalyticsEvent.AUDIO_CANCELLED.NAME).push()
            }
        })



        conversationBinding.recordView.setOnBasketAnimationEndListener {
            conversationBinding.recordView.visibility = GONE
            conversationViewModel.stopRecording()
            AppAnalytics.create(AnalyticsEvent.AUDIO_CANCELLED.NAME).push()
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        conversationBinding.chatEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty()) {
                    conversationBinding.recordButton.goToState(FIRST_STATE)
                    editTextStatus = false
                    conversationBinding.recordButton.isListenForRecord = true
                    conversationBinding.quickToggle.show()


                } else {
                    if (editTextStatus.not()) {
                        conversationBinding.recordButton.goToState(SECOND_STATE)
                        editTextStatus = true
                        conversationBinding.recordButton.isListenForRecord = false
                        conversationBinding.quickToggle.hide()

                    }
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

        conversationBinding.chatEdit.setOnTouchListener { _, event ->
            if (MotionEvent.ACTION_UP == event.action) {
                if (emojiPopup.isShowing) {
                    emojiPopup.toggle()
                }
            }
            false
        }

        conversationBinding.recordButton.setOnRecordClickListener {
            if (conversationBinding.chatEdit.text.toString().isEmpty()) {
                checkAudioPermission(Runnable {
                    val downTime = SystemClock.uptimeMillis()
                    val eventTime = SystemClock.uptimeMillis() + 100
                    val motionEvent = MotionEvent.obtain(
                        downTime,
                        eventTime,
                        MotionEvent.ACTION_DOWN,
                        0f,
                        0f,
                        0
                    )
                    conversationBinding.recordView.dispatchTouchEvent(motionEvent)
                }, settingFlag = true)
                return@setOnRecordClickListener
            }

            if (conversationBinding.chatEdit.text!!.length > MESSAGE_CHAT_SIZE_LIMIT) {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.message_size_limit),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnRecordClickListener
            }
            /*if (replySuggestion.isNullOrEmpty()) {
                val toast = Toast.makeText(
                    applicationContext,
                    "Chacha kuch to select karo",
                    Toast.LENGTH_LONG
                )
                toast.setGravity(Gravity.TOP, 0, 80)
                toast.show()

                return@setOnRecordClickListener
            }*/
            if (cMessageType == BASE_MESSAGE_TYPE.TX) {
                val tChatMessage =
                    TChatMessage(conversationBinding.chatEdit.text.toString())
                val cell = MessageBuilderFactory.getMessage(
                    activityRef,
                    BASE_MESSAGE_TYPE.TX,
                    tChatMessage
                )
                conversationBinding.chatRv.addView(cell)
                scrollToEnd()
                conversationViewModel.sendTextMessage(
                    TChatMessage(conversationBinding.chatEdit.text.toString()),
                    chatModel = cell.message
                )
            }
            conversationBinding.chatEdit.setText(EMPTY)

            /*replySuggestion = null
             findViewById<View>(R.id.replay_root).visibility = GONE
             conversationBinding.userReplaySuggestionRv.visibility = VISIBLE
            conversationBinding.inputLL.setBackgroundResource(R.drawable.add_suggestion_bg)
             */

        }


        findViewById<View>(R.id.ll_audio).setOnClickListener {
            AppAnalytics.create(AnalyticsEvent.AUDIO_SELECTED.NAME).push()
            uploadAttachment()
            val pickerConfig = MediaPickerConfig()
                .setUriPermanentAccess(false)
                .setAllowMultiSelection(false)
                .setScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            MediaPicker.with(this, MediaPicker.MediaTypes.AUDIO)
                .setConfig(pickerConfig)
                .setFileMissingListener(object :
                    MediaPicker.MediaPickerImpl.OnMediaListener {
                    override fun onMissingFileWarning() {
                    }
                })
                .onResult()
                .subscribe({
                    it?.let {
                        it[0].path?.let { path ->
                            AppAnalytics.create(AnalyticsEvent.AUDIO_SENT.NAME).push()
                            addUploadAnyAudioMedia(Utils.getPathFromUri(path))
                        }

                    }
                }, {
                    AppAnalytics.create(AnalyticsEvent.BACK_PRESSED.NAME).push()
                })

        }
        findViewById<View>(R.id.ll_camera).setOnClickListener {
            AppAnalytics.create(AnalyticsEvent.CAMERA_SELECTED.NAME).push()
            uploadAttachment()
            uploadImageByUser()

        }
        findViewById<View>(R.id.ll_gallary).setOnClickListener {
            AppAnalytics.create(AnalyticsEvent.CAMERA_SELECTED.NAME).push()
            uploadAttachment()

            val pickerConfig = MediaPickerConfig()
                .setUriPermanentAccess(true)
                .setAllowMultiSelection(false)
                //.setShowConfirmationDialog(true)
                .setScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

            MediaPicker.with(this, MediaPicker.MediaTypes.IMAGE)
                .setConfig(pickerConfig)
                .setFileMissingListener(object :
                    MediaPicker.MediaPickerImpl.OnMediaListener {
                    override fun onMissingFileWarning() {
                    }
                })
                .onResult()
                .subscribe({
                    it?.let {
                        it[0].path?.let { path ->
                            if (IMAGE_REGEX.matches(path)) {
                                addUserImageInView(path)
                            } else {
                                addUserVideoInView(path)
                            }
                        }
                    }
                }, {
                    AppAnalytics.create(AnalyticsEvent.BACK_PRESSED.NAME).push()

                })
        }
        conversationBinding.scrollToEndButton.setOnClickListener {
            scrollToEnd()
        }

        findViewById<View>(R.id.iv_cancel_delete).setOnClickListener {
            removingConversationList.forEach {
                refreshViewAtPos(it)
            }
            removingConversationList.clear()
            findViewById<MaterialToolbar>(R.id.toolbar_delete_chat).visibility = GONE

        }
        findViewById<View>(R.id.iv_delete_chat).setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val listIds: MutableList<String> = mutableListOf()
                removingConversationList.forEach {
                    removeViewAtPos(it)
                    listIds.add(it.chatId)
                }
                conversationViewModel.deleteMessages(listIds)
                removingConversationList.clear()
            }
            findViewById<MaterialToolbar>(R.id.toolbar_delete_chat).visibility = GONE
        }
        /* findViewById<View>(R.id.replay_root).setOnClickListener {
             findViewById<View>(R.id.replay_root).visibility = GONE
             conversationBinding.userReplaySuggestionRv.visibility = VISIBLE
             replySuggestion = null
             conversationBinding.inputLL.setBackgroundResource(R.drawable.add_suggestion_bg)

         }*/

        /* KeyboardVisibilityEvent.setEventListener(
             activityRef.get()
         ) { isOpen ->
             if (isOpen) {
                 if (replySuggestion.isNullOrEmpty()) {
                     findViewById<View>(R.id.replay_root).visibility = GONE
                     conversationBinding.userReplaySuggestionRv.visibility = VISIBLE
                 } else {
                     conversationBinding.userReplaySuggestionRv.visibility = GONE
                     findViewById<View>(R.id.replay_root).visibility = VISIBLE
                 }
                 conversationBinding.inputLL.setBackgroundResource(R.drawable.add_suggestion_bg)
             } else {
                 conversationBinding.userReplaySuggestionRv.visibility = GONE
                 findViewById<View>(R.id.replay_root).visibility = GONE
                 conversationBinding.inputLL.setBackgroundResource(R.drawable.rect_round)
             }
         }*/

    }

    private fun initSuggestionView() {
/*
        conversationBinding.userReplaySuggestionRv.builder
            .setHasFixedSize(true)
            .setLayoutManager(LinearLayoutManager(this, RecyclerView.HORIZONTAL, false))
        conversationBinding.userReplaySuggestionRv.addItemDecoration(
            LayoutMarginDecoration(
                com.vanniktech.emoji.Utils.dpToPx(
                    this,
                    4f
                )
            )
        )
        val sug = arrayOf("Answer", "Koi preshani", "Kuch aur")
        for (i in sug) {
            conversationBinding.userReplaySuggestionRv.addView(SuggestionViewHolder(i))
        }
        conversationBinding.userReplaySuggestionRv.refresh()*/
    }

    private fun removeViewAtPos(chatObj: ChatModel) {
        try {
            var tempView: BaseChatViewHolder
            conversationBinding.chatRv.allViewResolvers?.let {
                it.listIterator().forEach {
                    tempView = it as BaseChatViewHolder
                    if (chatObj.chatId.toLowerCase(Locale.getDefault()) == tempView.message.chatId.toLowerCase(
                            Locale.getDefault()
                        )
                    ) {
                        AppObjectController.uiHandler.postDelayed({
                            try {
                                conversationBinding.chatRv.removeView(tempView)
                            } catch (e: IndexOutOfBoundsException) {
                                e.printStackTrace()
                            }
                        }, 100)
                    }
                }

            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

    }

    private fun addListenerObservable() {
        conversationViewModel.chatObservableLiveData.observe(this, Observer {
            conversationList.addAll(it)
            conversationList.iterator().forEach { chatModel ->
                /*if (chatModel.isSeen.not() && unreadMessagePosition == -1) {
                    val count = conversationBinding.chatRv.adapter?.itemCount ?: 0
                    unreadMessagePosition = count - it.indexOf(chatModel)
                }*/

                getView(chatModel)?.let { cell ->
                    conversationBinding.chatRv.addView(cell)
                }
            }

            /*if (unreadMessagePosition > -1) {
                val totalUnreadMessage = it.size - unreadMessagePosition
                conversationBinding.chatRv.addView(
                    unreadMessagePosition,
                    UnreadMessageViewHolder(totalUnreadMessage)
                )
            }*/
            conversationBinding.chatRv.refresh()
            scrollToEnd()
            /* AppObjectController.uiHandler.postDelayed({
                 linearLayoutManager.scrollToPosition(unreadMessagePosition)
             }, 250)
             if (unreadMessagePosition == -1) {
                 scrollToEnd()
             } else {
                 if (unreadScrollDone.not()) {
                     unreadScrollDone = true
                     AppObjectController.uiHandler.postDelayed({
                         linearLayoutManager.scrollToPosition(unreadMessagePosition)
                     }, 250)
                 }}*/

        })

        conversationViewModel.refreshViewLiveData.observe(this, Observer { chatModel ->
            val view: BaseChatViewHolder =
                conversationBinding.chatRv.getViewResolverAtPosition(conversationBinding.chatRv.viewResolverCount - 1) as BaseChatViewHolder
            view.message = chatModel
            AppObjectController.uiHandler.postDelayed({
                conversationBinding.chatRv.refreshView(view)
            }, 250)
        })
    }

    private fun subscribeRXBus() {
        compositeDisposable.add(RxBus2.listen(PlayVideoEvent::class.java)
            .subscribe({
                pauseAudioPlayer()
                VideoPlayerActivity.startConversionActivity(
                    this,
                    it.chatModel, inboxEntity.course_name
                )
            }, {
                it.printStackTrace()
            })
        )
        compositeDisposable.add(RxBus2.listen(ImageShowEvent::class.java).subscribe {
            it.imageUrl?.let { imageUrl ->
                ImageShowFragment.newInstance(imageUrl, inboxEntity.course_name, it.imageId)
                    .show(supportFragmentManager, "ImageShow")
            }
        })
        compositeDisposable.add(RxBus2.listen(PdfOpenEventBus::class.java).subscribe {
            PdfViewerActivity.startPdfActivity(this, it.pdfObject, inboxEntity.course_name)
        })
        compositeDisposable.add(RxBus2.listen(RemoveViewEventBus::class.java).subscribe {

        })

        compositeDisposable.add(RxBus2.listen(DownloadMediaEventBus::class.java).subscribe {

            Dexter.withActivity(this)
                .withPermissions(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE

                )
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        token?.continuePermissionRequest()
                    }

                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {

                        report?.areAllPermissionsGranted()?.let { flag ->
                            if (flag) {
                                val pos =
                                    conversationBinding.chatRv.getViewResolverPosition(it.viewHolder)
                                val view: BaseChatViewHolder =
                                    conversationBinding.chatRv.getViewResolverAtPosition(pos) as BaseChatViewHolder
                                val chatModel = it.chatModel
                                chatModel.downloadStatus = DOWNLOAD_STATUS.DOWNLOADING
                                view.message = it.chatModel
                                AppObjectController.uiHandler.postDelayed({
                                    conversationBinding.chatRv.refreshView(view)
                                }, 250)
                            }
                        }

                    }

                }).check()
        })

        compositeDisposable.add(RxBus2.listen(DownloadCompletedEventBus::class.java)
            .subscribeOn(Schedulers.computation())
            .subscribe {
                CoroutineScope(Dispatchers.IO).launch {
                    val obj = AppObjectController.appDatabase.chatDao()
                        .getUpdatedChatObject(it.chatModel)
                    refreshViewAtPos(obj)
                }

            })
        compositeDisposable.add(RxBus2.listen(MediaEngageEventBus::class.java).subscribe {
            CoroutineScope(Dispatchers.IO).launch {
                if (it.type.equals("AUDIO", ignoreCase = true)) {
                    EngagementNetworkHelper.engageAudioApi(it)
                }
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

        compositeDisposable.add(RxBus2.listen(DeleteMessageEventBus::class.java)
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {

                if (removingConversationList.contains(it.chatModel)) {
                    removingConversationList.remove(it.chatModel)
                } else {
                    if (it.chatModel.isSelected) {
                        removingConversationList.add(it.chatModel)
                    }
                }
                if (removingConversationList.size > 0) {
                    findViewById<MaterialToolbar>(R.id.toolbar_delete_chat).visibility = VISIBLE
                } else {
                    findViewById<MaterialToolbar>(R.id.toolbar_delete_chat).visibility = GONE
                }
                findViewById<AppCompatTextView>(R.id.message_delete_count).text =
                    removingConversationList.size.toString()
            })
        /*compositeDisposable.add(RxBus2.listen(TagMessageEventBus::class.java)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                conversationBinding.inputLL.setBackgroundResource(R.drawable.add_suggestion_bg)
                findViewById<View>(R.id.replay_root).visibility = VISIBLE
                conversationBinding.userReplaySuggestionRv.visibility = GONE
                findViewById<AppCompatTextView>(R.id.tv_reply_text).text = it.tag
                replySuggestion = it.tag

            })*/


    }

    private fun refreshViewAtPos(chatObj: ChatModel) {
        CoroutineScope(Dispatchers.IO).launch {
            try {

                var tempView: BaseChatViewHolder
                conversationBinding.chatRv.allViewResolvers?.let {

                    it.forEachIndexed { index, view ->
                        tempView = view as BaseChatViewHolder
                        if (chatObj.chatId.toLowerCase(Locale.getDefault()) == tempView.message.chatId.toLowerCase(
                                Locale.getDefault()
                            )
                        ) {
                            tempView.message = chatObj
                            tempView.message.isSelected = false
                            AppObjectController.uiHandler.postDelayed({
                                conversationBinding.chatRv.refreshView(index)
                            }, 250)
                        }
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

    }


    fun emojiToggle() {
        emojiPopup.toggle()

    }


    private fun addUploadAnyAudioMedia(audioFilePath: String) {
        val recordUpdatedPath = AppDirectory.getFilePath(audioFilePath)
        AppDirectory.copy(audioFilePath, recordUpdatedPath)
        val tAudioMessage = TAudioMessage(recordUpdatedPath, recordUpdatedPath)
        val cell =
            MessageBuilderFactory.getMessage(activityRef, BASE_MESSAGE_TYPE.AU, tAudioMessage)
        conversationBinding.chatRv.addView(cell)
        scrollToEnd()
        conversationViewModel.uploadMedia(
            recordUpdatedPath,
            tAudioMessage,
            cell.message
        )
    }

    private fun addUploadAudioMedia(mediaPath: String) {
        try {
            val recordUpdatedPath = AppDirectory.getRecordingSentFilePath()
            AppDirectory.copy(mediaPath, recordUpdatedPath)
            val tAudioMessage = TAudioMessage(recordUpdatedPath, recordUpdatedPath)
            val cell =
                MessageBuilderFactory.getMessage(activityRef, BASE_MESSAGE_TYPE.AU, tAudioMessage)
            conversationBinding.chatRv.addView(cell)
            scrollToEnd()
            conversationViewModel.uploadMedia(
                recordUpdatedPath,
                tAudioMessage,
                cell.message
            )
        }catch (ex:Exception){
            Crashlytics.logException(ex)
        }
    }


    private fun getView(
        chatModel: ChatModel
    ): BaseCell? {
        return if (chatModel.type == BASE_MESSAGE_TYPE.Q) {
            getGenericView(chatModel.question?.material_type, chatModel)
        } else {
            getGenericView(chatModel.type, chatModel)
        }

    }

    private fun getGenericView(
        mszType: BASE_MESSAGE_TYPE?,
        chatModel: ChatModel
    ): BaseCell? {

        return when (mszType) {
            BASE_MESSAGE_TYPE.AU ->
                AudioPlayerViewHolder(activityRef, chatModel)

            BASE_MESSAGE_TYPE.IM ->
                ImageViewHolder(activityRef, chatModel)
            BASE_MESSAGE_TYPE.PD ->
                PdfViewHolder(activityRef, chatModel)

            BASE_MESSAGE_TYPE.VI ->
                VideoViewHolder(activityRef, chatModel)
            else -> TextViewHolder(activityRef, chatModel)
        }

    }


    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        if (requestCode == IMAGE_SELECT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.let { intent ->
                when {
                    intent.hasExtra(JoshCameraActivity.IMAGE_RESULTS) -> {
                        val returnValue =
                            intent.getStringArrayListExtra(JoshCameraActivity.IMAGE_RESULTS)
                        returnValue?.get(0)?.let { addUserImageInView(it) }
                    }
                    intent.hasExtra(JoshCameraActivity.VIDEO_RESULTS) -> {
                        val videoPath = intent.getStringExtra(JoshCameraActivity.VIDEO_RESULTS)
                        addUserVideoInView(videoPath)
                    }
                    else -> return
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun uploadImageByCameraOrGallery() {
        AppAnalytics.create(AnalyticsEvent.CAMERA_CLICKED.NAME).push()
        uploadImageByUser()
    }


    private fun uploadImageByUser() {
        val options = Options.init()
            .setRequestCode(IMAGE_SELECT_REQUEST_CODE)
            .setCount(1)
            .setFrontfacing(false)
            .setPath(AppDirectory.getTempPath())
            .setImageQuality(ImageQuality.HIGH)
            .setScreenOrientation(Options.SCREEN_ORIENTATION_PORTRAIT)

        JoshCameraActivity.start(this, options)
    }


    private fun addUserImageInView(imagePath: String) {
        val imageUpdatedPath = AppDirectory.getImageSentFilePath()
        AppDirectory.copy(imagePath, imageUpdatedPath)
        val tImageMessage = TImageMessage(imageUpdatedPath, imageUpdatedPath)
        val cell = MessageBuilderFactory.getMessage(
            activityRef,
            BASE_MESSAGE_TYPE.IM,
            tImageMessage
        )
        conversationBinding.chatRv.addView(
            cell
        )
        scrollToEnd()

        conversationViewModel.uploadMedia(
            imageUpdatedPath, tImageMessage, cell.message
        )

    }

    private fun addUserVideoInView(videoPath: String) {
        val videoSentFile = AppDirectory.videoSentFile()
        AppDirectory.copy(videoPath, videoSentFile.absolutePath)
        val tVideoMessage = TVideoMessage(videoSentFile.absolutePath, videoSentFile.absolutePath)

        val cell = MessageBuilderFactory.getMessage(
            activityRef,
            BASE_MESSAGE_TYPE.VI,
            tVideoMessage
        )
        conversationBinding.chatRv.addView(
            cell
        )
        scrollToEnd()

        conversationViewModel.uploadMedia(
            videoSentFile.absolutePath, tVideoMessage, cell.message
        )
    }


    private fun scrollToEnd() {
        AppObjectController.uiHandler.postDelayed({
            val count = conversationBinding.chatRv.adapter?.itemCount ?: 0
            linearLayoutManager.scrollToPosition(count - 1)
            conversationBinding.scrollToEndButton.visibility = GONE
        }, 150)

    }

    fun uploadAttachment() {
        AppAnalytics.create(AnalyticsEvent.ATTACHMENT_CLICKED.NAME).push()
        AttachmentUtil.revealAttachments(revealAttachmentView, conversationBinding)
        this.revealAttachmentView = !revealAttachmentView

    }

    private fun checkAudioPermission(
        callback: Runnable?,
        settingFlag: Boolean = false
    ) {
        Dexter.withActivity(this)
            .withPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            conversationBinding.recordButton.isListenForRecord = true
                            callback?.run()
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()

                }

            }).withErrorListener {
                if (settingFlag) {
                    openSettings()
                }

            }
            .onSameThread()
            .check()
    }


    override fun onResume() {
        super.onResume()
        activityRef = WeakReference(this)
        subscribeRXBus()
    }


    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
    }


    override fun onBackPressed() {
        AppAnalytics.create(AnalyticsEvent.BACK_PRESSED.NAME)
            .addParam("name", javaClass.simpleName)
            .push()
        this@ConversationActivity.finishAndRemoveTask()
    }

    override fun onPause() {
        super.onPause()
        pauseAudioPlayer()
    }

    private fun pauseAudioPlayer() {
        try {
            val jcPlayerManager: JcPlayerManager by lazy {
                JcPlayerManager.getInstance(applicationContext).get()!!
            }
            jcPlayerManager.pauseAudio()
        } catch (ex: Exception) {

        }
    }

}
