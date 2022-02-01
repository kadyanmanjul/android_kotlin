package com.joshtalks.joshskills.ui.group.lib

import com.joshtalks.joshskills.ui.group.model.GroupMember
import com.joshtalks.joshskills.ui.group.model.MemberResult
import com.pubnub.api.models.consumer.objects_api.member.PNGetChannelMembersResult

data class PubNubMemberData(val data: PNGetChannelMembersResult) : MemberNetworkData {

    val groupList = mutableListOf<GroupMember>()

    override fun getMemberData(): MemberResult? {
        if (data.data.isEmpty())
            return null
        data.data.map {
            groupList.add(GroupMember(
                mentorID = it.uuid.id,
                memberName = it.uuid.name,
                memberIcon = it.uuid.profileUrl,
                isAdmin = false,
                isOnline = false
            ))
        }
        return MemberResult(groupList, data.totalCount, 0)
    }

    override fun getPageInfo() = PageInfo(
        pubNubPrevious = data.previousPage(),
        pubNubNext = data.nextPage(),
    )
}