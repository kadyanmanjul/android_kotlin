package com.joshtalks.badebhaiya.profile

import android.os.Bundle
import android.transition.Fade
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.ScaleAnimation
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
import kotlinx.android.synthetic.main.activity_feed.*
import timber.log.Timber

class FansListFragment : Fragment() {

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

//        // Inflate the layout for this fragment
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
                                    .remove(this@FansListFragment)
                                    .commitAllowingStateLoss()
                            } else {
//                                supportFragmentManager.popBackStack()
                                supportFragmentManager.beginTransaction()
                                    .remove(this@FansListFragment)
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
                    val peopleList = feedViewModel.fansList.collectAsLazyPagingItems()
                    FansListScreen(
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
        activity?.supportFragmentManager?.beginTransaction()?.remove(this@FansListFragment)
            ?.commitAllowingStateLoss()
//        requireActivity().supportFragmentManager.popBackStack()
    }

    companion object {

        fun open(activity: AppCompatActivity, @IdRes containerId: Int){

            val fragment=FansListFragment()
//                fragment.apply {
//                fragment.exitTransition = MaterialSharedAxis(
//                    MaterialSharedAxis.Z,
//                    /* forward= */ false
//                ).apply {
//                    duration = 500
//                }
//            }
            fragment.exitTransition=Fade(Fade.OUT).apply {
                duration=300
            }

            activity.supportFragmentManager
                .beginTransaction()
                .add(containerId, fragment)
                .addToBackStack(null)
                .commit()
        }
    }
}
