package com.joshtalks.joshskills.quizgame.ui.main.view.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.FragmentChoiceFragnmentBinding
import com.joshtalks.joshskills.quizgame.ui.data.model.UserDetails

class ChoiceFragnment : Fragment() {
    private lateinit var binding: FragmentChoiceFragnmentBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_choice_fragnment,
                container,
                false
            )
        binding.lifecycleOwner = this
        binding.clickHandler = this
        return binding.root
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            ChoiceFragnment().apply {
                arguments = Bundle().apply {

                }
            }
    }

    fun openFavouritePartnerScreen(){
        val fm = activity?.supportFragmentManager
        fm?.beginTransaction()
            ?.replace(R.id.container,
                FavouritePartnerFragment.newInstance(),"SearchingOpponentTeam")
            ?.commit()
    }

    fun openRandomScreen(){
        val fm = activity?.supportFragmentManager
        fm?.beginTransaction()
            ?.replace(R.id.container,
                RandomPartnerFragment.newInstance(),"SearchingOpponentTeam")
            ?.commit()
    }
}