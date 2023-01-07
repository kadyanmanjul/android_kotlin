package com.joshtalks.joshskills

import com.joshtalks.joshskills.common.repository.service.DIR
import retrofit2.http.Body
import retrofit2.http.POST

interface LauncherScreenImpressionService {
    @POST("$DIR/impression/launcher_screen/")
    suspend fun saveImpression(@Body impression: Map<String, String>)
}