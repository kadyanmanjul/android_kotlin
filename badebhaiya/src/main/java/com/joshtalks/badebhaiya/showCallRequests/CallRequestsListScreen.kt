package com.joshtalks.badebhaiya.showCallRequests

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.showCallRequests.model.RequestData
import com.joshtalks.badebhaiya.showCallRequests.viewModel.RequestsViewModel
import com.joshtalks.badebhaiya.signup.fragments.ListBioText
import com.joshtalks.badebhaiya.signup.fragments.NameText
import com.joshtalks.badebhaiya.signup.fragments.ToolbarHeadingText
import com.joshtalks.badebhaiya.utils.getActivity

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun CallRequestsListScreen(viewModel: RequestsViewModel) {

    val data = viewModel.requestsList.collectAsState()

            Scaffold(
                scaffoldState = rememberScaffoldState(),
                topBar = {
                    CallRequestsToolbar()
                },
                content = {
                    Divider()

                    if (data.value != null){
                        if (data.value!!.request_data.isNullOrEmpty()){
                            EmptyData()
                        } else {
                            CallRequestsList(list = data.value!!.request_data)
                        }
                    } else {
                        JoshProgress()
                    }
                },
                backgroundColor = colorResource(id = R.color.conversation_room_color)
            )

}

@Composable
fun JoshProgress(){
    Box(modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
fun CallRequestsToolbar() {
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

    }
}

@Composable
fun CallRequestsList(list: List<RequestData>) {
    LazyColumn {
        itemsIndexed(list) { index, item ->
            ItemCallRequest(item)
        }
    }
}

@Composable
fun ItemCallRequest(callRequest: RequestData) {
    Column() {
        val activityObj = LocalContext.current.getActivity()
        val didRead = remember {
            mutableStateOf(callRequest.is_read)
        }
        Row(
            modifier = Modifier
                .padding(18.dp)
                .clickable {
                    activityObj?.let { myActivity ->
                        didRead.value = true
                        RequestBottomSheetFragment.open(
                            callRequest.user.user_id,
                            myActivity.supportFragmentManager
                        )
                    }
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfilePicture(
                imageUrl = if (callRequest.user.photo_url.isNullOrEmpty()) "https://upload.wikimedia.org/wikipedia/commons/9/9d/Unknown_Member.jpg" else callRequest.user.photo_url,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.fillMaxWidth(0.75f)
            ) {
                NameText(text = callRequest.user.short_name, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                ListBioText(
                    text = callRequest.request_submitted,
                    textColor = if (didRead.value) colorResource(
                        id = R.color.gray_txt
                    ) else Color.Black,
                    fontSize = 14.sp,
                    fontWeight = if (didRead.value) FontWeight.Normal else FontWeight.Bold
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
                        if (didRead.value) colorResource(
                            id = R.color.gray_txt
                        ) else Color.Black
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = callRequest.submitTime, color = if (didRead.value) colorResource(
                            id = R.color.gray_txt
                            ) else Color.Black,
                    fontSize = 12.sp,
                    fontWeight = if (didRead.value) FontWeight.Normal else FontWeight.Bold
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

@Preview(showBackground = true)
@Composable
fun EmptyData(){
    Box(modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center) {
        Text(
            modifier = Modifier.width(200.dp),
            text = stringResource(R.string.no_call_request),
            fontSize = 16.sp,
            color = colorResource(id = R.color.gray_txt),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
    }
}