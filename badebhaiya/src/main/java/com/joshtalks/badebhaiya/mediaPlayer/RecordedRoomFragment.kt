package com.joshtalks.badebhaiya.mediaPlayer

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
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
import com.joshtalks.badebhaiya.feed.model.SpeakerData
import com.joshtalks.badebhaiya.liveroom.LiveRoomState
import com.joshtalks.badebhaiya.recordedRoomPlayer.isPlaying
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.utils.DEFAULT_NAME
import com.joshtalks.badebhaiya.utils.datetimeutils.DateTimeUtils
import com.joshtalks.badebhaiya.utils.setUserImageRectOrInitials
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class RecordedRoomFragment : Fragment() {

    companion object {
        fun newInstance() = RecordedRoomFragment()
        fun open(activity: AppCompatActivity, from: String) {
            val fragment = RecordedRoomFragment() // replace your custom fragment class
            val bundle = Bundle()
            bundle.putString("source", from) // use as per your need
            fragment.arguments = bundle

            activity
                .supportFragmentManager
                .beginTransaction()
                .add(R.id.feedRoot, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    //    private var mediaPlayer : MediaPlayer?=null
    private var from: String = EMPTY
//    private var mediaPlayer: MediaPlayer? = null
    private lateinit var recordingUrl: String
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
//        binding= DataBindingUtil.setContentView(requireActivity(), R.layout.fragment_record_room)
        binding = FragmentRecordRoomBinding.inflate(inflater, container, false)
        binding.recordedViewModel = viewModel
//        viewModel = ViewModelProvider(this).get(RecordedRoomViewModel::class.java)

        var mBundle: Bundle? = Bundle()
        mBundle = this.arguments
        from = mBundle?.getString("source").toString()
        clickListener()
        attachBackPressedDispatcher()
        recordingUrl =
            "https://s3.ap-south-1.amazonaws.com/www.staging.static.joshtalks.com/extra/conversationRooms/3076/e0e79c479247abebc6149f8918b57d02_c3ec44fa-7515-424f-bd02-d49dbaaf816a.m3u8"
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
                Log.i("TAG", "onStartTrackingTouch: ")
                binding.seekbar.thumb.alpha = 255
                shouldUpdateSeekbar = false

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                Log.i("TAG", "onStopTrackingTouch: ")
                p0?.let {
                    binding.seekbar.thumb.alpha = 255
                    viewModel.seekTo(it.progress.toLong())
                    shouldUpdateSeekbar = true
                }
            }

        })

        val dummy = SpeakerData(
            123,
            "wvasdbfvbrevfeb",
            "Yami",
            "https://imageio.forbes.com/specials-images/imageserve/61688aa1d4a8658c3f4d8640/Antonio-Juliano/0x0.jpg?format=jpg&width=960",
            "qwerfdsvb",
            "Bade Bhaiya",
            "Bade"
        )
        val throu = MediaData(
            dummy,
            "https://s3.ap-south-1.amazonaws.com/www.staging.static.joshtalks.com/extra/conversationRooms/3076/e0e79c479247abebc6149f8918b57d02_c3ec44fa-7515-424f-bd02-d49dbaaf816a.m3u8",
            "wvw",
            "Rooom Name"
        )
//        MediaNotification().mediaNotification(requireActivity(),throu)

        return binding.root
    }

    private fun attachBackPressedDispatcher() {
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            handleBackPress(this)
        }
    }

    private fun handleBackPress(onBackPressedCallback: OnBackPressedCallback) {
        Log.i("KHOLO", "handleBackPress: ${viewModel.lvRoomState.value}")
        if (viewModel.lvRoomState.value == LiveRoomState.EXPANDED) {
            // Minimise live room.
            collapseLiveRoom()
        } else {
            // Live is already minimized ask if user wants to quit live room.
            if (from == "Profile") {
                feedViewModel.isBackPressed.value = true
                from = "None"
            } else {
                activity?.supportFragmentManager?.popBackStack()
                collapseLiveRoom()
//                        finishFragment()
//                        return
            }
            feedViewModel.isBackPressed.value = false
        }

    }


    private fun finishFragment() {
        if (isAdded) {
            Timber.d("finishFragment: ")
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    fun convert(duration: Int): String { // to convert mill sec to minutes and seconds
        return String.format(
            "%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(duration.toLong()),
            TimeUnit.MILLISECONDS.toSeconds(duration.toLong()) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration.toLong()))
        )
    }

    fun collapseLiveRoom() {
        binding.liveRoomRootView.transitionToEnd()
//        viewModel.lvRoomState.value = LiveRoomState.COLLAPSED
    }

    fun expandLiveRoom() {
        binding.liveRoomRootView.transitionToStart()
//        viewModel.lvRoomState.value=LiveRoomState.EXPANDED
    }


    private fun trackRecordRoomState() {
//        binding.liveRoomRootView.addTransitionListener(object : MotionLayout.TransitionListener{
//            override fun onTransitionStarted(
//                motionLayout: MotionLayout?,
//                startId: Int,
//                endId: Int
//            ) {
//            }
//
//            override fun onTransitionChange(
//                motionLayout: MotionLayout?,
//                startId: Int,
//                endId: Int,
//                progress: Float
//            ) {
//            }
//
//            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
//                if (currentId == R.id.collapsed){
//                    viewModel.lvRoomState.value = LiveRoomState.COLLAPSED
//                } else {
//                    viewModel.lvRoomState.value = LiveRoomState.EXPANDED
//                }
//            }
//
//            override fun onTransitionTrigger(
//                motionLayout: MotionLayout?,
//                triggerId: Int,
//                positive: Boolean,
//                progress: Float
//            ) {
//            }
//
//        })
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
        viewModel.lvRoomState.value = LiveRoomState.EXPANDED

        binding.leaveEndRoomBtn.setOnClickListener {
//                expandLiveRoom()
            viewModel.lvRoomState.value = LiveRoomState.EXPANDED
        }
        binding.buttonContainer.setOnClickListener {
//                expandLiveRoom()
            viewModel.lvRoomState.value = LiveRoomState.EXPANDED

        }

        binding.apply {

            downArrow.setOnClickListener {
                collapseLiveRoom()
            }

            pause.setOnClickListener {

                viewModel.playOrToggleSong()
            }

            playbackSpeed.setOnClickListener {
                when {
                    playbackSpeed.text.toString()=="1x" -> {
                        playbackSpeed.text="1.25x"
                        viewModel.increaseSpeed(1.25f)
                    }
                    playbackSpeed.text.toString()=="1.25x" -> {
                        playbackSpeed.text="1.5x"
                        viewModel.increaseSpeed(1.5f)
                    }
                    playbackSpeed.text.toString()=="1.5x" -> {
                        playbackSpeed.text="1.75x"
                        viewModel.increaseSpeed(1.75f)
                    }
                    playbackSpeed.text.toString()=="1.75x" -> {
                        playbackSpeed.text="2x"
                        viewModel.increaseSpeed(2f)
                    }
                    else -> {
                        playbackSpeed.text="1x"
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

}