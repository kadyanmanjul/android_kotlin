package com.joshtalks.joshskills.quizgame.ui.main.view.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.databinding.DataBindingUtil
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.FragmentChoiceFragnmentBinding
import com.joshtalks.joshskills.quizgame.StartActivity
import com.joshtalks.joshskills.quizgame.util.AudioManagerQuiz

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        playSound(R.raw.compress_background_util_quiz)
        onBackPress()
    }

    fun playSound(sound:Int){
        if (activity?.application?.let { AudioManagerQuiz.audioRecording.isPlaying() } != true){
            activity?.application?.let { AudioManagerQuiz.audioRecording.startPlaying(it,sound,true) }
        }
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
                FavouritePartnerFragment.newInstance(),"Favourite")
            ?.remove(this)
            ?.commit()
    }

    fun openRandomScreen(){
        val fm = activity?.supportFragmentManager
        fm?.beginTransaction()
            ?.replace(R.id.container,
                RandomPartnerFragment.newInstance(),"Random")
            ?.remove(this)
            ?.commit()
    }

    private fun onBackPress() {
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                moveToNewActivity()
            }
        })
    }

    private fun moveToNewActivity() {
        val i = Intent(activity, StartActivity::class.java)
        startActivity(i)
        (activity as Activity?)?.overridePendingTransition(0, 0)
        requireActivity().finish()
    }
}