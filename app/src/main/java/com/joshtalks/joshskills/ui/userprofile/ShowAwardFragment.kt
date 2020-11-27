package com.joshtalks.joshskills.ui.userprofile

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.text.HtmlCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.databinding.FragmentAwardShowBinding
import com.joshtalks.joshskills.repository.server.Award

class ShowAwardFragment : DialogFragment() {

    private lateinit var binding: FragmentAwardShowBinding
    private var award: Award? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.full_dialog)
        changeDialogConfiguration()
        arguments?.let {
            award = it.getParcelable(AWARD_DETAILS)!!
        }
        if (award == null) {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.run {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            window?.setLayout(width, height)
        }
    }

    private fun changeDialogConfiguration() {
        val params: WindowManager.LayoutParams? = dialog?.window?.attributes
        params?.width = WindowManager.LayoutParams.MATCH_PARENT
        params?.height = WindowManager.LayoutParams.MATCH_PARENT
        params?.gravity = Gravity.CENTER
        dialog?.window?.attributes = params
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_award_show, container, false)
        binding.lifecycleOwner = this
        binding.fragment = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.close.visibility = View.VISIBLE
        award?.imageUrl?.let {
            binding.image.setImage(it)
        }
        binding.text.text = HtmlCompat.fromHtml( award?.awardDescription.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY)

    }

    companion object {
        const val TAG = "ShowAwardFragment"
        const val AWARD_DETAILS = "award_details"
        fun newInstance(award: Award) =
            ShowAwardFragment()
                .apply {
                    arguments = Bundle().apply {
                        putParcelable(AWARD_DETAILS, award)
                    }
                }

        fun showDialog(supportFragmentManager: FragmentManager, award: Award) {
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            val prev = supportFragmentManager.findFragmentByTag(TAG)
            if (prev != null) {
                fragmentTransaction.remove(prev)
            }
            fragmentTransaction.addToBackStack(null)
            newInstance(award)
                .show(supportFragmentManager, TAG)
        }
    }

}
