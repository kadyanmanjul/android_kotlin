package com.joshtalks.joshskills.ui.group.lib

import com.joshtalks.joshskills.ui.group.model.GroupMember
import com.joshtalks.joshskills.ui.group.model.MemberResult
import com.pubnub.api.models.consumer.objects_api.member.PNGetChannelMembersResult

data class PubNubMemberData(val data: PNGetChannelMembersResult) : MemberNetworkData {

    val memberList = mutableListOf<GroupMember>()

    override fun getMemberData(adminId: String): MemberResult {
        memberList.clear()
        data.data.map {
            memberList.add(GroupMember(
                mentorID = it.uuid.id,
                memberName = it.uuid.name,
                memberIcon = it.uuid.profileUrl,
                isAdmin = adminId == it.uuid.id,
                isOnline = false
            ))
        }
        return MemberResult(memberList, data.totalCount)
    }

    override fun getPageInfo() = PageInfo(
        pubNubPrevious = data.previousPage(),
        pubNubNext = data.nextPage(),
    )
}