package com.joshtalks.badebhaiya.signup.fragments

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemsIndexed
import coil.compose.AsyncImage
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.composeTheme.JoshBadeBhaiyaTheme
import com.joshtalks.badebhaiya.composeTheme.NunitoSansFont
import com.joshtalks.badebhaiya.feed.model.Users
import timber.log.Timber


@Composable
//@Preview(showBackground = true)
fun PeopleToFollowScreen(
    peopleList: LazyPagingItems<Users>,
    onItemClick: (Users) -> Unit = {},
    onNextClick: () -> Unit = {},
    isNextEnabled: State<Boolean>
) {
    JoshBadeBhaiyaTheme {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            val list = peopleList
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = Color.White),
            ) {
                item {
                    ToolbarHeadingText(
                        text = stringResource(id = R.string.badebhaiyas_to_follow)
                    )
                }

                itemsIndexed(list) { index, value ->
                    value?.let {
                        ItemBadeBhaiya(badeBhaiya = it, bottomPadding = 0.dp, onClick = onItemClick)
                    }
                }

            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                MediumButton(
                    modifier = Modifier.padding(vertical = 20.dp),
                    text = stringResource(id = R.string.next),
                    onClick = onNextClick,
                    isNextEnabled
                )
            }
        }
    }
}

fun getPeopleToFollowPadding(index: Int, peopleList: Array<Users>): Dp {
    return if (index == peopleList.lastIndex) {
        Timber.d("Ye last tha => $index")
        80.dp
    } else {
        Timber.d("Ye last nahi tha => $index")
        0.dp
    }
}

@Composable
fun ToolbarHeadingText(
    modifier: Modifier = Modifier,
    text: String,
    textAlign: TextAlign = TextAlign.Center
) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        fontSize = 18.sp,
        textAlign = textAlign,
        fontWeight = FontWeight.Bold,
        fontFamily = NunitoSansFont
    )
}

@Composable
fun ItemBadeBhaiya(
    modifier: Modifier = Modifier,
    badeBhaiya: Users = Users(
        "",
        "sahil",
        "Sahil Khan",
        "https://imageio.forbes.com/specials-images/imageserve/61688aa1d4a8658c3f4d8640/Antonio-Juliano/0x0.jpg?format=jpg&width=960",
        "This is Akhand Swarup’s Bio He's an IES Officer.",
        is_speaker_followed = false
    ),
    bottomPadding: Dp = 0.dp,
    onClick: (Users) -> Unit = {}
) {
    Row(
        modifier = modifier
            .clickable {
                onClick(badeBhaiya)
            }
            .padding(
                horizontal = 18.dp,
                vertical = 12.dp,
            )
            .padding(bottom = bottomPadding)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (badeBhaiya.profilePic != null)
            AsyncImage(
                model = badeBhaiya.profilePic,
                modifier = Modifier
                    .size(62.dp)
                    .clip(RoundedCornerShape(dimensionResource(id = R.dimen._16sdp))),
                contentDescription = "BadeBhaiya Profile Picture",
                contentScale = ContentScale.Crop
            )
        else
            Image(
                painter = painterResource(id = R.drawable.profile_dummy_dp),
                modifier = Modifier
                    .size(62.dp)
                    .clip(RoundedCornerShape(dimensionResource(id = R.dimen._16sdp))),
                contentDescription = "BadeBhaiya Profile Picture",
                contentScale = ContentScale.Crop
            )

        Spacer(modifier = Modifier.size(16.dp))

        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(.8f)
        ) {
            NameText(text = badeBhaiya.full_name ?: "")
            ListBioText(text = badeBhaiya.bio ?: "")
        }
    }
}

@Composable
fun NameText(
    modifier: Modifier = Modifier,
    text: String,
    fontSize: TextUnit = TextUnit.Unspecified
) {
    Text(
        text = text,
        fontWeight = FontWeight.Bold,
        fontSize = fontSize
    )
}

@Composable
fun ListBioText(
    modifier: Modifier = Modifier,
    text: String,
    textColor: Color = Color.Black,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight = FontWeight.Normal
) {
    Text(
        text = text,
        maxLines = 2,
        color = textColor,
        fontSize = fontSize,
        fontWeight = fontWeight
    )
}

@Composable
fun MediumButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit = {},
    isNextEnabled: State<Boolean>
) {
    Button(
        modifier = modifier,
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = colorResource(id = R.color.next_button_enabled),
            disabledBackgroundColor = colorResource(id = R.color.next_button_disabled)
        ),
        shape = RoundedCornerShape(36.dp),
        enabled = isNextEnabled.value
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 60.dp),
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}