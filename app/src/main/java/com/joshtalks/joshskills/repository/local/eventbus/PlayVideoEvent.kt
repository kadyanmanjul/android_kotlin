package com.joshtalks.joshskills.repository.local.eventbus

import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.LessonQuestion

data class PlayVideoEvent(var chatModel: ChatModel)

data class PlayVideoEventForLessonQuestion(var lessonQuestion: LessonQuestion)
