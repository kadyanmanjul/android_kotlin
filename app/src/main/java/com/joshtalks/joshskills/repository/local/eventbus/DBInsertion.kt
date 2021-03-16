package com.joshtalks.joshskills.repository.local.eventbus

data class DBInsertion(var tableName: String,val refreshMessageUser:Boolean=false)