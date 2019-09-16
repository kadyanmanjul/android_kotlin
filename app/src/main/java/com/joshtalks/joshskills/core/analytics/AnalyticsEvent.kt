package com.joshtalks.joshskills.core.analytics

enum class AnalyticsEvent(val NAME: String) {


    BACK_PRESSED("Back Pressed"),

    LOGIN("Login"),
    PROFILE_IMAGE_UPLOAD("Profile Image Upload"),

    ACTIVITY_OPENED("Activity Opened"),


    QUESTION_SENT("Question Sent"),
    QUESTION_RECEIVED("Question Received"),
    MESSAGE_SENT_AUDIO("Message Sent Audio"),
    MESSAGE_SENT_TEXT("Message Sent Text"),
    MESSAGE_SENT_VIDEO("Message Sent Video"),
    MESSAGE_SENT_IMAGE("Message Sent Image"),

    AUDIO_OPENED("Audio Opened"),
    AUDIO_DOWNLOAD("Audio Download"),
    IMAGE_OPENED("Image Opened"),
    IMAGE_DOWNLOAD("Image Download"),
    PDF_OPENED("PDF Opened"),
    PDF_DOWNLOAD("PDF Download"),
    VIDEO_OPENED("Video Opened"),
    VIDEO_DOWNLOAD("Video Download"),

    NAME_ENTER("Name Enter"),
    DOB_SELECTED("DOB Selected"),
    IMAGE_UPLOAD("Image Upload"),

    API_FAILED("Api Failed"),

    PERMISSION_ACCEPT("Permission accept"),
    PERMISSION_DENIED("Permission accept"),

}