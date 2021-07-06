package com.joshtalks.joshskills.conversationRoom.roomsListing

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.conversationRoom.liveRooms.ConversationLiveRoomActivity
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.IS_CONVERSATION_ROOM_ACTIVE
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.USER_PROFILE_FLOW_FROM
import com.joshtalks.joshskills.core.interfaces.ConversationRoomListAction
import com.joshtalks.joshskills.core.setUserImageRectOrInitials
import com.joshtalks.joshskills.databinding.ActivityConversationsRoomsListingBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.extra.setOnSingleClickListener
import com.joshtalks.joshskills.ui.userprofile.UserProfileActivity
import com.joshtalks.joshskills.ui.voip.WebRtcService
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers


class ConversationRoomListingActivity : BaseActivity(),
    ConversationRoomListAction {

    private val db = FirebaseFirestore.getInstance()
    private val notebookRef = db.collection("conversation_rooms")
    private var adapter: ConversationRoomsListingAdapter? = null
    lateinit var viewModel: ConversationRoomListingViewModel
    lateinit var binding: ActivityConversationsRoomsListingBinding
    private val compositeDisposable = CompositeDisposable()
    private var internetAvailableFlag: Boolean = true
    private var isBackPressed: Boolean = false

    companion object {
        var CONVERSATION_ROOM_VISIBLE_TRACK_FLAG: Boolean = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PrefManager.put(IS_CONVERSATION_ROOM_ACTIVE, true)
        binding = ActivityConversationsRoomsListingBinding.inflate(layoutInflater)
        val view = binding.root
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(view)
        viewModel = ConversationRoomListingViewModel()
        setUpRecyclerView()
        setFlagInWebRtcServie()
        viewModel.makeEnterExitConversationRoom(true)
        binding.createRoom.apply {
            clipToOutline = true
            setOnSingleClickListener {
                showPopup()
            }
        }

        binding.userPic.clipToOutline = true
        binding.userPic.setUserImageRectOrInitials(
            Mentor.getInstance().getUser()?.photo,
            User.getInstance().firstName ?: "JS",
            16,
            true,
            8,
            textColor = R.color.black,
            bgColor = R.color.conversation_room_gray
        )
        binding.userPic.setOnSingleClickListener {
            goToProfile()
        }

        viewModel.navigation.observe(this, {
            when (it) {
                is ConversationRoomListingNavigation.ApiCallError -> showApiCallErrorToast()
                is ConversationRoomListingNavigation.OpenConversationLiveRoom -> openConversationLiveRoom(
                    it.channelName,
                    it.uid,
                    it.token,
                    it.isRoomCreatedByUser,
                    it.roomId
                )
            }
        })

    }

    private fun setFlagInWebRtcServie() {
        val intent = Intent(this, WebRtcService::class.java)
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
                    if (internetAvailableFlag) {
                        internetAvailable()
                    } else {
                        internetNotAvailable()
                    }
                }
        )
    }

    private fun internetNotAvailable() {
        binding.notificationBar.visibility = View.VISIBLE
        binding.notificationBar.hideActionLayout()
        binding.notificationBar.setHeading("The Internet connection appears to be offline")
        binding.notificationBar.setBackgroundColor(false)
    }

    private fun internetAvailable() {
        binding.notificationBar.visibility = View.GONE
    }

    private fun openConversationLiveRoom(
        channelName: String?,
        uid: Int?,
        token: String?,
        isRoomCreatedByUser: Boolean,
        roomId: Int?
    ) {
        CONVERSATION_ROOM_VISIBLE_TRACK_FLAG = false
        val intent = Intent(this, ConversationLiveRoomActivity::class.java)
        intent.putExtra("CHANNEL_NAME", channelName)
        intent.putExtra("UID", uid)
        intent.putExtra("TOKEN", token)
        intent.putExtra("IS_ROOM_CREATED_BY_USER", isRoomCreatedByUser)
        intent.putExtra("ROOM_ID", roomId)
        startActivity(intent)
    }

    private fun showApiCallErrorToast() {
        Toast.makeText(this, "Something went wrong. Please try Again!!!", Toast.LENGTH_SHORT).show()
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
            viewModel.createRoom(topic)
            alertDialog.dismiss()
        }

        dialogView.findViewById<MaterialTextView>(R.id.cancel).setOnClickListener {
            alertDialog.dismiss()
        }

    }

    override fun onStart() {
        super.onStart()
        adapter?.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter?.stopListening()
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
    }

    override fun onBackPressed() {
        super.onBackPressed()
        isBackPressed = true
        viewModel.makeEnterExitConversationRoom(false)
    }

    private fun setUpRecyclerView() {
        val query: Query = notebookRef
        val options: FirestoreRecyclerOptions<ConversationRoomsListingItem> =
            FirestoreRecyclerOptions.Builder<ConversationRoomsListingItem>()
                .setQuery(query, ConversationRoomsListingItem::class.java)
                .build()
        adapter = ConversationRoomsListingAdapter(options, this)
        query.addSnapshotListener { value, error ->
            if (error != null) {
                return@addSnapshotListener
            } else {
                if (value == null || value.isEmpty) {
                    binding.noRoomsText.visibility = View.VISIBLE
                    binding.noRoomsText.text =
                        String.format("\uD83D\uDC4B Hi there! \n\n Start a new room to get a\n conversation going!")
                    binding.recyclerView.visibility = View.GONE
                } else {
                    binding.noRoomsText.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE

                    binding.recyclerView.setHasFixedSize(true)
                    binding.recyclerView.layoutManager = LinearLayoutManager(this)
                    binding.recyclerView.adapter = adapter
                }
            }
        }
    }

    override fun onRoomClick(item: ConversationRoomsListingItem) {
        viewModel.joinRoom(item)
    }

}