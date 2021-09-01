package com.joshtalks.joshskills.repository.local.eventbus

class LessonItemClickEventBus(val lessonId: Int, val isNewGrammar: Boolean,val isLessonCompleted:Boolean=false)