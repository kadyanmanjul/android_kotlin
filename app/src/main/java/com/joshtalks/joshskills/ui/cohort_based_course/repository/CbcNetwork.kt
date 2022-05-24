package com.joshtalks.joshskills.ui.cohort_based_course.repository

import com.joshtalks.joshskills.base.constants.DIR
import com.joshtalks.joshskills.ui.cohort_based_course.models.CohortModel
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface CbcNetwork {

    @GET("$DIR/course/cohort_batch/")
    suspend fun getCohortBatches(): Response<CohortModel>

    @JvmSuppressWildcards
    @POST("$DIR/course/cohort_batch/")
    suspend fun postSelectedBatch(@Body params: Map<String, Any>): Response<Unit>

}
