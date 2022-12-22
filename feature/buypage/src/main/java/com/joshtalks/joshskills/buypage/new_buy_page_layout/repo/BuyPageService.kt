package com.joshtalks.joshskills.buypage.new_buy_page_layout.repo

import com.joshtalks.joshskills.buypage.new_buy_page_layout.model.*
import com.joshtalks.joshskills.common.repository.service.DIR
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface BuyPageService {

    @GET("$DIR/course/get_user_coupons/")
    suspend fun getValidCoupon(@Query("test_id") testId: Int): Response<CouponListModel>

    @GET("$DIR/course/get_coupon_code/")
    suspend fun getCouponFromCode(@Query("code") code: String): Response<Coupon>

    @POST("$DIR/course/course_price_details/")
    suspend fun getCoursePriceList(@Body params: PriceParameterModel): Response<CoursePriceListModel>

    @GET("$DIR/course/buy_course_feature/")
    suspend fun getCourseFeatureDetails(@Query("test_id") testId: Int): Response<BuyCourseFeatureModel>

    @GET("$DIR/support/sales_support/")
    suspend fun getSalesSupportReason() : Response<SalesReasonList>

    @GET("$DIR/course/list_reviews/")
    suspend fun getReviews(@Query("page") pageNo: Int, @Query("test_id") testId: Int): ReviewsListResponse

}