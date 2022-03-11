package com.joshtalks.badebhaiya.liveroom.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.databinding.BottomSheetRaisedHandsBinding

class RaisedHandsBottomSheet: DialogFragment() {
    private lateinit var binding: BottomSheetRaisedHandsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        isCancelable = true
        binding = DataBindingUtil.inflate(inflater, R.layout.bottom_sheet_raised_hands, container, false
        )
        binding.lifecycleOwner = this
        binding.fragment = this
        return binding.root
    }
}