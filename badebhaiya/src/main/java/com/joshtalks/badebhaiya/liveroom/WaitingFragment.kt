package com.joshtalks.badebhaiya.liveroom


import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.feed.Call
import com.joshtalks.badebhaiya.feed.FeedActivity
import com.joshtalks.badebhaiya.feed.FeedViewModel
import kotlinx.android.synthetic.main.activity_feed.*
import timber.log.Timber
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Card
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.joshtalks.badebhaiya.feed.model.Waiting
import kotlinx.coroutines.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [WaitingFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class WaitingFragment : Fragment(), Call {
    // TODO: Rename and change types of parameters

    val viewModel by lazy {
        ViewModelProvider(requireActivity()).get(FeedViewModel::class.java)
    }

    lateinit var users:MutableList<Waiting>

    @OptIn(InternalCoroutinesApi::class)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        viewModel.getWaitingList()
        try {
            (activity as FeedActivity).swipeRefreshLayout.isEnabled=false
        } catch (e: Exception){
        }
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                activity?.run {
                    if (this is FeedActivity){
                        Timber.d("back from profile and is feed activity")

                        try {
                            (activity as FeedActivity).swipeRefreshLayout.isEnabled=true
                        } catch (e: Exception){

                        }
                        supportFragmentManager.beginTransaction().remove(this@WaitingFragment)
                            .commitAllowingStateLoss()
                    } else  {
                        supportFragmentManager.popBackStack()
                    }
                }
            }
        })

        return ComposeView(requireContext()).apply {
            setContent {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = colorResource(id = R.color.base_app_color)
                ) {
                    val list by viewModel.waitingRoomUsers.observeAsState()

                    list?.let {
                        ElementList(it)
                    }

                }

            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addObserver()

    }


    @Composable
    fun ElementList(
        list: List<Waiting>,
    ) {
        Timber.d("ELEMENT COMPOSABLE CREATED")

            Column() {
                Image(
                    painter = painterResource(R.drawable.ic_hallway_down_arrow),
                    contentDescription = "downKey",
                    Modifier
                        .size(55.dp)
                        .padding(15.dp)
                )
                Box (
                    Modifier
                        .fillMaxSize()
                        .background(Color.White),
                        ) {
                    Column(Modifier.padding(15.dp)) {

                            Text(
                                text = "WAITING ROOM",
                                Modifier
                                    .background(Color.White)
                                    .fillMaxWidth()
                                    .padding(10.dp)
                                    .clip(RoundedCornerShape(20.dp)),
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )

                        Box(
                            modifier = Modifier
                                .fillMaxSize(),
                            Alignment.TopCenter
                        ) {
                            LazyVerticalGrid(
                                modifier = Modifier.align(Alignment.TopCenter),
                                columns = GridCells.Fixed(4)
                            ) {
                                Timber.d("INFLATE HONE KA TRY")

                                items(list)
                                { item ->
                                    Timber.d("INFLATE HUA")
                                    Element(item)
//                        Element()

                                }
                            }
                        }

                    }
                }
            }


    }

 @Composable
    fun Element(
        item: Waiting
    ){

                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .background(Color.White)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(dimensionResource(id = R.dimen._16sdp)))
                        .padding(5.dp)){
                    Timber.d("IMAGE LINK => ${item.profilePic}")
                    if (item.profilePic != null){
                        AsyncImage(
                            model = item.profilePic,
                            modifier = Modifier
                                .size(62.dp)
                                .clip(RoundedCornerShape(dimensionResource(id = R.dimen._16sdp))),
                            contentDescription = "BadeBhaiya Profile Picture",
                            contentScale = ContentScale.Crop
                        )
                    }

                    else{
                        Image(
                            painter = painterResource(id = R.drawable.profile_dummy_dp),
                            modifier = Modifier
                                .size(62.dp)
                                .clip(RoundedCornerShape(dimensionResource(id = R.dimen._16sdp))),
                            contentDescription = "BadeBhaiya Profile Picture",
                            contentScale = ContentScale.Crop
                        )
                    }

                    NameText(text = item.short_name ?:"")

                }

    }

    @Composable
    fun NameText(text: String) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }

    fun addObserver(){
        viewModel.waitResponse.observe(viewLifecycleOwner){
//            binding.audienceList.layoutManager= GridLayoutManager(requireContext(),3)
            Log.i("WAITINGROOM", "adObserver: $it")
            users= viewModel.waitResponse.value as MutableList<Waiting>
            Timber.d("LIST SIZE HAI => ${it.size}")
              lifecycleScope.launch {
                  it.forEach{ element ->
                      delay(500)
                      Timber.d("ITEM ADDED")
                      val lis = viewModel.waitingRoomUsers.value!!.toMutableList()
                      lis.add(element)
                      viewModel.waitingRoomUsers.postValue(lis)
                  }
              }
        }

        viewModel.waitingRoomUsers.observe(viewLifecycleOwner){
            Timber.d("WAITING LIST => $it")
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment WaitingFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            WaitingFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }

            const val TAG = "WaitingFragment"

            fun open(supportFragmentManager: FragmentManager, @IdRes containerId: Int){

                supportFragmentManager
                    .beginTransaction()
                    .add(containerId, WaitingFragment())
                    .addToBackStack(TAG)
                    .commit()
            }
    }

    override fun itemClick(userId: String) {
        TODO("Not yet implemented")
    }
}