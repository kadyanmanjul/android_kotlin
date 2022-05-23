package com.joshtalks.badebhaiya.signup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.joshtalks.badebhaiya.composeTheme.JoshBadeBhaiyaTheme
import com.joshtalks.badebhaiya.feed.model.Users
import com.joshtalks.badebhaiya.signup.fragments.PeopleToFollowScreen

class PeopleToFollowActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JoshBadeBhaiyaTheme {
                // A surface container using the 'background' color from the theme
                val peopleList = arrayOf(
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
                    JoshBadeBhaiyaTheme {
                        PeopleToFollowScreen(peopleList)
                    }
                }
            }
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