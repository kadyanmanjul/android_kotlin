package com.joshtalks.joshskills.ui.chat

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.ActivityConversationBinding
import com.joshtalks.joshskills.emoji.PageTransformer
import com.joshtalks.recordview.CustomImageButton.FIRST_STATE
import com.joshtalks.recordview.CustomImageButton.SECOND_STATE
import com.joshtalks.recordview.OnRecordListener
import com.vanniktech.emoji.EmojiPopup
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.greentoad.turtlebody.mediapicker.MediaPicker
import com.greentoad.turtlebody.mediapicker.core.MediaPickerConfig
import com.joshtalks.appcamera.pix.JoshCameraActivity
import com.joshtalks.appcamera.pix.Options
import com.joshtalks.appcamera.utility.ImageQuality
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.Utils
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
import com.joshtalks.joshskills.util.MediaPlayerManager
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.DexterError
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.PermissionRequestErrorListener
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

import android.provider.Settings;
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.SmoothLinearLayoutManager
import com.joshtalks.joshskills.repository.server.chat_message.TVideoMessage

const val CHAT_ROOM_OBJECT = "chat_room"
const val IMAGE_SELECT_REQUEST_CODE = 1077

class ConversationActivity : BaseActivity() {

    private lateinit var conversationBinding: ActivityConversationBinding
    private lateinit var inboxEntity: InboxEntity
    private val conversationViewModel: ConversationViewModel by lazy {
        ViewModelProviders.of(this).get(ConversationViewModel::class.java)
    }
    private lateinit var emojiPopup: EmojiPopup
    internal var editTextStatus: Boolean = false

    var cMessageType: BASE_MESSAGE_TYPE = BASE_MESSAGE_TYPE.TX
    private var compositeDisposable = CompositeDisposable()

    private var revealAttachmentView: Boolean = false
    private lateinit var activityRef: WeakReference<FragmentActivity>
    private val rvHandler = Handler(Looper.getMainLooper())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inboxEntity = intent.getSerializableExtra(CHAT_ROOM_OBJECT) as InboxEntity
        conversationViewModel.inboxEntity = inboxEntity
        conversationBinding = DataBindingUtil.setContentView(this, R.layout.activity_conversation)
        conversationBinding.viewmodel = conversationViewModel
        conversationBinding.handler = this
        conversationBinding.inputLL
        activityRef = WeakReference(this)

