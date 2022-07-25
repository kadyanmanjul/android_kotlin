package com.joshtalks.badebhaiya.mediaPlayer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.databinding.DataBindingUtil
import coil.compose.AsyncImage
import com.google.firebase.messaging.FirebaseMessagingService
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.composeTheme.NunitoSansFont
import com.joshtalks.badebhaiya.core.EMPTY
import com.joshtalks.badebhaiya.core.NotificationChannelNames
import com.joshtalks.badebhaiya.core.setOnSingleClickListener
import com.joshtalks.badebhaiya.core.showToast
import com.joshtalks.badebhaiya.databinding.FragmentRecordRoomBinding
import com.joshtalks.badebhaiya.feed.FeedViewModel
import com.joshtalks.badebhaiya.feed.model.SpeakerData
import com.joshtalks.badebhaiya.liveroom.LiveRoomState
import com.joshtalks.badebhaiya.notifications.FirebaseNotificationService
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.utils.DEFAULT_NAME
import com.joshtalks.badebhaiya.utils.setUserImageRectOrInitials
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.android.synthetic.main.fragment_record_room.*
import kotlinx.android.synthetic.main.fragment_record_room.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

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

    private var from:String= EMPTY
    private var mediaPlayer : MediaPlayer?=null
    private lateinit var recordingUrl:String

    lateinit var notificationManager: NotificationManager

    private val feedViewModel by lazy {
        ViewModelProvider(requireActivity())[FeedViewModel::class.java]
    }

    private lateinit var viewModel: RecordedRoomViewModel
    lateinit var binding: FragmentRecordRoomBinding

    private val load by lazy {
        CoroutineScope(Dispatchers.IO).launch{
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
            }
            kotlin.runCatching {
                mediaPlayer?.setDataSource(recordingUrl)
                mediaPlayer?.prepare()
            }
        }
    }

    private val notificationChannelId="MediaOne"
    private var notificationChannelName = NotificationChannelNames.DEFAULT.type

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
//        binding= DataBindingUtil.setContentView(requireActivity(), R.layout.fragment_record_room)
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_record_room, container, false)

        viewModel = ViewModelProvider(this).get(RecordedRoomViewModel::class.java)

        var mBundle: Bundle? = Bundle()
        mBundle = this.arguments
        from = mBundle?.getString("source").toString()
        clickListener()
        attachBackPressedDispatcher()
        recordingUrl="https://s3.ap-south-1.amazonaws.com/www.staging.static.joshtalks.com/extra/conversationRooms/3076/e0e79c479247abebc6149f8918b57d02_c3ec44fa-7515-424f-bd02-d49dbaaf816a.m3u8"
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

        load.invokeOnCompletion {
            binding.totalTime.text=convert(mediaPlayer!!.duration)
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

        createChannel()

        binding.seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if(p2 && load.isCompleted){
                    mediaPlayer!!.seekTo(p1)
                }
                seekbar.getThumb().setAlpha(255)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                Log.i("TAG", "onStartTrackingTouch: ")
                seekbar.getThumb().setAlpha(255);
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                Log.i("TAG", "onStopTrackingTouch: ")
                seekbar.getThumb().setAlpha(255);
            }

        })

        val dummy=SpeakerData(123,
            "wvasdbfvbrevfeb",
            "Yami",
            "https://imageio.forbes.com/specials-images/imageserve/61688aa1d4a8658c3f4d8640/Antonio-Juliano/0x0.jpg?format=jpg&width=960",
            "qwerfdsvb",
            "Bade Bhaiya",
            "Bade"
        )
        val throu=MediaData(dummy,"https://s3.ap-south-1.amazonaws.com/www.staging.static.joshtalks.com/extra/conversationRooms/3076/e0e79c479247abebc6149f8918b57d02_c3ec44fa-7515-424f-bd02-d49dbaaf816a.m3u8","wvw","Rooom Name")
        MediaNotification().mediaNotification(activity!!,throu)

        return inflater.inflate(R.layout.fragment_record_room, container, false)
    }

    private fun attachBackPressedDispatcher(){
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            handleBackPress(this)
        }
    }

    private fun handleBackPress(onBackPressedCallback: OnBackPressedCallback) {
        Log.i("KHOLO", "handleBackPress: ${viewModel.lvRoomState.value}")
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
                    mediaPlayer?.pause()
                    mediaPlayer?.release()
                    mediaPlayer=null
                    activity?.supportFragmentManager?.popBackStack()
                    collapseLiveRoom()
//                        finishFragment()
//                        return
                    }
                feedViewModel.isBackPressed.value=false
                }

    }

    private fun createChannel() {
//        TODO("Not yet implemented")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val notificationChannel=NotificationChannel(MediaNotification().CHANNEL_ID, "BadeBHaiyaTrial",NotificationManager.IMPORTANCE_HIGH)
            notificationManager= activity?.getSystemService(NotificationManager::class.java) as NotificationManager
            if(notificationManager!=null){
                notificationManager.createNotificationChannel(notificationChannel)
            }

        }
    }

    override fun onResume() {
        super.onResume()
        if(!load.isCompleted){
            load
        }

    }

    private fun play(){
//         state = false
//        binding.imgPlay.setImageResource(R.drawable.ic_baseline_pause_24)
//        binding.progressBar.visibility = View.VISIBLE // to show loading till the prepare() is ready
        load.invokeOnCompletion {// wait for prepare() to be ready
            mediaPlayer?.start()
//            runOnUiThread { // to update UI on the main thread
//                binding.progressBar.visibility = View.GONE
                binding.seekbar.progress = mediaPlayer?.currentPosition!!
                binding.seekbar.max = mediaPlayer!!.duration
                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed(object : Runnable { // to update the values of seekbar and curr time continually
                    override fun run() {
                        try {
                            binding.seekbar.progress = mediaPlayer!!.currentPosition
                            binding.currentTime.text = convert(binding.seekbar.progress)
                            binding.totalTime.text=convert(mediaPlayer!!.duration)
                            handler.postDelayed(this, 1000)
                            if (mediaPlayer!!.isPlaying){
                                binding.pause.setImageResource(R.drawable.ic_pause_icon)
//                                state = true
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }, 0)
//            }
        }

    }

private fun finishFragment(){
    if (isAdded){
        Timber.d("finishFragment: ")
        requireActivity().supportFragmentManager.popBackStack()
    }
}

    fun convert(duration:Int): String { // to convert mill sec to minutes and seconds
        return String.format("%02d:%02d",TimeUnit.MILLISECONDS.toMinutes(duration.toLong()),
            TimeUnit.MILLISECONDS.toSeconds(duration.toLong()) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration.toLong()))
        )
    }

    fun collapseLiveRoom(){
        binding.liveRoomRootView.transitionToEnd()
//        viewModel.lvRoomState.value = LiveRoomState.COLLAPSED
    }

    fun expandLiveRoom() {
        binding.liveRoomRootView.transitionToStart()
//        viewModel.lvRoomState.value=LiveRoomState.EXPANDED
    }


    private fun trackRecordRoomState(){
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

    fun addObserver(){

        viewModel.lvRoomState.observe(viewLifecycleOwner){
            when(it){
                LiveRoomState.EXPANDED -> expandLiveRoom()
                LiveRoomState.COLLAPSED -> {}
            }
        }

    }


    private fun clickListener() {
        viewModel.lvRoomState.value=LiveRoomState.EXPANDED

        binding.leaveEndRoomBtn.setOnClickListener {
//                expandLiveRoom()
            viewModel.lvRoomState.value=LiveRoomState.EXPANDED
        }
        binding.buttonContainer.setOnClickListener{
            showToast("button container")
//                expandLiveRoom()
            viewModel.lvRoomState.value=LiveRoomState.EXPANDED

        }

        binding.apply {

            downArrow.setOnClickListener{
                collapseLiveRoom()
            }

            pause.setOnClickListener {

                if(mediaPlayer?.isPlaying == true){
                    mediaPlayer?.pause()
                    binding.pause.setImageResource(R.drawable.ic_play)
                }
                else {
                    play()
                }
            }

            backward.setOnClickListener {
                if (load.isCompleted){
                    val newTime = mediaPlayer!!.currentPosition - 15000
                    if (newTime>0){
                        mediaPlayer!!.seekTo(newTime)
                    }else{
                        mediaPlayer!!.seekTo(0)
                    }
                }
            }
            forward.setOnClickListener {
                if (load.isCompleted){
                    val newTime = mediaPlayer!!.currentPosition + 15000
                    if (newTime<mediaPlayer!!.duration){
                        mediaPlayer!!.seekTo(newTime)
                    }
                }
            }

//            buttonContainer.setOnClickListener{
//                showToast("button Container")
//            }




            playbackSpeed.setOnClickListener {
               if(playbackSpeed.text.toString()=="1x")
               {
                   playbackSpeed.text="1.25x"
                   val params=mediaPlayer?.playbackParams
                   params?.speed=1.25f
               }
                else if(playbackSpeed.text.toString()=="1.25x")
                {
                    playbackSpeed.text="1.5x"
                    val params=mediaPlayer?.playbackParams
                    params?.speed=1.5f
                }
                else if(playbackSpeed.text.toString()=="1.5x")
                {
                    playbackSpeed.text="1.75x"
                    val params=mediaPlayer?.playbackParams
                    params?.speed=1.75f
                }
                else if(playbackSpeed.text.toString()=="1.75x")
                {
                    playbackSpeed.text="2x"
                    val params=mediaPlayer?.playbackParams
                    params?.speed=2f
                }
                else
               {
                   playbackSpeed.text="1x"
                   val params=mediaPlayer?.playbackParams
                   params?.speed=1f
               }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.pause()
        mediaPlayer?.release()
        mediaPlayer=null
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        addObserver()
        // TODO: Use the ViewModel
    }

//    override fun onInitDataBinding(viewBinding: FragmentRecordRoomBinding) {
//        TODO("Not yet implemented")
//    }

//    override fun getViewModel(): RecordedRoomViewModel {
//        TODO("Not yet implemented")
//    }

}