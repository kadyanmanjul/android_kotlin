package com.joshtalks.joshskills.ui.group.utils

import junit.framework.TestCase

class UtilsTest : TestCase() {

    fun testGetMemberCount() {
        val memberText = "4 members, 1 online"
        val memberText1 = "3 members, 1 online"
        val memberText2 = ""
        val memberText3 = "0 members, 1 online"
        val memberText4 = "members, 1 online"
        val memberText5 = "0 practise partner calls in last 24 hours"

        assertEquals(4, getMemberCount(memberText))
        assertEquals(3, getMemberCount(memberText1))
        assertEquals(1, getMemberCount(memberText2))
        assertEquals(0, getMemberCount(memberText3))
        assertEquals(1, getMemberCount(memberText4))
        assertEquals(2, getMemberCount(memberText5))
    }
}