        setToolbar()
        addListenerObservable()
        initRV()
        setUpEmojiPopup()
        initView()
        conversationViewModel.getAllUserMessage()
        AppObjectController.uiHandler.postDelayed({
            checkAudioPermission(null)
        }, 1000)
    }

    private fun setToolbar() {
        findViewById<View>(R.id.iv_back).visibility = VISIBLE
        findViewById<View>(R.id.image_view_logo).visibility = VISIBLE

        findViewById<AppCompatTextView>(R.id.text_message_title).text = inboxEntity.course_name
        findViewById<AppCompatImageView>(R.id.iv_back).setOnClickListener {
            finish()
        }
    }

    private fun subscribeRXBus() {


        compositeDisposable.add(RxBus2.listen(PlayVideoEvent::class.java).subscribe {
            VideoPlayerActivity.startConversionActivity(
                this,
                it.chatModel
            )
        })
        compositeDisposable.add(RxBus2.listen(ImageShowEvent::class.java).subscribe {
            it.imageUrl?.let {
                ImageShowFragment.newInstance(it).show(supportFragmentManager, "ImageShow")
            }
        })
        compositeDisposable.add(RxBus2.listen(PdfOpenEventBus::class.java).subscribe {
            PdfViewerActivity.startPdfActivity(this, it.pdfObject)
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

                }).check()

        })

        compositeDisposable.add(RxBus2.listen(DownloadCompletedEventBus::class.java).subscribe {
            CoroutineScope(Dispatchers.IO).launch {

                try {
                    val obj =
                        AppObjectController.appDatabase.chatDao()
                            .getUpdatedChatObject(it.chatModel)
                    val pos = conversationBinding.chatRv.getViewResolverPosition(it.viewHolder)
                    val view: BaseChatViewHolder =
                        conversationBinding.chatRv.getViewResolverAtPosition(pos) as BaseChatViewHolder
                    view.message = obj
                    AppObjectController.uiHandler.postDelayed({
                        conversationBinding.chatRv.refreshView(view)
                    }, 250)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }

        })


    }

    @SuppressLint("ClickableViewAccessibility")
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

                conversationBinding.recordView.visibility = VISIBLE
                Handler().postDelayed({
                    conversationViewModel.startRecord()
                }, 350)

            }

            override fun onCancel() {
                conversationViewModel.stopRecording()
            }

            override fun onFinish(recordTime: Long) {
                conversationBinding.recordView.visibility = GONE
                conversationViewModel.stopRecording()
                addUploadAudioMedia(conversationViewModel.recordFile.absolutePath)

            }

            override fun onLessThanSecond() {
                conversationBinding.recordView.visibility = GONE
                conversationViewModel.stopRecording()


            }
        })


        conversationBinding.recordView.setOnBasketAnimationEndListener {
            conversationBinding.recordView.visibility = GONE
            conversationViewModel.stopRecording()

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

        conversationBinding.chatEdit.setOnTouchListener { v, event ->
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



            if (cMessageType == BASE_MESSAGE_TYPE.TX) {
                val tChatMessage =
                    TChatMessage(conversationBinding.chatEdit.text.toString())
                conversationBinding.chatRv.addView(
                    MessageBuilderFactory.getMessage(
                        activityRef,
                        BASE_MESSAGE_TYPE.TX,
                        tChatMessage
                    )
                )
            }

            conversationViewModel.sendTextMessage(TChatMessage(conversationBinding.chatEdit.text.toString()))
            conversationBinding.chatEdit.setText("")
            scrollToEnd()
        }


        findViewById<View>(R.id.ll_audio).setOnClickListener {
            uploadAttachment()
            val pickerConfig = MediaPickerConfig()
                .setUriPermanentAccess(false)
                .setAllowMultiSelection(false)
                //.setShowConfirmationDialog(true)
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
                            addUploadAudioMedia(Utils.getPathFromUri(path))
                        }

                    }
                }, {
                    it.printStackTrace()
                })

        }
        findViewById<View>(R.id.ll_camera).setOnClickListener {
            uploadAttachment()
            uploadImageByUser()

        }
        findViewById<View>(R.id.ll_gallary).setOnClickListener {
            uploadAttachment()
            val pickerConfig = MediaPickerConfig()
                .setUriPermanentAccess(false)
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
                            addUserImageInView((Utils.getPathFromUri(path)))
                        }
                    }
                }, {
                })
        }


    }

    fun emojiToggle() {
        emojiPopup.toggle()

    }


    private fun addUploadAudioMedia(mediaPath: String) {
        val tAudioMessage =
            TAudioMessage(mediaPath, mediaPath)
        conversationBinding.chatRv.addView(
            MessageBuilderFactory.getMessage(
                activityRef,
                BASE_MESSAGE_TYPE.AU,
                tAudioMessage
            )
        )
        scrollToEnd()
        conversationViewModel.uploadMedia(
            mediaPath,
            TAudioMessage(mediaPath, mediaPath)
        )
    }


    private fun initRV() {
        val linearLayoutManager = SmoothLinearLayoutManager(this);
        linearLayoutManager.stackFromEnd = true
        linearLayoutManager.isSmoothScrollbarEnabled = true

        conversationBinding.chatRv.layoutManager = linearLayoutManager
        conversationBinding.chatRv.setHasFixedSize(false)

        conversationBinding.chatRv.itemAnimator = null
        conversationBinding.chatRv.addItemDecoration(
            LayoutMarginDecoration(
                com.vanniktech.emoji.Utils.dpToPx(
                    this,
                    4f
                )
            )
        )
    }


    private fun addListenerObservable() {
        conversationViewModel.chatObservableLiveData.observe(this, Observer {
            it.forEach { chatModel ->
                getView(chatModel)?.let {
                    conversationBinding.chatRv.addView(it)
                }
            }
            conversationBinding.chatRv.findViewHolderForAdapterPosition(conversationBinding.chatRv.viewResolverCount)
            conversationBinding.chatRv.refresh()
        })

        conversationViewModel.refreshViewLiveData.observe(this, Observer { chatModel ->
            AppObjectController.uiHandler.postDelayed({
                val view: BaseChatViewHolder =
                    conversationBinding.chatRv.getViewResolverAtPosition(conversationBinding.chatRv.viewResolverCount - 1) as BaseChatViewHolder
                view.message = chatModel
                conversationBinding.chatRv.refreshView(view)
            }, 250)
        })

    }


    private fun setUpEmojiPopup() {
        emojiPopup = EmojiPopup.Builder.fromRootView(conversationBinding.root)
            .setOnEmojiBackspaceClickListener { ignore ->
                Log.d(
                    TAG,
                    "Clicked on Backspace"
                )
            }
            .setOnEmojiClickListener { ignore, ignore2 -> Log.d(TAG, "Clicked on emoji") }
            .setOnEmojiPopupShownListener { conversationBinding.ivEmoji.setImageResource(R.drawable.ic_keyboard) }
            .setOnSoftKeyboardOpenListener { ignore -> Log.d(TAG, "Opened soft keyboard") }
            .setOnEmojiPopupDismissListener { conversationBinding.ivEmoji.setImageResource(R.drawable.happy_face) }
            .setOnSoftKeyboardCloseListener { Log.d(TAG, "Closed soft keyboard") }
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


    companion object {
        fun startConversionActivity(context: Context, inboxEntity: InboxEntity) {
            val intent = Intent(context, ConversationActivity::class.java).apply {

            }
            intent.putExtra(CHAT_ROOM_OBJECT, inboxEntity)
            context.startActivity(intent)

        }

    }

    override fun onPause() {
        super.onPause()
        MediaPlayerManager.getInstance().release()
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
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
                if (intent.hasExtra(JoshCameraActivity.IMAGE_RESULTS)) {
                    val returnValue =
                        intent.getStringArrayListExtra(JoshCameraActivity.IMAGE_RESULTS);
                    returnValue?.get(0)?.let { addUserImageInView(it) }
                } else if (intent.hasExtra(JoshCameraActivity.VIDEO_RESULTS)) {
                    val videoPath = intent.getStringExtra(JoshCameraActivity.VIDEO_RESULTS)
                    addUserVideoInView(videoPath)

                } else {

                }
            }


        }


        super.onActivityResult(requestCode, resultCode, data)
    }

    fun uploadImageByUser() {
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

        conversationBinding.chatRv.addView(
            MessageBuilderFactory.getMessage(
                activityRef,
                BASE_MESSAGE_TYPE.IM,
                tImageMessage
            )
        )
        conversationViewModel.uploadMedia(
            imageUpdatedPath, tImageMessage
        )

        scrollToEnd()
    }

    private fun addUserVideoInView(videoPath: String) {
        Log.e("selectedImagePath", videoPath)
        var videoSentFile = AppDirectory.videoSentFile()
        var flag = AppDirectory.copy(videoPath, videoSentFile.absolutePath)
        var tVideoMessage = TVideoMessage(videoSentFile.absolutePath, videoSentFile.absolutePath)

        conversationBinding.chatRv.addView(
            MessageBuilderFactory.getMessage(
                activityRef,
                BASE_MESSAGE_TYPE.VI,
                tVideoMessage
            )
        )
        conversationViewModel.uploadMedia(
            videoSentFile.absolutePath, tVideoMessage
        )
        scrollToEnd()
    }


    private fun scrollToEnd() {
        AppObjectController.uiHandler.postDelayed({
            conversationBinding.chatRv.smoothScrollToPosition(
                conversationBinding.chatRv.adapter?.itemCount ?: 0
            )
        }, 250)

    }

    fun uploadAttachment() {
        AttachmentUtil.revealAttachments(revealAttachmentView, conversationBinding)
        revealAttachmentView = !revealAttachmentView

    }


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        /* if (intent != null) {
             if (intent.hasExtra(MEDIA_) && intent.getBooleanExtra(MEDIA_, false)) {
                 val videoUri = intent.getStringExtra(VideoRecordActivity.VIDEO_URI)
                 val videoScreenshot =
                     intent.getStringExtra(VideoRecordActivity.VIDEO_SCREENSHOT)
                 Log.e("video path", videoUri + "   sc  " + videoScreenshot)

             }


         }*/
    }

    override fun onResume() {
        super.onResume()
        activityRef = WeakReference(this)
        subscribeRXBus()
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
                    token?.continuePermissionRequest();

                }

            }).withErrorListener {
                if (settingFlag) {
                    openSettings()
                }

            }
            .onSameThread()
            .check();
    }

}
