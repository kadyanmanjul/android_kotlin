package com.joshtalks.badebhaiya.appUpdater

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afollestad.materialdialogs.internal.button.DialogActionButton
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.composeTheme.JoshBadeBhaiyaTheme

@Preview(showBackground = true)
@Composable
fun ForceUpdateNoticeScreen(
    onDownloadClick: () -> Unit = {},
    onExitClick: () -> Unit = {}
){
    JoshBadeBhaiyaTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ){
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    modifier = Modifier
                        .fillMaxWidth(.4f)
                        .padding(bottom = 20.dp),
                    painter = painterResource(id = R.drawable.bb_app_logo),
                    contentDescription = "bb logo")
                Text(text = stringResource(id = R.string.force_update_notice))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    DialogActionText(
                        modifier = Modifier
                            .padding(
                                top = dimensionResource(id = R.dimen._20sdp),
                            ),
                        text = stringResource(R.string.download_now)
                    ) {
                    }
                    DialogActionText(
                        modifier = Modifier
                            .padding(
                                top = dimensionResource(
                                    id = R.dimen._20sdp
                                ),
                                start = dimensionResource(
                                    id = R.dimen._20sdp
                                )
                            ),
                        text = stringResource(R.string.exit)
                    ) {

                    }

                }

            }

        }
    }
}

@Composable
fun DialogActionText(
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
        fontSize = 16.sp
    )
}
