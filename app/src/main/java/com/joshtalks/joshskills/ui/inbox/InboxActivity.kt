package com.joshtalks.joshskills.ui.inbox

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.*
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.service.FCMTokenManager
import com.joshtalks.joshskills.messaging.RxBus
import com.joshtalks.joshskills.repository.local.DatabaseUtils
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.service.SyncChatService
import com.joshtalks.joshskills.ui.chat.ConversationActivity
import com.joshtalks.joshskills.ui.sign_up_old.RegisterInfoActivity
import com.joshtalks.joshskills.ui.view_holders.EmptyHorizontalView
import com.joshtalks.joshskills.ui.view_holders.InboxViewHolder
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_inbox.*

const val REGISTER_INFO_CODE = 2001

class InboxActivity : CoreJoshActivity(), LifecycleObserver {

    private val viewModel: InboxViewModel by lazy {
        ViewModelProviders.of(this).get(InboxViewModel::class.java)
    }
    private var compositeDisposable = CompositeDisposable()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FCMTokenManager.pushToken()
        AppObjectController.joshApplication.updateDeviceDetail()
        DatabaseUtils.updateUserMessageSeen()
        setContentView(R.layout.activity_inbox)
        setToolbar()
        addObserver()
        lifecycle.addObserver(this)
        AppObjectController.clearDownloadMangerCallback()
        if (Mentor.getInstance().hasId()) {
            viewModel.getRegisterCourses()
        }
        SyncChatService.syncChatWithServer()
    }


    private fun setToolbar() {
        val titleView = findViewById<AppCompatTextView>(R.id.text_message_title)
        titleView.text = getString(R.string.inbox_header)

    }


    private fun addObserver() {
        compositeDisposable.add(
            RxBus.getDefault().toObservable()
                .subscribeOn(Schedulers.io()).subscribe({
                    if (it is InboxEntity) {
                        ConversationActivity.startConversionActivity(this, it)
                    }
                }, {
                    it.printStackTrace()

                })
        )


        viewModel.registerCourseNetworkLiveData.observe(this, Observer {
            if (it == null || it.isEmpty()) {
                startActivityForResult(
                    Intent(this, RegisterInfoActivity::class.java),
                    REGISTER_INFO_CODE
                )
            } else {
                recycler_view_inbox.removeAllViews()
                for (inbox in it) {
                    recycler_view_inbox.addView(
                        InboxViewHolder(
                            inbox
                        )
                    )
                }
                addEmptyView()
            }
        })

        viewModel.registerCourseMinimalLiveData.observe(this, Observer {
            if (it != null && it.isNotEmpty()) {
                for (inbox in it) {
                    recycler_view_inbox.addView(
                        InboxViewHolder(
                            inbox
                        )
                    )
                }
                addEmptyView()
            }
        })


    }

    private fun addEmptyView() {
        for (i in 1..8) {
            recycler_view_inbox.addView(EmptyHorizontalView())
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REGISTER_INFO_CODE) {
            finish()
        }
    }


}
