package com.joshtalks.joshskills.premium.ui.group.lib

import com.joshtalks.joshskills.premium.ui.group.model.GroupMember
import com.pubnub.api.models.consumer.objects_api.member.PNGetChannelMembersResult

data class PubNubMemberData(val data: PNGetChannelMembersResult) : MemberNetworkData {

    val memberList = mutableListOf<GroupMember>()

    override fun getMemberData(groupId: String, adminId: String): List<GroupMember> {
        memberList.clear()
        data.data.map {
            try {
                memberList.add(
                    GroupMember(
                        mentorID = it.uuid.id,
                        memberName = it.uuid.name,
                        memberIcon = it.uuid.profileUrl,
                        isAdmin = adminId == it.uuid.id,
                        isOnline = false,
                        groupId = groupId
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return memberList
    }

    override fun getPageInfo() = PageInfo(
        pubNubPrevious = data.previousPage(),
        pubNubNext = data.nextPage(),
    )
}