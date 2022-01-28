package com.joshtalks.joshskills.quizgame.ui.data.model

class CallRequest(
    var favUserId:String, // jisko notification bhejna hai uska uuid
    var fromUserId: String,  // send karne wale ka mentor_id
    var fromUserName :String, // send karne wale ka name
    var channelId:String, // channel id
    var type:String, // type= accept/decline
    var token:String // token
)
