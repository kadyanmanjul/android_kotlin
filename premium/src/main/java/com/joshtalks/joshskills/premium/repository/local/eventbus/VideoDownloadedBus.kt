package com.joshtalks.joshskills.premium.repository.local.eventbus

import com.joshtalks.joshskills.premium.repository.local.entity.ChatModel
import com.joshtalks.joshskills.premium.repository.local.entity.LessonQuestion

class VideoDownloadedBus(val messageObject: ChatModel)

class VideoDownloadedBusForLessonQuestion(val lessonQuestion: LessonQuestion)
