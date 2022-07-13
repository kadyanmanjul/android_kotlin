package com.joshtalks.badebhaiya.showCallRequests

import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.signup.fragments.ListBioText
import com.joshtalks.badebhaiya.signup.fragments.NameText
import com.joshtalks.badebhaiya.signup.fragments.ToolbarHeadingText
import com.joshtalks.badebhaiya.utils.getActivity

data class CallRequest(
    val profilePicture: String,
    val firstName: String,
    val latestRequest: String,
    val requestTime: String,
    val didRead: Boolean
)

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
@Preview(showBackground = true)
fun CallRequestsListScreen() {
    val list = List(100) {
        CallRequest(
            profilePicture = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?ixlib=rb-1.2.1&ixid=MnwxMjA3fDB8MHxzZWFyY2h8Mnx8cGVyc29ufGVufDB8fDB8fA%3D%3D&w=1000&q=80",
            firstName = "Sahil Khan",
            latestRequest = "Hi Please mere liye room krdo muje bht kuch sikhna h apse isliye please krdo",
            requestTime = "6:07 PM",
            didRead = true
        )
    }
    Scaffold(
        scaffoldState = rememberScaffoldState(),
        topBar = {
            CallRequestsToolbar()
        },
        content = {
            Divider()
            CallRequestsList(list = list)
        },
        backgroundColor = colorResource(id = R.color.conversation_room_color)
    )
}

@Composable
fun CallRequestsToolbar() {
//    TopAppBar() {
//        Image(
//            painter = painterResource(id = R.drawable.ic_arrow_back),
//            contentDescription = "Back Button",
//            colorFilter = ColorFilter.tint(colorResource(id = R.color.black)),
//        )

//        Row(
//            verticalAlignment = Alignment.CenterVertically
//        ) {
    val activity = LocalContext.current.getActivity()
    Box(
    ) {
        Image(
            modifier = Modifier
                .padding(start = 16.dp)
                .clickable {
                    activity?.onBackPressed()
                }
                .align(Alignment.CenterStart),
            painter = painterResource(id = R.drawable.ic_arrow_back),
            contentDescription = "Back Button",
            colorFilter = ColorFilter.tint(colorResource(id = R.color.black)),
        )
        ToolbarHeadingText(text = stringResource(id = R.string.call_requests).toUpperCase(Locale.current))

//            }
//        }
    }
}

@Composable
fun CallRequestsList(list: List<CallRequest>) {
    LazyColumn {
        itemsIndexed(list) { index, item ->
            ItemCallRequest(item)
        }
    }
}

@Composable
fun ItemCallRequest(callRequest: CallRequest) {
    Column() {
        Row(
            modifier = Modifier
                .padding(18.dp)
                .clickable {
                    // TODO: Open BottomSheet for showing Request.
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfilePicture(
                imageUrl = callRequest.profilePicture
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.fillMaxWidth(0.75f)
            ) {
                NameText(text = callRequest.firstName, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                ListBioText(
                    text = callRequest.latestRequest,
                    textColor = if (callRequest.didRead) colorResource(
                        id = R.color.gray_txt
                    ) else Color.Black,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))

            Column(
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Image(
                    modifier = Modifier.align(Alignment.End),
                    painter = painterResource(id = R.drawable.ic_forward_arrow),
                    contentDescription = "see request",
                    colorFilter = ColorFilter.tint(
                        if (callRequest.didRead) colorResource(
                            id = R.color.gray_txt
                        ) else Color.Black
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = callRequest.requestTime, color = if (callRequest.didRead) colorResource(
                            id = R.color.gray_txt
                            ) else Color.Black,
                    fontSize = 12.sp
                )
            }
        }

        Divider(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .align(Alignment.CenterHorizontally),
        )

    }

}

@Composable
fun ProfilePicture(modifier: Modifier = Modifier, imageUrl: String) {
    AsyncImage(
        model = imageUrl,
        modifier = modifier
            .size(62.dp)
            .clip(RoundedCornerShape(dimensionResource(id = R.dimen._16sdp))),
        contentDescription = "Profile Picture",
        contentScale = ContentScale.Crop
    )
}