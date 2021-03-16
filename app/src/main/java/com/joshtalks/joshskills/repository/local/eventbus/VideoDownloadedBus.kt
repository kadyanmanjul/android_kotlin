package com.joshtalks.joshskills.repository.local.eventbus

import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.LessonQuestion

class VideoDownloadedBus(val messageObject: ChatModel)

class VideoDownloadedBusForLessonQuestion(val lessonQuestion: LessonQuestion)
