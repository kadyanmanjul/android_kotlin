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
import com.joshtalks.badebhaiya.core.EMPTY
import com.joshtalks.badebhaiya.databinding.FragmentSearchBinding
import com.joshtalks.badebhaiya.feed.FeedActivity
import com.joshtalks.badebhaiya.feed.FeedViewModel
import com.joshtalks.badebhaiya.feed.model.SearchRoomsResponseList
import com.joshtalks.badebhaiya.feed.model.Users
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

class SearchFragment : Fragment() {

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
        //binding.recyclerView.layoutManager=LinearLayoutManager(binding.recyclerView.context)
        //binding.recyclerView.adapter=myAdapter
        //searchViewModel.readData.observe

        binding.handler = this
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                //showToast("Back Pressed")
                (activity as FeedActivity).swipeRefreshLayout.isEnabled=true
                activity?.run {
                    supportFragmentManager.beginTransaction().remove(this@SearchFragment)
                        .commitAllowingStateLoss()
                }
            }
        })
        //binding.recyclerView.visibility=GONE
        binding.searchCancel.setOnClickListener{
            (activity as FeedActivity).swipeRefreshLayout.isEnabled=true
            activity?.run {
                supportFragmentManager.beginTransaction().remove(this@SearchFragment)
                    .commitAllowingStateLoss()
            }
        }

        //(activity as FeedActivity?).disableSwipe()
        (activity as FeedActivity).swipeRefreshLayout.isEnabled=false
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var job: Job? = null
        //binding.searchBar.clearFocus()
        binding.searchBar.addTextChangedListener{
            //var job: Job? = null

            if(it.toString()=="")
            {
                binding.defaultText.visibility= VISIBLE
            }
            else {
                binding.defaultText.visibility = GONE
                //binding.recyclerView.visibility= VISIBLE
                job?.cancel()
                //showToast("textChange")
                job = MainScope().launch {
                    delay(500)
                    if (it.toString() != null)
                        viewModel.searchUser(it.toString())
                }
            }
        }

//        binding.searchBar.setOnQueryTextListener(object: androidx.appcompat.widget.SearchView.OnQueryTextListener{
//
//            var users=mutableListOf<Users>()
//
//            override fun onQueryTextSubmit(query: String): Boolean {
//                //showToast("textSubmit")
//                return false
//            }
//
//            override fun onQueryTextChange(p0: String?): Boolean {
//                if(p0=="")
//                {
//                    binding.defaultText.visibility= VISIBLE
//                }
//                else {
//                    binding.defaultText.visibility = GONE
//                    //binding.recyclerView.visibility= VISIBLE
//                    job?.cancel()
//                    //showToast("textChange")
//                    job = MainScope().launch {
//                        delay(500)
//                        if (p0 != null)
//                            viewModel.searchUser(p0)
//                    }
//                }
//                return true
//            }
//        })

//        binding.recyclerView.btnFollow.setOnClickListener{
//            viewModel.updateFollowRequest()
//        }
        addObserver()
    }
    fun addObserver() {
        viewModel.searchResponse.observe(viewLifecycleOwner){
            binding.recyclerView.layoutManager=LinearLayoutManager(requireContext())
            if(it!=null)
            binding.recyclerView.adapter=SearchAdapter(it.users)
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
}