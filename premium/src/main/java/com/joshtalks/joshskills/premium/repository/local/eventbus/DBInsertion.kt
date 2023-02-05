package com.joshtalks.joshskills.premium.repository.local.eventbus

data class DBInsertion(var tableName: String, val refreshMessageUser: Boolean = false)
