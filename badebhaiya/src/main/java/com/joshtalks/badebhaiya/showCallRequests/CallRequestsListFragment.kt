package com.joshtalks.badebhaiya.showCallRequests

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.annotation.IdRes
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.badebhaiya.composeTheme.JoshBadeBhaiyaTheme
import com.joshtalks.badebhaiya.liveroom.viewmodel.LiveRoomViewModel
import com.joshtalks.badebhaiya.profile.ProfileFragment
import com.joshtalks.badebhaiya.showCallRequests.viewModel.RequestsViewModel
import com.joshtalks.badebhaiya.utils.open
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

//@AndroidEntryPoint
class CallRequestsListFragment : Fragment() {

//    private val viewModel: RequestsViewModel by viewModels()

    private val viewModel by lazy {
        ViewModelProvider(this)[RequestsViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                activity?.run {
                    Timber.d("back from profile")
                    supportFragmentManager.popBackStack()
                }
            }
        })
        return ComposeView(
            requireContext()
        ).apply {
            setContent {
                JoshBadeBhaiyaTheme {
                    CallRequestsListScreen(viewModel = viewModel)
                }
            }
        }
    }

    companion object {
        const val TAG = "CallRequestsListFragment"

        fun open(supportFragmentManager: FragmentManager, @IdRes containerId: Int,){
            supportFragmentManager
                .beginTransaction()
                .replace(containerId, CallRequestsListFragment())
                .addToBackStack(TAG)
                .commit()
        }
    }

}