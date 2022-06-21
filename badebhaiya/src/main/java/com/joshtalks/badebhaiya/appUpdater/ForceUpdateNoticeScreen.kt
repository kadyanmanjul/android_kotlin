package com.joshtalks.badebhaiya.appUpdater

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
) {
    JoshBadeBhaiyaTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    modifier = Modifier
                        .fillMaxWidth(.7f)
                        .padding(bottom = 20.dp),
                    painter = painterResource(id = R.drawable.bb_app_logo),
                    contentDescription = "bb logo"
                )
                Text(
                    text = stringResource(id = R.string.force_update_notice),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.padding(20.dp))
                BigActionButton(
                    text = stringResource(R.string.update_now),
                    onClick = onDownloadClick
                )

                Spacer(modifier = Modifier.padding(20.dp))

                BigActionButton(
                    text = stringResource(R.string.skip),
                    color = R.color.base_app_color,
                    textColor = colorResource(id = R.color.red),
                    onClick = onExitClick
                )

            }

        }
    }
}

@Composable
fun BigActionButton(
    modifier: Modifier = Modifier,
    text: String,
    color: Int = R.color.green,
    textColor: Color = Color.White,
    onClick: () -> Unit
) {
    Button(
        modifier = modifier
            .fillMaxWidth(),
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = colorResource(id = color)
        ),
        shape = RoundedCornerShape(36.dp),
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 60.dp),
            color = textColor,
            fontWeight = FontWeight.SemiBold
        )
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
