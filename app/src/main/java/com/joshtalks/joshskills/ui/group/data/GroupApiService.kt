package com.joshtalks.joshskills.ui.group.data

import com.joshtalks.joshskills.repository.service.DIR
import com.joshtalks.joshskills.ui.group.model.*
import org.json.JSONArray
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Path

interface GroupApiService {

    @GET("$DIR/group/list_groups/")
    suspend fun getGroupList(@Query("page") pageNo : Int, @Query("mentor_id") mentorId : String): GroupListResponse

    @GET("$DIR/group/search_groups_v2/")
    suspend fun searchGroup(@Query("page") pageNo : Int, @Query("key") searchQuery : String): GroupListResponse

    @POST("$DIR/group/add_member_group_v2/")
    suspend fun joinGroup(@Body request : GroupRequest): Map<String, Any?>

    @POST("$DIR/group/update_lasttimetoken/")
    suspend fun updateTimeToken(@Body request : TimeTokenRequest): Response<Unit>

    @POST("$DIR/group/create_group_v2/")
    suspend fun createGroup(@Body request : AddGroupRequest): Map<String, Any?>

    @POST("$DIR/group/edit_group_info/")
    suspend fun editGroup(@Body request : EditGroupRequest): Response<Unit>

    @POST("$DIR/group/remove_member_group_v2/")
    suspend fun leaveGroup(@Body request : LeaveGroupRequest): Response<Unit>

    @POST("$DIR/group/group_online_members_count/")
    suspend fun getOnlineUserCount(@Body request: JSONArray): Map<String, GroupMemberCount>

    @GET("$DIR/group/group_online_members/{group_id}/")
    suspend fun getGroupOnlineCount(@Path("group_id") groupId: String): Map<String, Any?>

    @POST("$DIR/group/group_request/")
    suspend fun sendJoinRequest(@Body request: GroupJoinRequest): Response<Unit>

    @GET("$DIR/group/list_group_requests/")
    suspend fun getRequestsList(@Query("group_id") groupId: String): GroupRequestList

}