package com.joshtalks.joshskills.common.ui.chat

import android.content.Intent
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.afollestad.materialdialogs.MaterialDialog
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.CoreJoshActivity
import com.joshtalks.joshskills.common.core.PermissionUtils
import com.joshtalks.joshskills.common.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.common.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.common.core.analytics.ParamKeys
import com.joshtalks.joshskills.common.core.custom_ui.JoshSnackBar
import com.joshtalks.joshskills.common.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.common.messaging.MessageBuilderFactory
import com.joshtalks.joshskills.common.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.common.repository.local.entity.ChatModel
import com.joshtalks.joshskills.common.repository.server.chat_message.*
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.*

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
            .setBackgroundColor(ContextCompat.getColor(application, R.color.pure_white))
            .setActionText("Please enable")
            .setDuration(JoshSnackBar.LENGTH_INDEFINITE)
            .setTextSize(14f)
            .setTextColor(ContextCompat.getColor(application, R.color.text_subdued))
            .setText(getString(R.string.internet_not_available_msz))
            .setMaxLines(1)
            .setActionTextColor(ContextCompat.getColor(application, R.color.success))
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
        PermissionUtils.storageReadAndWritePermission(
            this,
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
                                    MixPanelTracker.publishEvent(MixPanelEvent.CLEAR_ALL_MEDIA)
                                        .addParam(ParamKeys.IS_SUCCESS,true)
                                        .push()
                                }
                                negativeButton(R.string.no) { dialog ->
                                    MixPanelTracker.publishEvent(MixPanelEvent.CLEAR_ALL_MEDIA)
                                        .addParam(ParamKeys.IS_SUCCESS,false)
                                        .push()
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
            }
        )
    }

    private fun mediaClearWorker(conversationId: String) {
        val observer = Observer<WorkInfo> { workInfo ->
            try {
                workInfo?.run {
                    if (state == WorkInfo.State.ENQUEUED) {
                        showProgressBar()
                    } else if (state == WorkInfo.State.SUCCEEDED) {
                        refreshForceList()
                        uiHandler.postDelayed(
                            {
                                hideProgressBar()
                            },
                            2000
                        )
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
                }
        )
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
        return com.joshtalks.joshskills.common.messaging.MessageBuilderFactory.getMessage(BASE_MESSAGE_TYPE.TX, TChatMessage(text)).apply {
            messageTime = lastMessage.messageTime + 10
            created = lastMessage.created
        }
    }

    protected fun getAudioMessage(tAudioMessage: TAudioMessage, lastMessage: ChatModel): ChatModel {
        return com.joshtalks.joshskills.common.messaging.MessageBuilderFactory.getMessage(BASE_MESSAGE_TYPE.AU, tAudioMessage).apply {
            messageTime = lastMessage.messageTime + 10
            created = lastMessage.created
        }
    }

    protected fun getImageMessage(tImageMessage: TImageMessage, lastMessage: ChatModel): ChatModel {
        return com.joshtalks.joshskills.common.messaging.MessageBuilderFactory.getMessage(BASE_MESSAGE_TYPE.IM, tImageMessage).apply {
            messageTime = lastMessage.messageTime + 10
            created = lastMessage.created
        }
    }

    protected fun getVideoMessage(tVideoMessage: TVideoMessage, lastMessage: ChatModel): ChatModel {
        return com.joshtalks.joshskills.common.messaging.MessageBuilderFactory.getMessage(BASE_MESSAGE_TYPE.VI, tVideoMessage).apply {
            messageTime = lastMessage.messageTime + 10
            created = lastMessage.created
        }
    }

    protected fun getUnlockClassMessage(lastMessage: ChatModel): ChatModel {
        return com.joshtalks.joshskills.common.messaging.MessageBuilderFactory.getMessage(
            BASE_MESSAGE_TYPE.UNLOCK,
            TUnlockClassMessage(getString(R.string.unlock_class_demo))
        ).apply {
            messageTime = lastMessage.messageTime + 10
            created = lastMessage.created
        }
    }
}
