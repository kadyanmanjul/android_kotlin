package com.joshtalks.joshskills.conversationRoom.roomsListing

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.firestore.FirebaseFirestore
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.IS_CONVERSATION_ROOM_ACTIVE
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.USER_PROFILE_FLOW_FROM
import com.joshtalks.joshskills.core.interfaces.ConversationRoomListAction
import com.joshtalks.joshskills.databinding.ActivityConversationsRoomsListingBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.extra.setOnSingleClickListener
import com.joshtalks.joshskills.ui.userprofile.UserProfileActivity
import com.joshtalks.joshskills.ui.voip.WebRtcService
import com.joshtalks.joshskills.ui.voip.WebRtcService.Companion.isConversionRoomActive
import com.joshtalks.joshskills.ui.voip.WebRtcService.Companion.isRoomCreatedByUser
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers


class ConversationRoomListingActivity : BaseActivity(),
    ConversationRoomListAction {

    private val db = FirebaseFirestore.getInstance()
    private val notebookRef = db.collection("conversation_rooms")
    private var conversationRoomsListingAdapter: ConversationRoomsListingAdapter? = null
    lateinit var viewModel: ConversationRoomListingViewModel
    lateinit var binding: ActivityConversationsRoomsListingBinding
    private val compositeDisposable = CompositeDisposable()
    private var internetAvailableFlag: Boolean = true
    private var isBackPressed: Boolean = false
    var isActivityOpenFromNotification: Boolean = false
    var roomId: String = ""
    var handler: Handler? = null
    var runnable: Runnable? = null

    companion object {
        var CONVERSATION_ROOM_VISIBLE_TRACK_FLAG: Boolean = true
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        getIntentExtras(intent)
        openConversationRoomByNotificationIntent()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PrefManager.put(IS_CONVERSATION_ROOM_ACTIVE, true)
        binding = ActivityConversationsRoomsListingBinding.inflate(layoutInflater)
        val view = binding.root
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.window.statusBarColor =
                this.resources.getColor(R.color.conversation_room_color, theme)
        }
        handler = Handler(Looper.getMainLooper())
        setContentView(view)
        viewModel = ConversationRoomListingViewModel()
        getIntentExtras(intent)
        setUpRecyclerView()
        setFlagInWebRtcServie()
        viewModel.makeEnterExitConversationRoom(true)

        with(binding) {
            createRoom.apply {
                clipToOutline = true
                setOnSingleClickListener {
                    showPopup()
                }
            }
        }
        viewModel.navigation.observe(this, {
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
        openConversationRoomByNotificationIntent()

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
                    String.format("\uD83D\uDC4B Hi there! \n\n Start a new room to get a\n conversation going!")
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
                layoutManager = LinearLayoutManager(this@ConversationRoomListingActivity)
                adapter = conversationRoomsListingAdapter
            }
        }
    }

    private fun openConversationRoomByNotificationIntent() {
        if (isActivityOpenFromNotification && roomId.isNotEmpty()) {
            Handler(Looper.getMainLooper()).postDelayed({
                notebookRef.document(roomId).get().addOnSuccessListener {
                    viewModel.joinRoom(
                        ConversationRoomsListingItem(
                            it["channel_name"]?.toString() ?: "",
                            it["topic"]?.toString(),
                            it["started_by"]?.toString()?.toInt(),
                            roomId.toInt(),
                            null
                        )
                    )
                }
            }, 200)
        }
    }

    private fun setFlagInWebRtcServie() {
        val intent = Intent(this, WebRtcService::class.java)
        isConversionRoomActive = true
        startService(intent)
    }

    fun goToProfile() {
        UserProfileActivity.startUserProfileActivity(
            this, Mentor.getInstance().getId(),
            arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
            null, USER_PROFILE_FLOW_FROM.AWARD.value,
            conversationId = intent.getStringExtra(CONVERSATION_ID)
        )
    }

    override fun onResume() {
        super.onResume()
        observeNetwork()
    }

    private fun observeNetwork() {
        compositeDisposable.add(
            ReactiveNetwork.observeNetworkConnectivity(applicationContext)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { connectivity ->
                    internetAvailableFlag = connectivity.available()
                    Log.d("ABC", "internetAvailableFlag: $internetAvailableFlag ${connectivity.available()}")
                    if (internetAvailableFlag) {
                        internetAvailable()
                    } else {
                        internetNotAvailable()
                    }
                }
        )
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
        //ConversationLiveRoomActivity.startConversationLiveRoomActivity(this,channelName,uid,token,isRoomCreatedByUser,roomId)
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
            Toast.makeText(this, "Something Went Wrong !!!", Toast.LENGTH_SHORT).show()
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


    private fun showPopup() {
        var topic = ""
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.alert_label_editor, null)
        dialogBuilder.setView(dialogView)

        val alertDialog: AlertDialog = dialogBuilder.create()
        val width = AppObjectController.screenWidth * .8
        val height = ViewGroup.LayoutParams.WRAP_CONTENT
        alertDialog.show()
        alertDialog.window?.setLayout(width.toInt(), height)
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogView.findViewById<MaterialTextView>(R.id.create_room).setOnClickListener {
            topic = dialogView.findViewById<EditText>(R.id.label_field).text.toString()
            viewModel.createRoom(topic,false)
            alertDialog.dismiss()
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

    override fun onBackPressed() {
        super.onBackPressed()
        isBackPressed = true
        viewModel.makeEnterExitConversationRoom(false)
    }

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
        viewModel.joinRoom(item)
    }

}