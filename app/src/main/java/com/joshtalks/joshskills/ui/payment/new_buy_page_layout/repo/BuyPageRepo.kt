package com.joshtalks.joshskills.ui.payment.new_buy_page_layout.repo

import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model.PriceParameterModel

class BuyPageRepo {

    //it's need test id
    suspend fun getFeatureList(testId:Int) = AppObjectController.commonNetworkService.getCourseFeatureDetails(testId)

    suspend fun getCouponList() = AppObjectController.commonNetworkService.getValidCoupon(0)

    suspend fun getPriceList(params: PriceParameterModel) = AppObjectController.commonNetworkService.getCoursePriceList(params)

    suspend fun getReviewAndRating(testId:Int) = AppObjectController.commonNetworkService.getReviews(testId)

    suspend fun getCouponFromCode(code: String) = AppObjectController.commonNetworkService.getCouponFromCode(code)

}