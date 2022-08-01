package com.joshtalks.badebhaiya.recordedRoomPlayer.listeners

import com.joshtalks.badebhaiya.recordedRoomPlayer.listeners.model.RecordedRoomListener
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemsIndexed
import coil.compose.AsyncImage
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.composeTheme.JoshBadeBhaiyaTheme
import com.joshtalks.badebhaiya.composeTheme.NunitoFont
import com.joshtalks.badebhaiya.composeTheme.NunitoSansFont
import com.joshtalks.badebhaiya.feed.model.Fans
import com.joshtalks.badebhaiya.recordedRoomPlayer.listeners.model.RecordedRoomListenerItem

@Composable
fun ListenersListScreen(
    peopleList: LazyPagingItems<RecordedRoomListenerItem>,
    onItemClick: (RecordedRoomListenerItem) -> Unit = {},
    onCloseCall: () -> Unit ={}
) {
    JoshBadeBhaiyaTheme {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            val list = peopleList
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = colorResource(id = R.color.base_app_color)),
            ) {
                item {
                    ToolbarHeadingText(
                        text = "LISTENERS",
                        onClick = onCloseCall
                    )
                }

                itemsIndexed(list) { index, value ->
                    value?.let {
                        ItemFans(listener = it, bottomPadding = 0.dp, onClick = onItemClick)
                    }
                }

            }
        }
    }
}

@Composable
fun ToolbarHeadingText(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    text: String,
    textAlign: TextAlign = TextAlign.Center
) {
    Column{
        Spacer(modifier = Modifier.padding(10.dp))
        Row() {
            Spacer(modifier = Modifier.padding(10.dp))
            Image(
                painter = painterResource(id = R.drawable.ic_arrow_back),
                colorFilter = ColorFilter.tint(Color.Black),
                modifier = Modifier
                    .size(29.dp)
                    .clickable {onClick() },
                contentDescription = "Fan Profile Picture",
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.padding(10.dp))
            Text(
                text = text,
                modifier = modifier
                    .fillMaxWidth()
                    .padding(end = 65.dp),
                fontSize = 20.sp,
                textAlign = textAlign,
                fontWeight = FontWeight.Bold,
                fontFamily = NunitoFont
            )
        }
        Spacer(modifier = Modifier.padding(vertical=10.dp))
    }

}

@Composable
fun ItemFans(
    modifier: Modifier = Modifier,
    listener: RecordedRoomListenerItem,
    bottomPadding: Dp = 0.dp,
    onClick: (RecordedRoomListenerItem) -> Unit = {}
) {
    Row(
        modifier = modifier
            .clickable {
                onClick(listener)
            }
            .padding(
                horizontal = 18.dp,
                vertical = 12.dp,
            )
            .padding(bottom = bottomPadding)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (listener.photo_url != null)
            AsyncImage(
                model = listener.photo_url,
                modifier = Modifier
                    .size(62.dp)
                    .clip(RoundedCornerShape(dimensionResource(id = R.dimen._20sdp))),
                contentDescription = "Fan Profile Picture",
                contentScale = ContentScale.Crop
            )
        else
            SetInitials(name = listener.first_name)

        Spacer(modifier = Modifier.size(16.dp))

        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(.8f)
        ) {
            NameText(text = listener.getFullName())
        }
    }
}

@Composable
fun SetInitials(name:String?){
    if(!name.isNullOrEmpty()){
        Card(modifier =Modifier
            .clip(RoundedCornerShape(dimensionResource(id = R.dimen._20sdp)
            ) )){
            Text(text = name[0].toString(),
                textAlign= TextAlign.Center,
                fontSize=35.sp,
                color= colorResource(id = R.color.white),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .size(62.dp)
                    .background(colorResource(id = R.color.button_color))
                    .padding(top = 5.dp)
            )
        }
    }
}

@Composable
fun NameText( text: String) {
    Text(
        text = text,
        fontWeight = FontWeight.Bold,
    )
}
