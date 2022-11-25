package com.joshtalks.joshskills.common.ui.payment.new_buy_page_layout.viewmodel

import android.view.View
import androidx.paging.PagingData
import com.joshtalks.joshskills.common.base.BaseViewModel
import com.joshtalks.joshskills.common.ui.payment.new_buy_page_layout.adapter.RatingAndReviewsAdapter
import com.joshtalks.joshskills.common.ui.payment.new_buy_page_layout.model.ReviewItem
import com.joshtalks.joshskills.common.ui.payment.new_buy_page_layout.repo.BuyPageRepo
import com.joshtalks.joshskills.common.ui.payment.new_buy_page_layout.utils.ReviewItemComparator
import com.joshtalks.joshskills.common.ui.special_practice.utils.BUY_PAGE_BACK_PRESS
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

class RatingAndReviewViewModel: com.joshtalks.joshskills.common.base.BaseViewModel() {

    private val buyPageRepo by lazy { BuyPageRepo() }
    var testId = 0
    val mainDispatcher: CoroutineDispatcher by lazy { Dispatchers.Main }
    var ratingAndReviewAdapter =  RatingAndReviewsAdapter(ReviewItemComparator)
    lateinit var reviewLiveData: Flow<PagingData<ReviewItem>>

    fun fetchReviews() {
        reviewLiveData = buyPageRepo.getReviewResult(testId).flow
    }

    fun onBackPress(view: View) {
        message.what = BUY_PAGE_BACK_PRESS
        singleLiveEvent.value = message
    }

}