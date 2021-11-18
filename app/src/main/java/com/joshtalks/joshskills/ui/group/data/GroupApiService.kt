package com.joshtalks.joshskills.ui.group.data

import com.joshtalks.joshskills.repository.local.model.RequestRegisterGAId
import com.joshtalks.joshskills.repository.service.DIR
import com.joshtalks.joshskills.ui.group.model.AddGroupRequest
import com.joshtalks.joshskills.ui.group.model.GroupListResponse
import com.joshtalks.joshskills.ui.group.model.JoinGroupRequest
import com.squareup.okhttp.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GroupApiService {
    @GET("$DIR/group/list_groups/")
    suspend fun getGroupList(@Query("page") pageNo : Int, @Query("mentor_id") mentorId : String): GroupListResponse

    @GET("$DIR/group/search_groups/")
    suspend fun searchGroup(@Query("page") pageNo : Int, @Query("key") searchQuery : String): GroupListResponse

    @POST("$DIR/group/add_member_group/")
    suspend fun joinGroup(@Body request : JoinGroupRequest): Response<Unit>

    @POST("$DIR/group/create_group_v2/")
    suspend fun createGroup(@Body request : AddGroupRequest): Map<String, Any?>

    @GET("$DIR/group/group_online_members/{group_id}")
    suspend fun getOnlineUserCount(@Path("group_id") groupId: String): Map<String, Any?>
}