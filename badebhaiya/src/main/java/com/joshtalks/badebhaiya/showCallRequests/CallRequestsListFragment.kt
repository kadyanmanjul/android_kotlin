package com.joshtalks.badebhaiya.showCallRequests

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.IdRes
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.joshtalks.badebhaiya.composeTheme.JoshBadeBhaiyaTheme
import com.joshtalks.badebhaiya.profile.ProfileFragment
import com.joshtalks.badebhaiya.utils.open

class CallRequestsListFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return ComposeView(
            requireContext()
        ).apply {
            setContent {
                JoshBadeBhaiyaTheme {
                    CallRequestsListScreen()
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