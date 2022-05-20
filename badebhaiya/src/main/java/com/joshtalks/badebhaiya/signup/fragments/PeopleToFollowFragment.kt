package com.joshtalks.badebhaiya.signup.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.theme.JoshBadeBhaiyaTheme

class PeopleToFollowFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return ComposeView(
            requireContext()
        ).apply {
            setContent {
                JoshBadeBhaiyaTheme {
                    PeopleToFollowScreen()
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
fun PeopleToFollowScreen(){
    Column {
        ToolbarHeadingText(
            text = stringResource(id = R.string.badebhaiyas_to_follow)
        )
    }
}

@Composable
fun ToolbarHeadingText(modifier: Modifier = Modifier, text: String){
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth(),
        fontSize = 18.sp,
    )
}