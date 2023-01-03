package com.joshtalks.joshskills.explore

import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.repository.service.DIR
import com.joshtalks.joshskills.explore.course_details.models.CourseDetailsResponseV2
import com.joshtalks.joshskills.explore.course_details.models.DemoCourseDetailsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.QueryMap

object ExploreNetwork {
    val exploreNetworkService: ExploreNetworkService by lazy {
        AppObjectController.retrofit.create(ExploreNetworkService::class.java)
    }
}

interface ExploreNetworkService {

    @GET("$DIR/course/course_details/")
    suspend fun getCourseDetails(@QueryMap params: Map<String, String>): Response<CourseDetailsResponseV2>

    @GET("$DIR/course/course_details_v2/")
    suspend fun getDemoCourseDetails(): Response<DemoCourseDetailsResponse>
}