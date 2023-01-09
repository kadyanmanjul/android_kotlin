package com.joshtalks.joshskills.buypage.new_buy_page_layout.repo

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.buypage.new_buy_page_layout.model.PriceParameterModel
import com.joshtalks.joshskills.buypage.new_buy_page_layout.utils.ReviewPagingSource
import com.joshtalks.joshskills.common.repository.service.CommonNetworkService

class BuyPageRepo {

    val buyPageNetworkService: BuyPageService by lazy {
        AppObjectController.retrofit.create(BuyPageService::class.java)
    }

    //it's need test id
    suspend fun getFeatureList(testId: Int) = buyPageNetworkService.getCourseFeatureDetails(testId)

    suspend fun getCouponList(testId: Int, lessonsCompleted: Int? = null) =
        AppObjectController.commonNetworkService.getValidCoupon(testId = testId, lessonsCompleted = lessonsCompleted)

    suspend fun getPriceList(params: PriceParameterModel) = buyPageNetworkService.getCoursePriceList(params)

    fun getReviewResult(testId: Int) =
        Pager(PagingConfig(10, enablePlaceholders = false, maxSize = 150)) {
            ReviewPagingSource(
                testId = testId,
                apiService = buyPageNetworkService
            )
        }

    suspend fun getCouponFromCode(code: String, testId: Int, lessonsCompleted: Int? = null) =
        buyPageNetworkService.getCouponFromCode(
            code = code,
            testId = testId,
            lessonsCompleted = lessonsCompleted
        )

    suspend fun saveBuyPageImpression(map: Map<String, String>) =
        AppObjectController.commonNetworkService.saveNewBuyPageLayoutImpression(map)

    suspend fun postSupportReason(map: HashMap<String, String>) =
        AppObjectController.commonNetworkService.saveSalesSupportReason(map)

    suspend fun getReasonList() = buyPageNetworkService.getSalesSupportReason()

    suspend fun logPaymentEvent(map: Map<String, Any>) =
        AppObjectController.commonNetworkService.saveJuspayPaymentLog(map)

    suspend fun saveBranchLog(params: Map<String, Any>) =
        AppObjectController.commonNetworkService.savePaymentLog(params)

}