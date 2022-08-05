package com.joshtalks.badebhaiya.profile

import android.os.Bundle
import android.transition.Fade
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.paging.compose.collectAsLazyPagingItems
import com.google.android.material.transition.MaterialSharedAxis
import com.joshtalks.badebhaiya.feed.FeedActivity
import com.joshtalks.badebhaiya.feed.FeedViewModel
import com.joshtalks.badebhaiya.profile.response.FollowingListScreen
import com.joshtalks.badebhaiya.signup.fragments.PeopleToFollowScreen
import kotlinx.android.synthetic.main.activity_feed.*
import timber.log.Timber
import java.lang.Exception

class FollowingListFragment : Fragment() {

    private val viewModel by lazy {
        ViewModelProvider(requireActivity()).get(ProfileViewModel::class.java)
    }

    private val feedViewModel by lazy {
        ViewModelProvider(requireActivity()).get(FeedViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {

                    activity?.run {
                        if (this is FeedActivity) {
                            Timber.d("back from profile and is feed activity")

                            try {
                                (activity as FeedActivity).swipeRefreshLayout.isEnabled = true
                            } catch (e: Exception) {

                            }
                            supportFragmentManager.beginTransaction()
                                .remove(this@FollowingListFragment)
                                .commitAllowingStateLoss()
                        } else {
//                                supportFragmentManager.popBackStack()
                            supportFragmentManager.beginTransaction()
                                .remove(this@FollowingListFragment)
                                .commitAllowingStateLoss()
                        }
                    }
                }
            })
        return ComposeView(
            requireContext()
        ).apply {
            isTransitionGroup=true
            setContent {
                    val peopleList = feedViewModel.followingList.collectAsLazyPagingItems()
                FollowingListScreen(
                        peopleList,
                        onItemClick = {
                          viewModel.openProfile(it.user_id)
                        },
                        onCloseCall = {
                            dismissFragment() }

                    )
            }
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

    private fun dismissFragment(){
//        requireActivity().supportFragmentManager.popBackStack()
        activity?.supportFragmentManager?.beginTransaction()?.remove(this@FollowingListFragment)
            ?.commitAllowingStateLoss()
    }

    companion object {

        fun open(activity: AppCompatActivity, @IdRes containerId: Int){

            val fragment=FollowingListFragment()
            fragment.exitTransition=Fade(Fade.OUT).apply {
                duration=300
            }
//            fragment.exitTransition=MaterialSharedAxis(
//                MaterialSharedAxis.Z,
//                /* forward= */ false
//            ).apply {
//                duration = 500
//            }

            activity
                .supportFragmentManager
                .beginTransaction()
                .add(containerId, fragment)
                .addToBackStack(null)
                .commit()
        }
    }
}