package com.joshtalks.joshskills.lesson.repo

import com.joshtalks.joshskills.common.repository.local.entity.practise.PointsListResponse
import com.joshtalks.joshskills.lesson.speaking.spf_models.BlockStatusModel
import com.joshtalks.joshskills.lesson.speaking.spf_models.UserRating
import com.joshtalks.joshskills.lesson.speaking.spf_models.VideoPopupItem
import com.joshtalks.joshskills.voip.base.constants.DIR
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface LessonService {
    @Headers("Cache-Control: public, only-if-cached,  max-stale=86400,  max-age=86400")
    @GET("$DIR/p2p/rating/")
    suspend fun getUserRating(): Response<UserRating>

    @GET("${DIR}/p2p/block_status")
    suspend fun getUserBlockStatus(): Response<BlockStatusModel>

    @GET("${DIR}/course/introduction_data/")
    suspend fun getIntroSpeakingVideo(): Response<VideoPopupItem>

    @GET("${DIR}/reputation/vp_rp_snackbar/")
    suspend fun getSnackBarText(
        @Query("question_id") questionId: String? = null,
        @Query("channel_name") channelName: String? = null,
        @Query("room_id") roomId: String? = null,
        @Query("conversation_question_id") conversationQuestionId: String? = null,
    ): PointsListResponse
}