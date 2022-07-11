package com.joshtalks.badebhaiya.showCallRequests

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.signup.fragments.ListBioText
import com.joshtalks.badebhaiya.signup.fragments.NameText
import com.joshtalks.badebhaiya.signup.fragments.ToolbarHeadingText

data class CallRequest(
    val profilePicture: String,
    val firstName: String,
    val latestRequest: String,
    val requestTime: String
)

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
@Preview(showBackground = true)
fun CallRequestsListScreen() {
    val list = List(100){
        CallRequest(
            profilePicture = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?ixlib=rb-1.2.1&ixid=MnwxMjA3fDB8MHxzZWFyY2h8Mnx8cGVyc29ufGVufDB8fDB8fA%3D%3D&w=1000&q=80",
            firstName = "Sahil Khan",
            latestRequest = "Hi Please mere liye room krdo muje bht kuch sikhna h apse isliye please krdo",
            requestTime = "6:07 PM"
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
        }
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

        Row {
            Image(
                alignment = Alignment.Center,
                painter = painterResource(id = R.drawable.ic_arrow_back),
                contentDescription = "Back Button",
                colorFilter = ColorFilter.tint(colorResource(id = R.color.black)),
            )
            ToolbarHeadingText(text = stringResource(id = R.string.call_requests))
        }
//    }
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
    Row(
        modifier = Modifier.clickable {
            // TODO: Open BottomSheet for showing Request.
        }
    ) {
        ProfilePicture(imageUrl = callRequest.profilePicture)
        Column {
            NameText(text = callRequest.firstName)
            ListBioText(text = callRequest.latestRequest)
        }
        Column {
            Image(painter = painterResource(id = R.drawable.ic_forward_arrow), contentDescription = "see request")
            Text(text = callRequest.requestTime, color = colorResource(id = R.color.gray_txt))
        }
        Divider()
    }
}

@Composable
fun ProfilePicture(imageUrl: String){
    AsyncImage(
        model = imageUrl,
        modifier = Modifier
            .size(62.dp)
            .clip(RoundedCornerShape(dimensionResource(id = R.dimen._16sdp))),
        contentDescription = "Profile Picture",
        contentScale = ContentScale.Crop
    )
}