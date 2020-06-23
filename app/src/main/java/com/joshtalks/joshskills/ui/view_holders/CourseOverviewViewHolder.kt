package com.joshtalks.joshskills.ui.view_holders

import com.joshtalks.joshskills.repository.server.course_detail.CourseOverviewData

class CourseOverviewViewHolder(
    override val sequenceNumber: Int,
    private val data: CourseOverviewData
) : CourseDetailsBaseCell(sequenceNumber)
