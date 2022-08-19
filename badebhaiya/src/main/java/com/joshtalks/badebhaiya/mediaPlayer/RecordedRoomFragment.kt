package com.joshtalks.badebhaiya.mediaPlayer

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.os.Bundle
import android.transition.Fade
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.transition.MaterialSharedAxis
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.EMPTY
import com.joshtalks.badebhaiya.core.NotificationChannelNames
import com.joshtalks.badebhaiya.core.setOnSingleClickListener
import com.joshtalks.badebhaiya.core.showToast
import com.joshtalks.badebhaiya.databinding.FragmentRecordRoomBinding
import com.joshtalks.badebhaiya.deeplink.DeeplinkGenerator
import com.joshtalks.badebhaiya.feed.FeedViewModel
import com.joshtalks.badebhaiya.feed.model.Room
import com.joshtalks.badebhaiya.feed.model.SpeakerData
import com.joshtalks.badebhaiya.liveroom.LiveRoomFragment
import com.joshtalks.badebhaiya.feed.model.RoomListResponseItem
import com.joshtalks.badebhaiya.impressions.Impression
import com.joshtalks.badebhaiya.liveroom.LiveRoomState
import com.joshtalks.badebhaiya.liveroom.viewmodel.LiveRoomViewModel
import com.joshtalks.badebhaiya.recordedRoomPlayer.AudioPlayerService
import com.joshtalks.badebhaiya.profile.ProfileFragment
import com.joshtalks.badebhaiya.profile.ProfileViewModel
import com.joshtalks.badebhaiya.recordedRoomPlayer.MusicServiceConnection
import com.joshtalks.badebhaiya.recordedRoomPlayer.PlayerData
import com.joshtalks.badebhaiya.recordedRoomPlayer.isPlaying
import com.joshtalks.badebhaiya.recordedRoomPlayer.listeners.ListenersListFragment
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.utils.DEFAULT_NAME
import com.joshtalks.badebhaiya.utils.datetimeutils.DateTimeUtils
import com.joshtalks.badebhaiya.utils.doForLoggedInUser
import com.joshtalks.badebhaiya.utils.pendingActions.PendingActionsManager
import com.joshtalks.badebhaiya.utils.setUserImageRectOrInitials
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_record_room.view.*
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class RecordedRoomFragment : Fragment() {

    companion object {
        const val TAG = "RecordedRoomFragment"
        const val ROOM_DATA = "room_data"

        fun newInstance() = RecordedRoomFragment()
        fun open(activity: AppCompatActivity, from: String, room: RoomListResponseItem?, feedViewModel: FeedViewModel? = null) {
//            LiveRoomFragment.removeIfFound(activity)

            MainScope().launch {
                AudioPlayerService.playingRoomId?.let {
                    if (it == room!!.roomId){
                        feedViewModel?.let { feedVm ->
                            feedVm.expandRecordedRoom.postValue(LiveRoomState.EXPANDED)
                            return@launch
                        }
                    }
                }

                feedViewModel?.let {
                    it.finishLiveRoom.emit(true)
                }

                val liveRoomFragment = activity.supportFragmentManager.findFragmentByTag(LiveRoomFragment.TAG)

                val foundFragment = activity.supportFragmentManager.findFragmentByTag(TAG)

                AudioPlayerService.setAudio(room!!)

                if (liveRoomFragment != null)
                    delay(500)

//            activity.stopService(Intent(activity.applicationContext, AudioPlayerService::class.java))

                foundFragment?.let {
                    activity.supportFragmentManager.beginTransaction().remove(it).commit()
//                CoroutineScope(Dispatchers.Main).launch {
//                    PlayerData.initPlayer.emit(true)
//                }
                }

                val fragment = RecordedRoomFragment() // replace your custom fragment class
                val bundle = Bundle()
                fragment?.apply {
                    exitTransition = MaterialSharedAxis(
                        MaterialSharedAxis.Z,
                        /* forward= */ false
                    ).apply {
                        duration = 500
                    }
                }
                bundle.putString("source", from) // use as per your need
                bundle.putParcelable(ROOM_DATA,room)
                fragment.arguments = bundle

//           try {
               activity
                   .supportFragmentManager
                   .beginTransaction()
                   .replace(R.id.feedRoot, fragment, TAG)
                   .addToBackStack(TAG)
                   .commit()
//           } catch (e: Exception){
//               showToast("Something Went Wrong")
//           }
        }
    }

        fun removeIfFound(activity: AppCompatActivity){
            val foundFragment = activity.supportFragmentManager.findFragmentByTag(TAG)
            foundFragment?.let {
                activity.supportFragmentManager.beginTransaction().remove(it).commit()
            }
        }
    }

    //    private var mediaPlayer : MediaPlayer?=null
    private var from: String = EMPTY
    private var url: String = EMPTY
     var roomData: RoomListResponseItem? = null
    //
//    private var mediaPlayer: MediaPlayer? = null
    private var shouldUpdateSeekbar = true

//    @Inject
//    lateinit var exoPlayer: ExoPlayer

    lateinit var notificationManager: NotificationManager

    private val viewModel: RecordedRoomViewModel by viewModels()

    private val feedViewModel by lazy {
        ViewModelProvider(requireActivity())[FeedViewModel::class.java]
    }

    private val profileViewModel by lazy {
        ViewModelProvider(this)[ProfileViewModel::class.java]
    }


    lateinit var binding: FragmentRecordRoomBinding

    private val notificationChannelId = "MediaOne"
    private var notificationChannelName = NotificationChannelNames.DEFAULT.type
    private val vm by lazy { ViewModelProvider(requireActivity()).get(LiveRoomViewModel::class.java) }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Timber.tag("audioservice").d("RECORDED FRAGMENT IS ON CREATE")
//        binding= DataBindingUtil.setContentView(requireActivity(), R.layout.fragment_record_room)
        binding = FragmentRecordRoomBinding.inflate(inflater, container, false)
        binding.recordedViewModel = viewModel
        binding.handler = this
        binding.user = User.getInstance()
//        viewModel = ViewModelProvider(this).get(RecordedRoomViewModel::class.java)
        viewModel.lvRoomState.value = LiveRoomState.EXPANDED
        vm.deflate.value=true
        var mBundle: Bundle? = Bundle()
        mBundle = this.arguments
        from = mBundle?.getString("source").toString()
        roomData = mBundle?.getParcelable(ROOM_DATA)
        binding.roomData=roomData
        clickListener()
        attachBackPressedDispatcher()
        lifecycleScope.launch {
            delay(500)
            viewModel.initPlayer()
        }
        Log.i("RECORDS", "onCreateView: $url")
        binding.profilePic.apply {
            clipToOutline = true
            setUserImageRectOrInitials(
                roomData?.speakersData?.photoUrl,
                roomData?.speakersData?.shortName ?: DEFAULT_NAME,
                202,
                true,
                16,
                textColor = R.color.black,
                bgColor = R.color.conversation_room_gray
            )
        }

        Timber.tag("usercount").d("USERS COUNT => ${roomData?.users_count}")

        Timber.tag("audiostarttime").d("AUDIO START TIME DATA => ${roomData?.displayStartDateTime()}")

//        binding.userPhoto.apply {
//            clipToOutline = true
//            setUserImageRectOrInitials(
//                User.getInstance().profilePicUrl,
//                User.getInstance().firstName ?: DEFAULT_NAME,
//                22,
//                true,
//                16,
//                textColor = R.color.black,
//                bgColor = R.color.conversation_room_gray
//            )
//        }
        trackRecordRoomState()

//        createChannel()

        binding.seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if (p2) {
                    setCurPlayerTimeToTextView(p1.toLong())
                }
//                if(p2 && load.isCompleted){
//                    mediaPlayer!!.seekTo(p1)
//                }
//                seekbar.getThumb().setAlpha(255)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                binding.seekbar.thumb.alpha = 255
                shouldUpdateSeekbar = false

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                p0?.let {
                    binding.seekbar.thumb.alpha = 255
                    viewModel.seekTo(it.progress.toLong())
                    shouldUpdateSeekbar = true
                }
            }

        })

        return binding.root
    }

    private fun attachBackPressedDispatcher() {
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            handleBackPress(this)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(
            MaterialSharedAxis.Z,
            /* forward= */ true
        ).apply {
            duration = 500
        }
        returnTransition = MaterialSharedAxis(
            MaterialSharedAxis.Z,
            /* forward= */ false
        ).apply {
            duration = 500
        }
    }

    override fun onResume() {
        binding.userPhoto.setUrlAndName(User.getInstance().profilePicUrl, User.getInstance().firstName)
        super.onResume()
    }

    fun onProfileClicked() {
        requireActivity().doForLoggedInUser {
            binding.recordedRoomRootView.transitionToEnd()
            val fragment = ProfileFragment() // replace your custom fragment class
            profileViewModel.sendEvent(Impression("FEED_SCREEN","CLICKED_OWN_PROFILE"))

            val bundle = Bundle()
//        fragment?.apply {
//            exitTransition = MaterialSharedAxis(
//                MaterialSharedAxis.Z,
//                /* forward= */ false
//            ).apply {
//                duration = 500
//            }
//        }
            val fragmentTransaction: FragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
            bundle.putString("user", User.getInstance().userId) // use as per your need
            bundle.putString("source","FEED_SCREEN")

            fragment.arguments = bundle
//        fragmentTransaction.setCustomAnimations(R.anim.fade_in,R.anim.fade_out, R.anim.fade_in,R.anim.fade_out)
            fragmentTransaction.replace(R.id.fragmentContainer, fragment)
            fragmentTransaction.addToBackStack(ProfileFragment.TAG)
            fragmentTransaction.commit()
        }
    }

    private fun handleBackPress(onBackPressedCallback: OnBackPressedCallback) {
        if (viewModel.lvRoomState.value  == LiveRoomState.EXPANDED){
            // Minimise live room.
            collapseLiveRoom()
        } else {
            // Live is already minimized ask if user wants to quit live room.
            if(from=="Profile")
            {
                feedViewModel.isBackPressed.value=true
                from="None"
            }
            else
            {
                vm.deflate.value=false
                activity?.supportFragmentManager?.popBackStack()
//                        finishFragment()
//                        return
            }
            feedViewModel.isBackPressed.value = false
        }

    }



 fun finishFragment(){
    if (isAdded){
        Timber.d("finishFragment: ")
        requireActivity().supportFragmentManager.popBackStack()
    }
}

    fun endFragment(){
        activity?.supportFragmentManager?.popBackStack()
    }

    fun convert(duration: Int): String { // to convert mill sec to minutes and seconds
        return String.format(
            "%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(duration.toLong()),
            TimeUnit.MILLISECONDS.toSeconds(duration.toLong()) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration.toLong()))
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Timber.tag("roomdestroy").d("RECORDED ROOM ONDESTROY")
        vm.deflate.value=false
        viewModel.destroyPlayer()
        Timber.tag("audioservice").d("RECORDED FRAGMENT IS ON DESTROY")

    }

    fun collapseLiveRoom() {
        binding.recordedRoomRootView.transitionToEnd()
        viewModel.lvRoomState.value = LiveRoomState.COLLAPSED
    }

    fun expandLiveRoom() {
        binding.recordedRoomRootView.transitionToStart()
//        viewModel.lvRoomState.value=LiveRoomState.EXPANDED
    }


    private fun trackRecordRoomState() {
        binding.recordedRoomRootView.addTransitionListener(object : MotionLayout.TransitionListener{
            override fun onTransitionStarted(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int
            ) {
            }

            override fun onTransitionChange(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int,
                progress: Float
            ) {
            }

            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                if (currentId == R.id.collapsed){
                    viewModel.lvRoomState.value = LiveRoomState.COLLAPSED
                } else {
                    viewModel.lvRoomState.value = LiveRoomState.EXPANDED
                }
            }

            override fun onTransitionTrigger(
                motionLayout: MotionLayout?,
                triggerId: Int,
                positive: Boolean,
                progress: Float
            ) {
            }

        })
    }

    fun addObserver() {

        feedViewModel.expandRecordedRoom.observe(viewLifecycleOwner){ roomState ->
            roomState?.let {
                when(it){
                    LiveRoomState.EXPANDED -> expandLiveRoom()
                    LiveRoomState.COLLAPSED -> {}
                }
            }
        }

        viewModel.curSongDuration.observe(viewLifecycleOwner) {
            if (it > 0){
                binding.seekbar.max = it.toInt()
                val sec = DateTimeUtils.millisToTime(it)
                Timber.tag("totaltime").d("TOTAL TIME IS => $sec")
                binding.totalTime.text = sec
            }
        }

        viewModel.curPlayerPosition.observe(viewLifecycleOwner) {
            if (shouldUpdateSeekbar) {
                binding.seekbar.progress = it.toInt()
                setCurPlayerTimeToTextView(it)
            }
        }

        viewModel.playbackState.observe(viewLifecycleOwner) { playBackState ->
            binding.pause.setImageResource(
                if (playBackState?.isPlaying == true) R.drawable.ic_pause_icon else R.drawable.ic_play
            )
            binding.seekbar.progress = playBackState?.position?.toInt() ?: 0
        }

        viewModel.lvRoomState.observe(viewLifecycleOwner) {
            when (it) {
                LiveRoomState.EXPANDED -> expandLiveRoom()
                LiveRoomState.COLLAPSED -> {}
            }
        }

    }


    @SuppressLint("SetTextI18n")
    private fun clickListener() {
//        viewModel.lvRoomState.value = LiveRoomState.EXPANDED

        binding.leaveEndRoomBtn.setOnClickListener {
//                expandLiveRoom()
            activity?.supportFragmentManager?.popBackStack()
        }

        binding.apply {
            shareBtn.setOnClickListener {
                feedViewModel.sendEvent(Impression("MEDIA_PLAYER","CLICKED_RECORD_ROOM_SHARE"))
//                showToast("Feature yet to be added")
            }

//            profilePic.setOnClickListener {
//                collapseLiveRoom()
//                itemClick()
//            }

            audienceCount.setOnClickListener {
                collapseLiveRoom()
                openListenersList()
            }

            roomName.setOnClickListener {
                collapseLiveRoom()
                itemClick()
            }

            moderatorName.setOnClickListener {
                        collapseLiveRoom()
                        itemClick()
            }

            downArrow.setOnClickListener {
                collapseLiveRoom()
            }

            pause.setOnClickListener {

                viewModel.playOrToggleSong()
            }

            playbackSpeed.setOnClickListener {
                when {
                    playbackSpeed.text.toString() == "1x" -> {
                        playbackSpeed.text = "1.25x"
                        viewModel.increaseSpeed(1.25f)
                    }
                    playbackSpeed.text.toString() == "1.25x" -> {
                        playbackSpeed.text = "1.5x"
                        viewModel.increaseSpeed(1.5f)
                    }
                    playbackSpeed.text.toString() == "1.5x" -> {
                        playbackSpeed.text = "1.75x"
                        viewModel.increaseSpeed(1.75f)
                    }
                    playbackSpeed.text.toString() == "1.75x" -> {
                        playbackSpeed.text = "2x"
                        viewModel.increaseSpeed(2f)
                    }
                    else -> {
                        playbackSpeed.text = "1x"
                        viewModel.increaseSpeed(1f)
                    }
                }
            }
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.shareBtn.setOnSingleClickListener {
            viewModel.pausePlayback()
            binding.progressBar.visibility = View.VISIBLE
            DeeplinkGenerator.shareRecordedRoom(requireActivity(), roomData!!) {
                binding.progressBar.visibility = View.GONE
            }
        }
        addObserver()
    }

    private fun setCurPlayerTimeToTextView(ms: Long) {
        val sec = DateTimeUtils.millisToTime(ms)
        binding.currentTime.text = sec
        Timber.tag("audiotime").d("STARTING TIME IS => ${sec}")
    }
    fun itemClick() {
        val nextFrag = ProfileFragment()
        val bundle = Bundle()
        bundle.putString("user", roomData?.speakersData?.userId) // use as per your need
        bundle.putString("source","MEDIA_PLAYER")
        nextFrag.arguments = bundle
        activity?.supportFragmentManager?.beginTransaction()
            ?.replace(R.id.root_view, nextFrag, "findThisFragment")
            //?.addToBackStack(null)
            ?.commit()
    }

    fun openListenersList(){
        ListenersListFragment.open(supportFragmentManager = requireActivity().supportFragmentManager, R.id.fragmentContainer, roomData?.roomId!!)
    }

}