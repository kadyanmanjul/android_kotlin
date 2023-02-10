package com.joshtalks.joshskills.premium.ui.voip.new_arch.ui.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.base.BaseFragment
import com.joshtalks.joshskills.premium.core.AppObjectController
import com.joshtalks.joshskills.premium.core.CURRENT_COURSE_ID
import com.joshtalks.joshskills.premium.core.FirebaseRemoteConfigKey.Companion.SEARCHING_SCREEN_RULES
import com.joshtalks.joshskills.premium.core.FirebaseRemoteConfigKey.Companion.SEARCHING_SCREEN_RULES_DEFAULT
import com.joshtalks.joshskills.premium.core.FirebaseRemoteConfigKey.Companion.SEARCHING_SCREEN_TIPS
import com.joshtalks.joshskills.premium.databinding.FragmentSearchingUserBinding
import com.joshtalks.joshskills.premium.ui.voip.new_arch.ui.models.SearchingRule
import com.joshtalks.joshskills.premium.ui.voip.new_arch.ui.models.SearchingTip
import com.joshtalks.joshskills.premium.ui.voip.new_arch.ui.viewmodels.VoiceCallViewModel
import com.joshtalks.joshskills.premium.ui.voip.new_arch.ui.views.adapter.SearchUserAdapter
import com.joshtalks.joshskills.premium.calling.constant.State
import com.joshtalks.joshskills.premium.calling.data.local.PrefManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.reflect.Type
import kotlin.random.Random
import com.joshtalks.joshskills.premium.core.PrefManager as CorePrefManager

const val SEARCHING_SCREEN_COUNT = "SEARCHING_SCREEN_COUNT"

class SearchingUserFragment : BaseFragment() {

    lateinit var searchingUserBinding: FragmentSearchingUserBinding
    private val gson = Gson()

    val voiceCallViewModel by lazy {
        ViewModelProvider(requireActivity())[VoiceCallViewModel::class.java]
    }

    val vm by lazy {
        ViewModelProvider(requireActivity())[VoiceCallViewModel::class.java]
    }

    val adapter by lazy {
        SearchUserAdapter(context = requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        searchingUserBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_searching_user, container, false)
        searchingUserBinding.listOfReason.adapter = adapter
        searchingUserBinding.listOfReason.layoutManager = LinearLayoutManager(requireContext())
        return searchingUserBinding.root
    }

