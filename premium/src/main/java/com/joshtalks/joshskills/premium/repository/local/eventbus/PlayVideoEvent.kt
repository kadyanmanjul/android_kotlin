package com.joshtalks.joshskills.premium.repository.local.eventbus

import com.joshtalks.joshskills.premium.repository.local.entity.ChatModel
import com.joshtalks.joshskills.premium.repository.local.entity.LessonQuestion

data class PlayVideoEvent(var chatModel: ChatModel)

data class PlayVideoEventForLessonQuestion(var lessonQuestion: LessonQuestion)
