package com.joshtalks.joshskills.ui.voip.favorite

import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.model.Mentor
import java.util.HashMap

class FavoriteCallerRepository {
    private val p2pNetworkService by lazy { AppObjectController.p2pNetworkService }

    suspend fun getFavList() = p2pNetworkService.getFavoriteCallerList(Mentor.getInstance().getId())

    suspend fun removeUserFormFppLit(requestParams: HashMap<String, List<Int>>) = p2pNetworkService.removeFavoriteCallerList(Mentor.getInstance().getId(), requestParams)

    suspend fun userIsCallOrNot(map: HashMap<String, String>) = p2pNetworkService.checkUserInCallOrNot(map)

}