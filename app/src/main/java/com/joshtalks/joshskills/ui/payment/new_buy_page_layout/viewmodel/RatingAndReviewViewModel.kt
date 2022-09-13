package com.joshtalks.joshskills.ui.payment.new_buy_page_layout.viewmodel

import android.util.Log
import android.view.View
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.adapter.RatingAndReviewsAdapter
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.repo.BuyPageRepo
import com.joshtalks.joshskills.ui.special_practice.utils.BUY_PAGE_BACK_PRESS
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RatingAndReviewViewModel: BaseViewModel() {

    private val buyPageRepo by lazy { BuyPageRepo() }
    val mainDispatcher: CoroutineDispatcher by lazy { Dispatchers.Main }
    var ratingAndReviewAdapter =  RatingAndReviewsAdapter()

    fun getRatingAndReviews(testId:String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = buyPageRepo.getReviewAndRating(Integer.parseInt(testId))
                if (response.isSuccessful && response.body() != null) {
                    withContext(mainDispatcher) {
                        Log.e("sagar", "getRatingAndReviews: ${response.body()?.reviews}")
                        ratingAndReviewAdapter.addRatingList(response.body()?.reviews)
                    }
                }

            }catch (e: Exception){
                Log.e("sagar", "getRatingAndReviews: ${e.message}")
                e.printStackTrace()
            }

        }
    }

    fun onBackPress(view: View) {
        message.what = BUY_PAGE_BACK_PRESS
        singleLiveEvent.value = message
    }

}