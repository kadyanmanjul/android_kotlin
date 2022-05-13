package com.joshtalks.badebhaiya

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.joshtalks.badebhaiya.core.hideKeyboard
import com.joshtalks.badebhaiya.databinding.FragmentSearchBinding
import com.joshtalks.badebhaiya.feed.Call
import com.joshtalks.badebhaiya.feed.FeedActivity
import com.joshtalks.badebhaiya.feed.FeedViewModel
import com.joshtalks.badebhaiya.feed.model.Users
import com.joshtalks.badebhaiya.profile.ProfileFragment
import kotlinx.android.synthetic.main.activity_feed.*
import kotlinx.android.synthetic.main.activity_sign_up.*
import kotlinx.android.synthetic.main.fragment_search.*
import kotlinx.android.synthetic.main.li_room_event.view.*
import kotlinx.android.synthetic.main.li_search_event.*
import kotlinx.android.synthetic.main.li_search_event.view.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class SearchFragment : Fragment(), Call {

    var users=mutableListOf<Users>()
    val viewModel by lazy {
        ViewModelProvider(requireActivity()).get(FeedViewModel::class.java)
    }

    lateinit var binding:FragmentSearchBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel.searchResponse.value=null
        Log.i("YASHENDRA", "onCreateView: (${viewModel.searchResponse.value})")

        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_search, container, false)

        binding.noresult.visibility= GONE
        binding.handler = this
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                (activity as FeedActivity).swipeRefreshLayout.isEnabled=true
                hide()
                activity?.run {
                    supportFragmentManager.beginTransaction().remove(this@SearchFragment)
                        .commitAllowingStateLoss()
                }
            }
        })
        binding.searchCancel.setOnClickListener{
            (activity as FeedActivity).swipeRefreshLayout.isEnabled=true
            hide()
            activity?.run {
                supportFragmentManager.beginTransaction().remove(this@SearchFragment)
                    .commitAllowingStateLoss()
            }
        }
        (activity as FeedActivity).swipeRefreshLayout.isEnabled=false
        return binding.root
    }

    fun hide()
    {
        hideKeyboard(requireActivity(), binding.searchBar)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var job: Job? = null
        binding.searchBar.addTextChangedListener {
                //var job: Job? = null

            binding.noresult.visibility= GONE
            if(it.toString()=="")
            {
                viewModel.searchResponse.value= null
                binding.defaultText.visibility= VISIBLE
                binding.noresult.visibility= GONE
                binding.recyclerView.visibility=GONE
            }
            else {
                binding.defaultText.visibility = GONE
                job?.cancel()
                job = MainScope().launch {
                    delay(500)
                    if (it.toString() != "" )
                        viewModel.searchUser(it.toString())
                }
//                addObserver()
            }

        }
        addObserver()
    }
    fun addObserver() {
        viewModel.searchResponse.observe(viewLifecycleOwner){
            binding.recyclerView.layoutManager=LinearLayoutManager(requireContext())
            if(it?.users!=null ) {
                binding.recyclerView.visibility= VISIBLE
                binding.recyclerView.adapter = SearchAdapter(it.users, this)
                if(it?.users.size>0) {
                    binding.noresult.visibility = GONE

                }
                else {
                    if(binding.searchBar.toString()=="") {
                        binding.noresult.visibility = GONE
                        binding.defaultText.visibility= VISIBLE
                    }
                    else {
                        binding.noresult.visibility = VISIBLE
                    }
                }
            }
        }
    }
    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment searchFragment.
         */
        // TODO: Rename and change types and number of parameters
        //       @JvmStatic
//        fun newInstance(param1: String, param2: String) =
//            searchFragment().apply {
//                arguments = Bundle().apply {
//                    putString(ARG_PARAM1, param1)
//                    putString(ARG_PARAM2, param2)
//                }
//            }
        @JvmStatic
        fun newInstance()=SearchFragment()
    }

    override fun itemClick(userId:String) {
        val nextFrag = ProfileFragment()
        val bundle = Bundle()
        bundle.putString("user", userId) // use as per your need
        nextFrag.arguments = bundle
        activity?.supportFragmentManager?.beginTransaction()
            ?.replace(R.id.find, nextFrag, "findThisFragment")
            //?.addToBackStack(null)
            ?.commit()
    }
}