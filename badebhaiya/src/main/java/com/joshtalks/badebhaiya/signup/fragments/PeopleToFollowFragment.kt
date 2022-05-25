package com.joshtalks.badebhaiya.signup.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.paging.compose.collectAsLazyPagingItems
import com.joshtalks.badebhaiya.core.IS_NEW_USER
import com.joshtalks.badebhaiya.core.PrefManager
import com.joshtalks.badebhaiya.core.SignUpStepStatus
import com.joshtalks.badebhaiya.signup.viewmodel.SignUpViewModel

class PeopleToFollowFragment : Fragment() {

    private val viewModel by lazy {
        ViewModelProvider(requireActivity()).get(SignUpViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return ComposeView(
            requireContext()
        ).apply {
            setContent {
                    val peopleList = viewModel.bbToFollow.collectAsLazyPagingItems()
                    val isNextEnabled = viewModel.isNextEnabled.observeAsState(initial = false)
                    PeopleToFollowScreen(
                        peopleList,
                        onItemClick = {
                          viewModel.openProfile(it.user_id)
                        },
                        onNextClick = {
                            PrefManager.put(IS_NEW_USER, value = false)
                            viewModel.signUpStatus.postValue(SignUpStepStatus.ProfileCompleted)
                        },
                        isNextEnabled
                    )
            }
        }
    }

    companion object {
        const val TAG = "PeopleToFollowFragment"

        fun open(supportFragmentManager: FragmentManager, @IdRes containerId: Int){

            supportFragmentManager
                .beginTransaction()
                .add(containerId, PeopleToFollowFragment())
                .addToBackStack(TAG)
                .commit()
        }
    }
}
