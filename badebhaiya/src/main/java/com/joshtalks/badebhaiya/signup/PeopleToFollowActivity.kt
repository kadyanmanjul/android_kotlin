package com.joshtalks.badebhaiya.signup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModelProvider
import androidx.paging.compose.collectAsLazyPagingItems
import com.joshtalks.badebhaiya.composeTheme.JoshBadeBhaiyaTheme
import com.joshtalks.badebhaiya.feed.model.Users
import com.joshtalks.badebhaiya.signup.fragments.PeopleToFollowScreen
import com.joshtalks.badebhaiya.signup.viewmodel.SignUpViewModel

class PeopleToFollowActivity : ComponentActivity() {

    private val viewModel by lazy {
        ViewModelProvider(this).get(SignUpViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val peopleListLocal = arrayOf(
            Users("", "sahil", "Sahil Khan", "https://imageio.forbes.com/specials-images/imageserve/61688aa1d4a8658c3f4d8640/Antonio-Juliano/0x0.jpg?format=jpg&width=960", "This is Akhand Swarup’s Bio He's an IES Officer.", is_speaker_followed = false),
            Users("", "sahil", "Sahil Khan", "https://imageio.forbes.com/specials-images/imageserve/61688aa1d4a8658c3f4d8640/Antonio-Juliano/0x0.jpg?format=jpg&width=960", "This is Akhand Swarup’s Bio He's an IES Officer.", is_speaker_followed = false),
            Users("", "sahil", "Sahil Khan", "https://imageio.forbes.com/specials-images/imageserve/61688aa1d4a8658c3f4d8640/Antonio-Juliano/0x0.jpg?format=jpg&width=960", "This is Akhand Swarup’s Bio He's an IES Officer.", is_speaker_followed = false),
            Users("", "sahil", "Sahil Khan", "https://imageio.forbes.com/specials-images/imageserve/61688aa1d4a8658c3f4d8640/Antonio-Juliano/0x0.jpg?format=jpg&width=960", "This is Akhand Swarup’s Bio He's an IES Officer.", is_speaker_followed = false),
            Users("", "sahil", "Sahil Khan", "https://imageio.forbes.com/specials-images/imageserve/61688aa1d4a8658c3f4d8640/Antonio-Juliano/0x0.jpg?format=jpg&width=960", "This is Akhand Swarup’s Bio He's an IES Officer.", is_speaker_followed = false),
            Users("", "sahil", "Sahil Khan", "https://imageio.forbes.com/specials-images/imageserve/61688aa1d4a8658c3f4d8640/Antonio-Juliano/0x0.jpg?format=jpg&width=960", "This is Akhand Swarup’s Bio He's an IES Officer.", is_speaker_followed = false),
            Users("", "sahil", "Sahil Khan", "https://imageio.forbes.com/specials-images/imageserve/61688aa1d4a8658c3f4d8640/Antonio-Juliano/0x0.jpg?format=jpg&width=960", "This is Akhand Swarup’s Bio He's an IES Officer.", is_speaker_followed = false),
            Users("", "sahil", "Sahil Khan", "https://imageio.forbes.com/specials-images/imageserve/61688aa1d4a8658c3f4d8640/Antonio-Juliano/0x0.jpg?format=jpg&width=960", "This is Akhand Swarup’s Bio He's an IES Officer.", is_speaker_followed = false),
            Users("", "sahil", "Sahil Khan", "https://imageio.forbes.com/specials-images/imageserve/61688aa1d4a8658c3f4d8640/Antonio-Juliano/0x0.jpg?format=jpg&width=960", "This is Akhand Swarup’s Bio He's an IES Officer.", is_speaker_followed = false),
            Users("", "sahil", "Sahil Khan", "https://imageio.forbes.com/specials-images/imageserve/61688aa1d4a8658c3f4d8640/Antonio-Juliano/0x0.jpg?format=jpg&width=960", "This is Akhand Swarup’s Bio He's an IES Officer.", is_speaker_followed = false),
            Users("", "sahil", "Sahil Khan", "https://imageio.forbes.com/specials-images/imageserve/61688aa1d4a8658c3f4d8640/Antonio-Juliano/0x0.jpg?format=jpg&width=960", "This is Akhand Swarup’s Bio He's an IES Officer.", is_speaker_followed = false),
            Users("", "sahil", "Sahil Khan", "https://imageio.forbes.com/specials-images/imageserve/61688aa1d4a8658c3f4d8640/Antonio-Juliano/0x0.jpg?format=jpg&width=960", "This is Akhand Swarup’s Bio He's an IES Officer.", is_speaker_followed = false),
            Users("", "sahil", "Sahil Khan", "https://imageio.forbes.com/specials-images/imageserve/61688aa1d4a8658c3f4d8640/Antonio-Juliano/0x0.jpg?format=jpg&width=960", "This is Akhand Swarup’s Bio He's an IES Officer.", is_speaker_followed = false),
        )

        setContent {
            // A surface container using the 'background' color from the theme

            val peopleList = viewModel.bbToFollow.collectAsLazyPagingItems()

//            PeopleToFollowScreen(peopleList)

        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    JoshBadeBhaiyaTheme {
        Greeting("Android")
    }
}