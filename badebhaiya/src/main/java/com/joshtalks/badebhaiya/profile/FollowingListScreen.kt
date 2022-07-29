package com.joshtalks.badebhaiya.profile.response

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.joshtalks.badebhaiya.feed.model.Users
import com.joshtalks.badebhaiya.profile.SetInitials
import com.joshtalks.badebhaiya.signup.fragments.ListBioText

@Composable
fun FollowingListScreen(
    peopleList: LazyPagingItems<Users>,
    onItemClick: (Users) -> Unit = {},
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
                        text = "FOLLOWING",
                        onClick = onCloseCall
                    )
                }

            itemsIndexed(list) { index, value ->
                value?.let {
                    ItemFans(following = it, bottomPadding = 0.dp, onClick = onItemClick)
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
                    .clickable { onClick() },
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
    following: Users = Users("", "sahil", "Sahil Khan", "https://imageio.forbes.com/specials-images/imageserve/61688aa1d4a8658c3f4d8640/Antonio-Juliano/0x0.jpg?format=jpg&width=960", "This is Akhand Swarup’s Bio He's an IES Officer.", is_speaker_followed = false),
    bottomPadding: Dp = 0.dp,
    onClick: (Users) -> Unit = {}
) {
    Row(
        modifier = modifier
            .clickable {
                onClick(following)
            }
            .padding(
                horizontal = 18.dp,
                vertical = 12.dp,
            )
            .padding(bottom = bottomPadding)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (following.profilePic != null)
            AsyncImage(
                model = following.profilePic,
                modifier = Modifier
                    .size(62.dp)
                    .clip(RoundedCornerShape(dimensionResource(id = R.dimen._20sdp))),
                contentDescription = "Fan Profile Picture",
                contentScale = ContentScale.Crop
            )
        else
            SetInitials(name = following.short_name)

        Spacer(modifier = Modifier.size(16.dp))

        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(.8f)
        ) {
            NameText(text = following.full_name ?: "")
            ListBioText(text = following.bio ?: "")
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
