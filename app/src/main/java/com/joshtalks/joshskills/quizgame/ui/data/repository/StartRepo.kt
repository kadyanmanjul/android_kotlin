package com.joshtalks.joshskills.quizgame.ui.data.repository

import com.joshtalks.joshskills.quizgame.ui.data.model.AddUserDb
import com.joshtalks.joshskills.quizgame.ui.data.model.UserDetails
import com.joshtalks.joshskills.quizgame.ui.data.network.RetrofitInstanse

class StartRepo {
       suspend fun addUser(addUserDb: AddUserDb) = RetrofitInstanse.getRetrofitInstance()?.addUserToDb(addUserDb)
}