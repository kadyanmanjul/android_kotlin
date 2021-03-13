package com.joshtalks.joshskills.ui.chat

import android.content.Intent
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.afollestad.materialdialogs.MaterialDialog
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.custom_ui.JoshSnackBar
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.messaging.MessageBuilderFactory
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.server.chat_message.*
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.Date

abstract class BaseConversationActivity : CoreJoshActivity() {
    private lateinit var internetAvailableStatus: Snackbar
    protected val uiHandler = AppObjectController.uiHandler
    protected val compositeDisposable = CompositeDisposable()
    protected var internetAvailableFlag: Boolean = true

    override fun onResume() {
        super.onResume()
        observeNetwork()
    }

    protected fun initSnackBar() {
        internetAvailableStatus = JoshSnackBar.builder().setActivity(this)
            .setBackgroundColor(ContextCompat.getColor(application, R.color.white))
            .setActionText("Please enable")
            .setDuration(JoshSnackBar.LENGTH_INDEFINITE)
            .setTextSize(14f)
            .setTextColor(ContextCompat.getColor(application, R.color.gray_79))
            .setText(getString(R.string.internet_not_available_msz))
            .setMaxLines(1)
            .setActionTextColor(ContextCompat.getColor(application, R.color.action_color))
            .setActionTextSize(12f)
            .setActionClickListener {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            }
            .build()
    }


    private fun internetNotAvailable() {
        if (::internetAvailableStatus.isInitialized) {
            internetAvailableStatus.show()
        }
    }

    private fun internetAvailable() {
        if (::internetAvailableStatus.isInitialized) {
            internetAvailableStatus.dismiss()
        }

    }

