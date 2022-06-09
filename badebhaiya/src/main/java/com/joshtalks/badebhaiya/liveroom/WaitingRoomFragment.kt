package com.joshtalks.badebhaiya.liveroom

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.rememberAsyncImagePainter
import com.joshtalks.badebhaiya.core.IS_NEW_USER
import com.joshtalks.badebhaiya.core.PrefManager
import com.joshtalks.badebhaiya.core.SignUpStepStatus
import com.joshtalks.badebhaiya.feed.FeedViewModel
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.signup.viewmodel.SignUpViewModel
import java.lang.reflect.Modifier
import java.nio.file.Files.size

class WaitingRoomFragment : Fragment() {

    private val feedViewModel by lazy {
        ViewModelProvider(requireActivity()).get(FeedViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return ComposeView(
            requireContext()
        ).apply {
            setContent {
                welcomeToRoom()
            }
        }
    }

    companion object {
        const val TAG = "WaitingFragment"

        fun open(supportFragmentManager: FragmentManager, @IdRes containerId: Int){

            supportFragmentManager
                .beginTransaction()
                .add(containerId, WaitingRoomFragment())
                .addToBackStack(TAG)
                .commit()
        }
    }
}

@Preview
@Composable
fun welcomeToRoom()
{
    Text(text="Welcome  ${ User.getInstance().firstName} ${User.getInstance().lastName}" )
    Image(
        painter = rememberAsyncImagePainter(User.getInstance().profilePicUrl),
        contentDescription = null,
        contentScale = ContentScale.FillWidth,
        modifier=androidx.compose.ui.Modifier
            .size(150.dp).
            clip(RoundedCornerShape(50.dp)).
            blur(30.dp,30.dp)
        )
}

