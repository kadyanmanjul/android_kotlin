package com.joshtalks.badebhaiya

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.content.res.AppCompatResources
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.joshtalks.badebhaiya.databinding.FragmentSearchBinding
import com.joshtalks.badebhaiya.feed.FeedViewModel
import com.joshtalks.badebhaiya.feed.model.Users
import com.joshtalks.badebhaiya.profile.ProfileViewModel
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

    val viewModel by lazy {
        ViewModelProvider(requireActivity()).get(FeedViewModel::class.java)
    }

    private val profileViewModel by lazy {
        ViewModelProvider(this)[ProfileViewModel::class.java]
    }

    lateinit var binding:FragmentSearchBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_search, container, false)
        binding.handler = this
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                //showToast("Back Pressed")
                activity?.run {
                    supportFragmentManager.beginTransaction().remove(this@SearchFragment)
                        .commitAllowingStateLoss()
                }
            }
        })
        binding.searchCancel.setOnClickListener{
            val manager = requireActivity().supportFragmentManager
            manager.beginTransaction().remove(this).commit()
        }
        //(activity as FeedActivity).swipeRefreshLayout.visibility=View.GONE
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var job: Job? = null
        var users=mutableListOf<Users>()

        search()

//        binding.searchBar.addTextChangedListener{
//            //var job: Job? = null
//
//                job?.cancel()
//                job = MainScope().launch {
//                    delay(500)
//                    if (it.toString().isNotEmpty())
//                        users=viewModel.searchUser(it.toString())
//                }
//            binding.recyclerView.adapter=SearchAdapter(users)
//            binding.recyclerView.layoutManager=LinearLayoutManager(requireContext())
//        }

//        binding.searchBar.addTextChangedListener( object: TextWatcher {
//            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
//            }
//            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
//                job?.cancel()
//                job = MainScope().launch {
//                    delay(500)
//                    if (binding.searchBar.toString().isNotEmpty())
//                        users=viewModel.searchUser(binding.searchBar.text.toString())
//                    //showToast("${binding.searchBar.text}")
//                }
//            }
//            override fun afterTextChanged(p0: Editable?) {
//                binding.recyclerView.adapter=SearchAdapter(users)
//                binding.recyclerView.layoutManager=LinearLayoutManager(requireContext())
//            }
//        })
    }


    fun search(){
        binding.searchBar.addTextChangedListener( object: TextWatcher {
            var job: Job? = null
            var users=mutableListOf<Users>()
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                job?.cancel()
                job = MainScope().launch {
                    delay(500)
                    if (binding.searchBar.toString().isNotEmpty())
                        users=viewModel.searchUser(binding.searchBar.text.toString())
                    //showToast("${binding.searchBar.text}")
                }
            }
            override fun afterTextChanged(p0: Editable?) {
                binding.recyclerView.adapter=SearchAdapter(users)
                binding.recyclerView.layoutManager=LinearLayoutManager(requireContext())
            }
        })
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