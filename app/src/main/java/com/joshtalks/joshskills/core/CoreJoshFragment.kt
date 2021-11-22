package com.joshtalks.joshskills.core

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.joshtalks.joshskills.core.custom_ui.PointSnackbar
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.track.TrackFragment
import timber.log.Timber

open class CoreJoshFragment : TrackFragment() {

    var coreJoshActivity: CoreJoshActivity? = null

    override fun onAttach(context: Context) {
        Timber.tag("VIEWPAGER ISSUE").d("onAttached Called --- ${this::class.java.simpleName}")
        super.onAttach(context)

        if (context is CoreJoshActivity) {
            this.coreJoshActivity = context
        }
    }

    fun showSnackBar(view: View, duration: Int, action_lable: String?) {
        if (PrefManager.getBoolValue(IS_PROFILE_FEATURE_ACTIVE)) {
            // SoundPoolManager.getInstance(AppObjectController.joshApplication).playSnackBarSound()
            PointSnackbar.make(view, duration, action_lable)?.show()
            playSnackbarSound(requireActivity())
        }
    }

    override fun getConversationId(): String? {
        return (requireActivity() as AppCompatActivity).intent.getStringExtra(CONVERSATION_ID)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.tag("VIEWPAGER ISSUE").d("onCreate Called --- ${this::class.java.simpleName}")
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Timber.tag("VIEWPAGER ISSUE").d("onCreateView Called --- ${this::class.java.simpleName}")
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onPause() {
        Timber.tag("VIEWPAGER ISSUE").d("onPause Called --- ${this::class.java.simpleName}")
        super.onPause()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.tag("VIEWPAGER ISSUE").d("onResume Called --- ${this::class.java.simpleName}")
    }

    override fun onResume() {
        Timber.tag("VIEWPAGER ISSUE").d("onResume Called --- ${this::class.java.simpleName}")
        super.onResume()
    }

    override fun onStart() {
        Timber.tag("VIEWPAGER ISSUE").d("onStart Called --- ${this::class.java.simpleName}")
        super.onStart()
    }

    override fun onStop() {
        Timber.tag("VIEWPAGER ISSUE").d("onStop Called --- ${this::class.java.simpleName}")
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Timber.tag("VIEWPAGER ISSUE").d("onDestroy Called --- ${this::class.java.simpleName}")
    }

    override fun onDestroy() {
        Timber.tag("VIEWPAGER ISSUE").d("onDestroy Called --- ${this::class.java.simpleName}")
        super.onDestroy()
    }
}
