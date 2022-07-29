package com.joshtalks.badebhaiya.mediaPlayer

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.os.Bundle
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
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.google.android.exoplayer2.ExoPlayer
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.EMPTY
import com.joshtalks.badebhaiya.core.NotificationChannelNames
import com.joshtalks.badebhaiya.core.showToast
import com.joshtalks.badebhaiya.databinding.FragmentRecordRoomBinding
import com.joshtalks.badebhaiya.feed.FeedViewModel
import com.joshtalks.badebhaiya.feed.model.RoomListResponseItem
import com.joshtalks.badebhaiya.impressions.Impression
import com.joshtalks.badebhaiya.liveroom.LiveRoomState
import com.joshtalks.badebhaiya.profile.ProfileFragment
import com.joshtalks.badebhaiya.recordedRoomPlayer.MusicServiceConnection
import com.joshtalks.badebhaiya.recordedRoomPlayer.isPlaying
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.utils.DEFAULT_NAME
import com.joshtalks.badebhaiya.utils.datetimeutils.DateTimeUtils
import com.joshtalks.badebhaiya.utils.setUserImageRectOrInitials
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_record_room.view.*
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class RecordedRoomFragment : Fragment() {

    @Inject
    lateinit var musicServiceConnection: MusicServiceConnection

    companion object {
        const val TAG = "RecordedRoomFragment"
        fun newInstance() = RecordedRoomFragment()
        fun open(activity: AppCompatActivity, from: String, room: RoomListResponseItem) {

            val foundFragment = activity.supportFragmentManager.findFragmentByTag(TAG)

//            activity.stopService(Intent(activity.applicationContext, AudioPlayerService::class.java))

            foundFragment?.let {
                activity.supportFragmentManager.beginTransaction().remove(it).commit()
            }

            val fragment = RecordedRoomFragment() // replace your custom fragment class
            val bundle = Bundle()
            bundle.putString("source", from) // use as per your need
            bundle.putString("url",room.recordings?.get(0)?.url)
            bundle.putString("userId", room.speakersData?.userId)
            fragment.arguments = bundle



            activity
                .supportFragmentManager
                .beginTransaction()
                .add(R.id.feedRoot, fragment, TAG)
                .addToBackStack(TAG)
                .commit()
        }
    }

    //    private var mediaPlayer : MediaPlayer?=null
    private var from: String = EMPTY
    private var url: String = EMPTY
    private var userId: String = EMPTY
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


    lateinit var binding: FragmentRecordRoomBinding

    private val notificationChannelId = "MediaOne"
    private var notificationChannelName = NotificationChannelNames.DEFAULT.type

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Timber.tag("audioservice").d("RECORDED FRAGMENT IS ON CREATE")
//        binding= DataBindingUtil.setContentView(requireActivity(), R.layout.fragment_record_room)
        binding = FragmentRecordRoomBinding.inflate(inflater, container, false)
        binding.recordedViewModel = viewModel
//        viewModel = ViewModelProvider(this).get(RecordedRoomViewModel::class.java)

        var mBundle: Bundle? = Bundle()
        mBundle = this.arguments
        from = mBundle?.getString("source").toString()
        url = mBundle?.getString("url").toString()
        userId=mBundle?.getString("userId").toString()
        clickListener()
        attachBackPressedDispatcher()
        binding.profilePic.apply {
            clipToOutline = true
            setUserImageRectOrInitials(
                "https://s3.ap-south-1.amazonaws.com/www.static.skills.com/bb-app//3c137648-af9b-418f-998c-33e76dab09f3.webp",
                "AKHAND" ?: DEFAULT_NAME,
                202,
                true,
                16,
                textColor = R.color.black,
                bgColor = R.color.conversation_room_gray
            )
        }

        binding.userPhoto.apply {
            clipToOutline = true
            setUserImageRectOrInitials(
                User.getInstance().profilePicUrl,
                User.getInstance().firstName ?: DEFAULT_NAME,
                22,
                true,
                16,
                textColor = R.color.black,
                bgColor = R.color.conversation_room_gray
            )
        }
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
        Timber.tag("audioservice").d("RECORDED FRAGMENT IS ON DESTROY")

    }

    fun collapseLiveRoom() {
        binding.liveRoomRootView.transitionToEnd()
        viewModel.lvRoomState.value = LiveRoomState.COLLAPSED
    }

    fun expandLiveRoom() {
        binding.liveRoomRootView.transitionToStart()
//        viewModel.lvRoomState.value=LiveRoomState.EXPANDED
    }


    private fun trackRecordRoomState() {
        binding.liveRoomRootView.addTransitionListener(object : MotionLayout.TransitionListener{
            override fun onTransitionStarted(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int
            ) {
                viewModel.lvRoomState.value = LiveRoomState.EXPANDED
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
                showToast("Feature yet to be added")
            }

            profilePic.setOnClickListener {
                collapseLiveRoom()
                itemClick(userId)
            }

            roomName.setOnClickListener {
                collapseLiveRoom()
                itemClick(userId)
            }

            moderatorName.setOnClickListener {
                collapseLiveRoom()
                itemClick(userId)
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
        addObserver()
    }

    private fun setCurPlayerTimeToTextView(ms: Long) {
        val sec = DateTimeUtils.millisToTime(ms)
        binding.currentTime.text = sec
        Timber.tag("audiotime").d("STARTING TIME IS => ${sec}")
    }
    fun itemClick(userId: String) {
        val nextFrag = ProfileFragment()
        val bundle = Bundle()
        bundle.putString("user", userId) // use as per your need
        bundle.putString("source","RECORD_PLAYER")
        nextFrag.arguments = bundle
        activity?.supportFragmentManager?.beginTransaction()
            ?.replace(R.id.root_view, nextFrag, "findThisFragment")
            //?.addToBackStack(null)
            ?.commit()
    }

}