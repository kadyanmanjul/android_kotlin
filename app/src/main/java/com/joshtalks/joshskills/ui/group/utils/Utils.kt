package com.joshtalks.joshskills.ui.group.utils

fun getMemberCount(memberText : String) : Int {
    var memberCount = 1
    if(memberText.isNotBlank()) {
        if(memberText.contains("practise partner calls"))
            return 2
        val num = memberText.split(" ")
        if(num.isNotEmpty())
            memberCount = num[0].toIntOrNull() ?: 1
    }
    return memberCount
}