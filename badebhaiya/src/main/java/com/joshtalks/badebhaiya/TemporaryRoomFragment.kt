package com.joshtalks.badebhaiya

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.joshtalks.badebhaiya.databinding.FragmentTemporaryRoomBinding


class TemporaryRoomFragment : Fragment() {

    private lateinit var binding: FragmentTemporaryRoomBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentTemporaryRoomBinding.inflate(inflater, container, false)
        return binding.root
    }


}