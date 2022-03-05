package com.joshtalks.joshskills.ui.special_practice

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.FileDataSource
import com.google.android.exoplayer2.upstream.FileDataSource.FileDataSourceException
import com.joshtalks.joshskills.core.CoreJoshFragment
import com.joshtalks.joshskills.databinding.FragmentViewShareVideoBinding


class ViewAndShareVideoFragment : CoreJoshFragment(), Player.EventListener {
    private lateinit var binding: FragmentViewShareVideoBinding
    private lateinit var videoPath: String
    private var exoPlayer: SimpleExoPlayer? = null


    companion object {
        fun newInstance(videoPath: String): ViewAndShareVideoFragment {
            val args = Bundle()
            args.putString("videoPath", videoPath)
            val fragment = ViewAndShareVideoFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentViewShareVideoBinding.inflate(inflater, container, false)
        arguments?.getString("videoPath")?.let {
            videoPath = it
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initPlayer(Uri.parse(videoPath))
    }

    private fun initPlayer(uri: Uri) {
        exoPlayer = ExoPlayerFactory.newSimpleInstance(
            requireContext(),
            DefaultTrackSelector(),
            DefaultLoadControl()
        )
        exoPlayer!!.addListener(this)
        val dataSpec = DataSpec(uri)
        val fileDataSource = FileDataSource()
        try {
            fileDataSource.open(dataSpec)
        } catch (e: FileDataSourceException) {
            e.printStackTrace()
        }
        val factory: DataSource.Factory = DataSource.Factory { fileDataSource }
        val audioSource: MediaSource = ExtractorMediaSource(
            fileDataSource.uri,
            factory, DefaultExtractorsFactory(), null, null
        )
        exoPlayer?.let {
            it.prepare(audioSource)
            it.playWhenReady = true
            it.repeatMode = Player.REPEAT_MODE_ONE
        }
    }
}