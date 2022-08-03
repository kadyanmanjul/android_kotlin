package com.joshtalks.badebhaiya.recordedRoomPlayer.listeners

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.paging.compose.collectAsLazyPagingItems
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.feed.FeedViewModel
import com.joshtalks.badebhaiya.profile.FansListFragment
import com.joshtalks.badebhaiya.profile.FansListScreen
import com.joshtalks.badebhaiya.profile.ProfileFragment
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class ListenersListFragment: Fragment() {

    private val viewModel: ListenersViewModel by viewModels()

    private var roomId: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment

        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                activity?.run {
                    Timber.d("back from profile")
                    supportFragmentManager.popBackStack()
                }
            }
        })

        val args = arguments

        roomId = args?.getInt(ROOM_ID)


        return ComposeView(
            requireContext()
        ).apply {
            setContent {
                val listenersList = viewModel.listenersList(roomId!!).collectAsLazyPagingItems()
                ListenersListScreen(
                    listenersList,
                    onItemClick = {
//                        viewModel.openProfile(it.user_id)
                        val nextFrag = ProfileFragment()
                        val bundle = Bundle()
                        bundle.putString("user", it.uuid) // use as per your need
                        bundle.putString("source","LIVE_ROOM")
                        nextFrag.arguments = bundle
                        activity?.supportFragmentManager?.beginTransaction()
                            ?.add(R.id.fragmentContainer, nextFrag, "findThisFragment")
                            //?.addToBackStack(null)
                            ?.commit()
                    },
                    onCloseCall = {
                        dismissFragment() }

                )
            }
        }
    }

    private fun dismissFragment(){
        requireActivity().supportFragmentManager.popBackStack()
    }

    companion object {

        const val ROOM_ID = "room_id"

        fun open(supportFragmentManager: FragmentManager, @IdRes containerId: Int, roomId: Int){

            val fragment = ListenersListFragment()

            val bundle = Bundle()
            bundle.putInt(ROOM_ID, roomId)
            fragment.arguments = bundle

            supportFragmentManager
                .beginTransaction()
                .add(containerId, fragment)
                .addToBackStack(null)
                .commit()
        }
    }
}