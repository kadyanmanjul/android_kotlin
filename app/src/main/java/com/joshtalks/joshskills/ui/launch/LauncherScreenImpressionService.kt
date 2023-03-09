package com.joshtalks.joshskills.ui.launch

import com.joshtalks.joshskills.repository.service.DIR
import retrofit2.http.Body
import retrofit2.http.POST

interface LauncherScreenImpressionService {
    @POST("$DIR/impression/launcher_screen/")
    suspend fun saveImpression(@Body impression: Map<String, String>)

    @POST("$DIR/impression/ia_to_main_app")
    suspend fun saveImpressionInstantApp(@Body impression: Map<String, String>)

}