    protected fun clearMediaFromInternal(conversationId: String) {
        PermissionUtils.storageReadAndWritePermission(this,
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            MaterialDialog(this@BaseConversationActivity).show {
                                title(R.string.delete_media_title)
                                message(R.string.delete_media_message) {
                                    lineSpacing(1.4f)
                                }
                                positiveButton(R.string.yes) { dialog ->
                                    mediaClearWorker(conversationId)
                                }
                                negativeButton(R.string.no) { dialog ->
                                }
                            }
                            return
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            PermissionUtils.permissionPermanentlyDeniedDialog(this@BaseConversationActivity)
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

    private fun mediaClearWorker(conversationId: String) {
        val observer = Observer<WorkInfo> { workInfo ->
            try {
                workInfo?.run {
                    if (state == WorkInfo.State.ENQUEUED) {
                        showProgressBar()
                    } else if (state == WorkInfo.State.SUCCEEDED) {
                        refreshForceList()
                        uiHandler.postDelayed({
                            hideProgressBar()
                        }, 2000)
                    }
                }

            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        WorkManager.getInstance(applicationContext)
            .getWorkInfoByIdLiveData(WorkManagerAdmin.clearMediaOfConversation(conversationId))
            .observe(this, observer)
    }

    fun refreshForceList() {}


    private fun observeNetwork() {
        compositeDisposable.add(
            ReactiveNetwork.observeNetworkConnectivity(applicationContext)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { connectivity ->
                    internetAvailableFlag = connectivity.available()
                    if (internetAvailableFlag) {
                        internetAvailable()
                    } else {
                        internetNotAvailable()
                    }
                })
    }

    protected fun getNewMessageObj(lastMessageTime: Date): ChatModel {
        return ChatModel(
            type = BASE_MESSAGE_TYPE.NEW_CLASS,
            text = getString(R.string.aapki_new_class)
        ).apply {
            created = lastMessageTime
        }
    }

    protected fun getTextMessage(text: String, lastMessage: ChatModel): ChatModel {
        return MessageBuilderFactory.getMessage(BASE_MESSAGE_TYPE.TX, TChatMessage(text)).apply {
            messageTime = lastMessage.messageTime+10
            created = lastMessage.created
        }
    }

    protected fun getAudioMessage(tAudioMessage: TAudioMessage,lastMessage: ChatModel): ChatModel {
        return MessageBuilderFactory.getMessage(BASE_MESSAGE_TYPE.AU, tAudioMessage).apply {
            messageTime = lastMessage.messageTime+10
            created = lastMessage.created
        }
    }

    protected fun getImageMessage(tImageMessage: TImageMessage,lastMessage: ChatModel): ChatModel {
        return MessageBuilderFactory.getMessage(BASE_MESSAGE_TYPE.IM, tImageMessage).apply {
            messageTime = lastMessage.messageTime+10
            created = lastMessage.created
        }
    }

    protected fun getVideoMessage(tVideoMessage: TVideoMessage,lastMessage: ChatModel): ChatModel {
        return MessageBuilderFactory.getMessage(BASE_MESSAGE_TYPE.VI, tVideoMessage).apply {
            messageTime = lastMessage.messageTime+10
            created = lastMessage.created
        }
    }

    protected fun getUnlockClassMessage(lastMessage: ChatModel): ChatModel {
        return MessageBuilderFactory.getMessage(
            BASE_MESSAGE_TYPE.UNLOCK,
            TUnlockClassMessage(getString(R.string.unlock_class_demo))
        ).apply {
            messageTime = lastMessage.messageTime+10
            created = lastMessage.created
        }
    }

/*

   override fun onNewIntent(mIntent: Intent) {
        intent = mIntent
        super.processIntent(mIntent)
        if (intent.hasExtra(UPDATED_CHAT_ROOM_OBJECT)) {
            flowFrom = "Notification"
            val temp = intent.getParcelableExtra(UPDATED_CHAT_ROOM_OBJECT) as InboxEntity?
            if (temp == null) {
                this.finish()
            }
            temp?.let { inboxObj ->
                try {
                    val tempIn: InboxEntity = inboxEntity
                    if (tempIn.conversation_id != inboxObj.conversation_id) {
                        this.inboxEntity = inboxObj
                    }
                } catch (ex: Exception) {
                    this.finish()
                    ex.printStackTrace()
                }
                fetchMessage()
            }
        }
        if (intent.hasExtra(HAS_COURSE_REPORT)) {
            openCourseProgressListingScreen()
        }
        if (intent.hasExtra(FOCUS_ON_CHAT_ID)) {
            mIntent.getParcelableExtra<ChatModel>(FOCUS_ON_CHAT_ID)?.chatId?.run {
                scrollToPosition(this)
            }
        }
        notificationActionProcess()
        super.onNewIntent(mIntent)
    }


    private fun notificationActionProcess() {
        val questionId = intent.getStringExtra(QUESTION_ID) ?: EMPTY
        (intent.getSerializableExtra(ShareConstants.ACTION_TYPE) as NotificationAction?)?.let {
            if (it == NotificationAction.ACTION_OPEN_QUESTION && questionId.isNotEmpty()) {
                intent.removeExtra(QUESTION_ID)
                intent.removeExtra(ShareConstants.ACTION_TYPE)
                CoroutineScope(Dispatchers.IO).launch {
                    val question: Question? =
                        AppObjectController.appDatabase.chatDao().getQuestionOnIdV2(questionId)
                    if (question != null) {
                        val chatModel: ChatModel =
                            AppObjectController.appDatabase.chatDao()
                                .getUpdatedChatObjectViaId(question.chatId)

                        when {
                            question.type == BASE_MESSAGE_TYPE.QUIZ || question.type == BASE_MESSAGE_TYPE.TEST -> {
                                AssessmentActivity.startAssessmentActivity(
                                    this@ConversationActivity,
                                    ASSESSMENT_REQUEST_CODE,
                                    question.assessmentId ?: 0
                                )
                            }
                            question.type == BASE_MESSAGE_TYPE.CP -> {

                                chatModel.question?.conversationPracticeId?.let { cpId ->
                                    ConversationPracticeActivity.startConversationPracticeActivity(
                                        this@ConversationActivity,
                                        CONVERSATION_PRACTISE_REQUEST_CODE,
                                        cpId,
                                        chatModel.question?.imageList?.getOrNull(0)?.imageUrl
                                    )
                                }
                            }
                            question.type == BASE_MESSAGE_TYPE.PR -> {
                                PractiseSubmitActivity.startPractiseSubmissionActivity(
                                    this@ConversationActivity,
                                    PRACTISE_SUBMIT_REQUEST_CODE,
                                    chatModel
                                )
                            }
                            question.material_type == BASE_MESSAGE_TYPE.VI -> {
                                VideoPlayerActivity.startConversionActivity(
                                    this@ConversationActivity,
                                    chatModel,
                                    inboxEntity.course_name,
                                    inboxEntity.duration
                                )
                            }
                            else -> {
                                return@launch
                            }
                        }
                    }
                }
            }

        }
    }
*/


}