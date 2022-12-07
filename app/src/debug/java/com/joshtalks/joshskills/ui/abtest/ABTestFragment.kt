package com.joshtalks.joshskills.ui.abtest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AB_TEST_DATA
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.databinding.FragmentAbTestBinding
import com.joshtalks.joshskills.ui.DebugViewModel

class ABTestFragment : Fragment() {
    private lateinit var binding: FragmentAbTestBinding

    private val viewModel by lazy {
        ViewModelProvider(this)[DebugViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_ab_test, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.initUI()
    }

    private fun FragmentAbTestBinding.initUI() {
        getABTestData().isNotEmpty().let {
            tvActive.text = "- ".plus(getABTestData().filter { it.value }.map { it.key }.joinToString("\n- "))
        }
    }

    private fun getABTestData(): Map<String, Boolean> {
        return try {
            AppObjectController.gsonMapperForLocal.fromJson(
                PrefManager.getStringValue(AB_TEST_DATA),
                object : TypeToken<Map<String, Boolean>>() {}.type
            )
        } catch (e: Exception) {
            emptyMap()
        }
    }
}