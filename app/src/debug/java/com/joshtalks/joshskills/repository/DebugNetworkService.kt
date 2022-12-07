package com.joshtalks.joshskills.repository

import com.joshtalks.joshskills.common.repository.service.DIR
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST

interface DebugNetworkService {
    @POST("$DIR/mentor/delete_user_gaid/")
    suspend fun deleteGaid(gaid: String): Response<Any>

    @POST("$DIR/mentor/delete_user/")
    suspend fun deleteUser(phone: String): Response<Any>

    @POST("$DIR/mentor/register/")
    suspend fun registerUser(): Response<Any>

    @GET("$DIR/mentor/instance/")
    suspend fun getInstance(): Response<Any>

    @GET("$DIR/mentor/login/")
    suspend fun loginMentor(): Response<Any>

}