    override fun initViewBinding() {
        searchingUserBinding.vm = vm
        searchingUserBinding.executePendingBindings()
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            withContext(Dispatchers.IO) {
                try {
                    val count = CorePrefManager.getIntValue(SEARCHING_SCREEN_COUNT, true, 0)
                    when (count) {
                        0 -> {
                            val json = AppObjectController.getFirebaseRemoteConfig().getString(
                                SEARCHING_SCREEN_TIPS
                            )
                            val listType: Type =
                                object : TypeToken<List<SearchingTip?>?>() {}.type
                            val tips: List<SearchingTip> = gson.fromJson(json, listType)
                            withContext(Dispatchers.Main) {
                                searchingUserBinding.imageViewPh.text = tips.first().title
                                adapter.items = tips.first().items
                                adapter.notifyDataSetChanged()
                                CorePrefManager.put(SEARCHING_SCREEN_COUNT, 1, true)
                                searchingUserBinding.executePendingBindings()
                            }
                        }
                        1 -> {
                            val json = AppObjectController.getFirebaseRemoteConfig()
                                .getString(SEARCHING_SCREEN_TIPS)
                            val listType: Type =
                                object : TypeToken<List<SearchingTip?>?>() {}.type
                            val tips: List<SearchingTip> = gson.fromJson(json, listType)
                            withContext(Dispatchers.Main) {
                                searchingUserBinding.imageViewPh.text = tips.last().title
                                searchingUserBinding.listOfReason.adapter = SearchUserAdapter(
                                    tips.last().items,
                                    this@SearchingUserFragment.requireContext()
                                )
                                CorePrefManager.put(SEARCHING_SCREEN_COUNT, 2, true)
                                searchingUserBinding.executePendingBindings()
                            }
                        }
                        2 -> {
                            val json = AppObjectController.getFirebaseRemoteConfig()
                                .getString(
                                    "$SEARCHING_SCREEN_RULES${
                                        CorePrefManager.getStringValue(
                                            com.joshtalks.joshskills.premium.core.CURRENT_COURSE_ID
                                        )
                                    }"
                                ).ifBlank {
                                    AppObjectController.getFirebaseRemoteConfig()
                                        .getString(SEARCHING_SCREEN_RULES_DEFAULT)
                                }
                            val listType: Type =
                                object : TypeToken<List<SearchingRule?>?>() {}.type
                            val rules: List<SearchingRule> = gson.fromJson(json, listType)
                            withContext(Dispatchers.Main) {
                                searchingUserBinding.imageViewPh.text = rules.first().title
                                searchingUserBinding.listOfReason.adapter = SearchUserAdapter(
                                    rules.first().items,
                                    this@SearchingUserFragment.requireContext()
                                )
                                CorePrefManager.put(SEARCHING_SCREEN_COUNT, 3, true)
                                searchingUserBinding.executePendingBindings()
                            }
                        }
                        3 -> {
                            val json = AppObjectController.getFirebaseRemoteConfig()
                                .getString(SEARCHING_SCREEN_TIPS)
                            val listType: Type =
                                object : TypeToken<List<SearchingTip?>?>() {}.type
                            val tips: List<SearchingTip> = gson.fromJson(json, listType)
                            val randomIndex = Random.Default.nextInt(0, tips.size)
                            withContext(Dispatchers.Main) {
                                searchingUserBinding.imageViewPh.text = tips[randomIndex].title
                                searchingUserBinding.listOfReason.adapter = SearchUserAdapter(
                                    tips[randomIndex].items,
                                    this@SearchingUserFragment.requireContext()
                                )
                                CorePrefManager.put(SEARCHING_SCREEN_COUNT, 4, true)
                                searchingUserBinding.executePendingBindings()
                            }
                        }
                        4 -> {
                            val json = AppObjectController.getFirebaseRemoteConfig()
                                .getString(
                                    "$SEARCHING_SCREEN_RULES${
                                        CorePrefManager.getStringValue(
                                            com.joshtalks.joshskills.premium.core.CURRENT_COURSE_ID
                                        )
                                    }"
                                ).ifBlank {
                                    AppObjectController.getFirebaseRemoteConfig()
                                        .getString(SEARCHING_SCREEN_RULES_DEFAULT)
                                }
                            val listType: Type = object : TypeToken<List<SearchingRule?>?>() {}.type
                            val rules: List<SearchingRule> = gson.fromJson(json, listType)
                            val randomIndex = Random.Default.nextInt(0, rules.size)
                            withContext(Dispatchers.Main) {
                                searchingUserBinding.imageViewPh.text = rules[randomIndex].title
                                searchingUserBinding.listOfReason.adapter = SearchUserAdapter(
                                    rules[randomIndex].items,
                                    this@SearchingUserFragment.requireContext()
                                )
                                CorePrefManager.put(SEARCHING_SCREEN_COUNT, 3, true)
                                searchingUserBinding.executePendingBindings()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun initViewState() {}

    override fun setArguments() {}

    override fun onResume() {
        super.onResume()
        setCurrentCallState()
    }

    private fun replaceCallUserFragment() {
        requireActivity().supportFragmentManager.commit {
            replace(R.id.voice_call_container, CallFragment(), "CallFragment")
        }
    }

    private fun setCurrentCallState() {
        if ((PrefManager.getVoipState() == com.joshtalks.joshskills.premium.calling.constant.State.JOINED || PrefManager.getVoipState() == com.joshtalks.joshskills.premium.calling.constant.State.CONNECTED)) {
            replaceCallUserFragment()
        }
    }


}
