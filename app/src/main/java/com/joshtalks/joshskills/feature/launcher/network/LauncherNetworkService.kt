package com.joshtalks.joshskills.feature.launcher.network

import com.joshtalks.joshskills.base.constants.DIR
import com.joshtalks.joshskills.repository.local.model.GaIDMentorModel
import com.joshtalks.joshskills.repository.local.model.RequestRegisterGAId
import com.joshtalks.joshskills.repository.server.GaIdResponse
import com.joshtalks.joshskills.repository.server.LinkAttribution
import com.joshtalks.joshskills.repository.server.signup.LoginResponse
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

@JvmSuppressWildcards
interface LauncherNetworkService {
    @POST("${DIR}/link_attribution/analytics/")
    suspend fun saveDeepLinkImpression(@Body params: Map<String, String>): Response<Any>

    @POST("${DIR}/mentor/device_gaid_id/")
    suspend fun getGaid(@Body params: Map<String, String?>): Response<GaIdResponse>

    @POST("$DIR/user/create_user/")
    suspend fun createGuestUser(@Body params: Map<String, String>): LoginResponse

    @POST("${DIR}/mentor/gaid/")
    suspend fun registerGAIdDetailsV2Async(@Body body: RequestRegisterGAId): GaIDMentorModel
}