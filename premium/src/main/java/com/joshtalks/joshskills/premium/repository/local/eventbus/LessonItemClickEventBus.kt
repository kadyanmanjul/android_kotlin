package com.joshtalks.joshskills.premium.repository.local.eventbus

class LessonItemClickEventBus(val lessonId: Int, val isNewGrammar: Boolean,val isLessonCompleted:Boolean=false)