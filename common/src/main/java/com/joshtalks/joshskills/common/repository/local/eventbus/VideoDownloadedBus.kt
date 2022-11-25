package com.joshtalks.joshskills.common.repository.local.eventbus

import com.joshtalks.joshskills.common.repository.local.entity.ChatModel
import com.joshtalks.joshskills.common.repository.local.entity.LessonQuestion

class VideoDownloadedBus(val messageObject: ChatModel)

class VideoDownloadedBusForLessonQuestion(val lessonQuestion: LessonQuestion)
