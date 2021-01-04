package com.joshtalks.joshskills.ui.translation

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.custom_ui.blurdialog.BlurDialogFragment
import com.joshtalks.joshskills.databinding.LanguageTranslationPopupBinding
import com.joshtalks.joshskills.repository.server.translation.TranslationData
import com.joshtalks.joshskills.ui.groupchat.uikit.ExoAudioPlayer2
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val ARG_WORD = "word"

class LanguageTranslationDialog : BlurDialogFragment() {
    private var word: String? = null
    private lateinit var binding: LanguageTranslationPopupBinding
    private val wordDetailLiveData: MutableLiveData<TranslationData> = MutableLiveData()
    private var exoAudioManager: ExoAudioPlayer2? = ExoAudioPlayer2.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            word = it.getString(ARG_WORD)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.apply {
            val width = AppObjectController.screenWidth * .85
            val height = ViewGroup.LayoutParams.WRAP_CONTENT
            window?.setLayout(width.toInt(), height)
            setCanceledOnTouchOutside(true)
            setCancelable(true)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.language_translation_popup, container, false)
        binding.lifecycleOwner = this
        binding.handler = this
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            attributes.windowAnimations = R.style.DialogAnimation
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        wordDetailLiveData.observe(this, {
            binding.progressBar.visibility = View.GONE
            binding.txtEnglish.text = it.word
            binding.txtHindi.text = it.hinMeaning
            binding.txtPhonetic.text = it.hinTransilteration
            binding.txtMeaning.text = it.engMeaning[0].noun
            binding.group.visibility = View.VISIBLE
        })

        word?.let {
            fetchWordMeaning(it)
        }
    }


    private fun fetchWordMeaning(word: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = AppObjectController.commonNetworkService.getWordDetail(word)
                delay(400)
                response.translationData[0].let {
                    wordDetailLiveData.postValue(it)
                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        exoAudioManager?.release()
    }

    fun speakNormally() {
        wordDetailLiveData.value?.fastPronunciation?.let {
            exoAudioManager?.play(it)
        }
    }

    fun speakSlow() {
        wordDetailLiveData.value?.slowPronunciation?.let {
            exoAudioManager?.play(it, isPlaybackSpeed = true)
        }
    }

    override fun getDownScaleFactor(): Float {
        return 4.0F
    }

    override fun getBlurRadius(): Int {
        return 20
    }

    override fun isRenderScriptEnable(): Boolean {
        return true
    }

    override fun isDebugEnable(): Boolean {
        return true
    }

    override fun isActionBarBlurred(): Boolean {
        return true
    }

    companion object {
        @JvmStatic
        private fun newInstance(word: String) =
            LanguageTranslationDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_WORD, word)
                }
            }

        @JvmStatic
        fun showLanguageDialog(fragmentManager: FragmentManager, word: String) {
            val prev =
                fragmentManager.findFragmentByTag(LanguageTranslationDialog::class.java.name)
            if (prev != null) {
                return
            }
            newInstance(word).show(fragmentManager, LanguageTranslationDialog::class.java.name)
        }
    }
}