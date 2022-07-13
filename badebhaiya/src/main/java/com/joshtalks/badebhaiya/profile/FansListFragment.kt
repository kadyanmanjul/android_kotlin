package com.joshtalks.badebhaiya.profile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.paging.compose.collectAsLazyPagingItems

class FansListFragment : Fragment() {

    private val viewModel by lazy {
        ViewModelProvider(requireActivity()).get(ProfileViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        return ComposeView(
            requireContext()
        ).apply {
            setContent {
                    val peopleList = viewModel.fansList.collectAsLazyPagingItems()
                    FansListScreen(
                        peopleList,
                        onItemClick = {
                          viewModel.openProfile(it.user_id!!)
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

        fun open(supportFragmentManager: FragmentManager, @IdRes containerId: Int){

            supportFragmentManager
                .beginTransaction()
                .add(containerId, FansListFragment())
                .addToBackStack(null)
                .commit()
        }
    }
}
