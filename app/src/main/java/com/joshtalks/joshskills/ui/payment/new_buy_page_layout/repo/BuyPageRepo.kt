package com.joshtalks.joshskills.ui.payment.new_buy_page_layout.repo

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model.PriceParameterModel
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.utils.ReviewPagingSource

class BuyPageRepo {

    //it's need test id
    suspend fun getFeatureList(testId:Int) = AppObjectController.commonNetworkService.getCourseFeatureDetails(testId)

    suspend fun getCouponList() = AppObjectController.commonNetworkService.getValidCoupon(0)

    suspend fun getPriceList(params: PriceParameterModel) = AppObjectController.commonNetworkService.getCoursePriceList(params)

    fun getReviewResult(testId: Int) =
        Pager(PagingConfig(10, enablePlaceholders = false, maxSize = 150)) {
            ReviewPagingSource(
                testId = testId,
                apiService = AppObjectController.commonNetworkService
            )
        }

    suspend fun getCouponFromCode(code: String) = AppObjectController.commonNetworkService.getCouponFromCode(code)

    suspend fun saveBuyPageImpression(map: Map<String, Any>) =
        AppObjectController.commonNetworkService.saveNewBuyPageLayoutImpression(map)

}