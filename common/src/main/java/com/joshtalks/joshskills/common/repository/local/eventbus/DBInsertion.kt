package com.joshtalks.joshskills.common.repository.local.eventbus

data class DBInsertion(var tableName: String, val refreshMessageUser: Boolean = false)
