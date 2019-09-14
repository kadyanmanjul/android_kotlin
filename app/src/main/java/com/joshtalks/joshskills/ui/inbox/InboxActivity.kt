package com.joshtalks.joshskills.ui.inbox

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.size
import androidx.lifecycle.*
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.messaging.RxBus
import com.joshtalks.joshskills.repository.local.DatabaseUtils
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.ui.chat.ConversationActivity
import com.joshtalks.joshskills.ui.sign_up_old.RegisterInfoActivity
import com.joshtalks.joshskills.ui.view_holders.EmptyHorizontalView
import com.joshtalks.joshskills.ui.view_holders.InboxViewHolder
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_inbox.*

class InboxActivity : CoreJoshActivity(), LifecycleObserver {

    private val viewModel: InboxViewModel by lazy {
        ViewModelProviders.of(this).get(InboxViewModel::class.java)
    }
    private var compositeDisposable = CompositeDisposable()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inbox)
        setToolbar()
        DatabaseUtils.updateUserMessageSeen()
        //    WorkMangerPapa.startDeviceDetailsUpdate()
        addObserver()
        lifecycle.addObserver(this)
        viewModel.getRegisterCourses()
        AppObjectController.clearDownloadMangerCallback()


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


        viewModel.registerCourseMinimalLiveData.observe(this, Observer {
            if (it == null) {
                startActivity(
                    Intent(
                        this,
                        RegisterInfoActivity::class.java
                    )
                )
            } else {
                if (recycler_view_inbox.size > 0) {
                    return@Observer
                }
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
        Toast.makeText(this,"HH",Toast.LENGTH_LONG).show()
    }
}
