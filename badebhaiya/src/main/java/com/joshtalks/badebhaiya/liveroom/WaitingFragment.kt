package com.joshtalks.badebhaiya.liveroom


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.afollestad.materialdialogs.internal.button.DialogActionButton
import com.joshtalks.badebhaiya.composeTheme.JoshBadeBhaiyaTheme
import com.joshtalks.badebhaiya.composeTheme.NunitoSansFont
import com.joshtalks.badebhaiya.feed.model.Waiting
import kotlinx.coroutines.*

class WaitingFragment : Fragment() {
    // TODO: Rename and change types of parameters

    val viewModel by lazy {
        ViewModelProvider(requireActivity()).get(FeedViewModel::class.java)
    }

    lateinit var users: MutableList<Waiting>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel.waitingRoomUsers.value= emptyList()
        viewModel.getWaitingList()
        try {
            (activity as FeedActivity).swipeRefreshLayout.isEnabled = false
        } catch (e: Exception) {
        }
        addObserver()
        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    activity?.run {
                        if (this is FeedActivity) {
                            Timber.d("back from profile and is feed activity")

                            try {
                                (activity as FeedActivity).swipeRefreshLayout.isEnabled = true
                            } catch (e: Exception) {

                            }
                            supportFragmentManager.beginTransaction().remove(this@WaitingFragment)
                                .commitAllowingStateLoss()
                        } else {
                            supportFragmentManager.popBackStack()
                        }
                    }
                }
            })
        return ComposeView(requireContext()).apply {
            setContent {
                JoshBadeBhaiyaTheme {
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
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        addObserver()

    }


    @Composable
    fun ElementList(
        list: List<Waiting>,
    ) {
        Timber.d("ELEMENT COMPOSABLE CREATED")

        Box {
            Column() {
                Image(
                    painter = painterResource(R.drawable.ic_hallway_down_arrow),
                    contentDescription = "downKey",
                    Modifier
                        .size(65.dp)
                        .padding(20.dp),
                )
                Spacer(modifier = Modifier.padding(10.dp))

                Box(
                    Modifier
                        .fillMaxSize()
                        ) {
                        Column(
                            Modifier.clip(
                                RoundedCornerShape(
                                    dimensionResource(id = R.dimen._30sdp),
                                    dimensionResource(id = R.dimen._30sdp),
                                    0.dp,
                                    0.dp
                                )
                            )
                        ) {

                            Text(
                                text = "WAITING ROOM",
                                Modifier
                                    .background(Color.White)
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                fontWeight = FontWeight.Bold,
                                fontFamily =  NunitoSansFont,
                                fontSize = 20.sp
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White)
                                    .padding(15.dp),
                                Alignment.TopCenter
                            ) {
                                LazyVerticalGrid(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .fillMaxSize(),
                                    columns = GridCells.Fixed(4)
                                ) {
                                    Timber.d("INFLATE HONE KA TRY")

                                    items(list)
                                    { item ->
                                        Timber.d("INFLATE HUA")
                                        Element(item)

                                    }
                                }
                            }
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorResource(id = R.color.transparent_white))
                    .padding(horizontal = 20.dp)
                    .clickable(enabled = false) {

                    }
            ) {
                Box(
                    modifier = Modifier
                        .align(
                            alignment = Alignment.Center
                        )
                ) {
                    WaitingRoomDialog()
                }
            }


    }

    private fun exit(){
        requireActivity().onBackPressed()
    }

    @Preview
    @Composable
    fun WaitingRoomDialog(
        modifier: Modifier = Modifier
    ) {
        var buttonVisibility by remember { mutableStateOf(true) }
        Column(
            modifier = modifier
                .clip(RoundedCornerShape(dimensionResource(id = R.dimen._16sdp)))
                .background(color = colorResource(id = R.color.white))
                .padding(dimensionResource(id = R.dimen._20sdp))
        ) {
            Text(
                text = stringResource(id = R.string.waiting_room_dialog_title, viewModel.speakerName),
                fontSize = 16.sp
            )

            AnimatedVisibility(
                visible = buttonVisibility,
                exit = fadeOut(
                    animationSpec = tween(700)
                ) + shrinkVertically(
                    animationSpec = tween(700)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    DialogActionButton(
                        modifier = Modifier
                            .padding(
                                top = dimensionResource(id = R.dimen._20sdp),
                            ),
                        text = stringResource(id = R.string.sure_ill_wait)
                    ) {
                        buttonVisibility = false
                    }
                    DialogActionButton(
                        modifier = Modifier
                            .padding(
                                top = dimensionResource(
                                    id = R.dimen._20sdp
                                ),
                                start = dimensionResource(
                                    id = R.dimen._20sdp
                                )
                            ),
                        text = stringResource(id = R.string.exit_room)
                    ) {
                        exit()
                    }

                }
            }
        }
    }

    @Composable
    fun DialogActionButton(
        modifier: Modifier = Modifier,
        text: String,
        onClick: () -> Unit
    ) {
        Text(
            text = text,
            modifier = modifier.clickable {
                onClick()
            },
            color = colorResource(id = R.color.blue_text_color),
            fontSize = 14.sp
        )
    }

    @Composable
    fun Element(
        item: Waiting
    ) {

             Column(
                 verticalArrangement = Arrangement.Center,
                 modifier = Modifier
                     .background(Color.White)
                     .fillMaxSize()
                     .padding(5.dp),
                 horizontalAlignment = Alignment.CenterHorizontally
             ){
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
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }

    private fun addObserver(){
        viewModel.waitResponse.observe(viewLifecycleOwner){
            Log.i("WAITINGROOM", "adObserver: $it")
            users= viewModel.waitResponse.value as MutableList<Waiting>
            Timber.d("LIST SIZE HAI => ${it.size}")
              lifecycleScope.launch {
                  it.forEach{ element ->
                      delay(1000)
                      Timber.d("ITEM ADDED")
                      val lis = viewModel.waitingRoomUsers.value!!.toMutableList()
                      lis.add(element)
                      viewModel.waitingRoomUsers.postValue(lis)
                  }
              }
        }

        viewModel.waitingRoomUsers.observe(viewLifecycleOwner) {
            Timber.d("WAITING LIST => $it")
        }
    }


    companion object {
        const val TAG = "WaitingFragment"

        fun open(activity: AppCompatActivity) {

                activity.supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, WaitingFragment(), TAG)
                    .commit()
            }
    }
}