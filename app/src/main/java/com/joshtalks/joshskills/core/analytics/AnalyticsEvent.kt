package com.joshtalks.joshskills.core.analytics

enum class AnalyticsEvent(val NAME: String) {


    BACK_PRESSED("Back Pressed"),
    ACTIVITY_OPENED("Activity Opened"),
    APP_INSTALLED("App Installed"),
    LOGIN_SCREEN_1("Login Screen 1"),
    LOGIN_CLICKED("Login Clicked"),
    OTP_ACCOUNT_KIT_ACTIVITY("Otp Account Kit Activity"),
    WHATSAPP_SELECTED("Whatsapp Selected"),

    UNREGISTER_USER("Unregister User"),


    SMS_SELECTED("Sms Selected"),
    OTP_VERIFICATION_SCREEN("OTP Verification Screen"),

    COURSE_FAILURE_SCREEN("Course Failure Screen"),
    CLICK_HELPLINE_SELECTED("Call Helpline Selected"),
    CLICK_TO_PAY_SELECTED("Click to Pay Selected"),

    INBOX_SCREEN("Inbox Screen"),
    COURSE_SELECTED("Course Selected"),


    SCROLL("Scroll"),

    AUDIO_BUTTON_CLICKED("Audio Button clicked"),
    AUDIO_SENT("Audio Sent"),
    AUDIO_CANCELLED("Audio Cancelled"),

    CAMERA_CLICKED("Camera Clicked"),
    VIDEO_RECORDED("Video Recorded"),
    VIDEO_SENT("Video Sent"),

    IMAGE_CLICKED("Image Clicked"),
    FILTER_SELECTED("Filter Selected"),
    IMAGE_SENT("Image Sent"),
    CROP_CLICKED("Crop Selected"),

    ATTACHMENT_CLICKED("Attachment clicked"),
    AUDIO_SELECTED("Audio Selected"),
    CAMERA_SELECTED("Camera Selected"),
    GALLERY_SELECTED("Gallery Selected"),
    MEDIA_DOWNLOAD("Media Download"),
    WATCH_ACTIVITY("Watch Activity"),

    AUDIO_PLAYED("Audio Played"),

    EMOJI_CLICKED("Emoji Clicked"),

    PDF_OPENED("PDF Opened"),

    LOGIN_ERROR("Login Error"),
    LOGIN_CANCELLED("Login Cancelled"),
    LOGIN_SUCCESS("Login Success"),
    CHAT_SCREEN("Chat Screen"),


    LOGIN("Login"),
    PROFILE_IMAGE_UPLOAD("Profile Image Upload"),



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
    PDF_DOWNLOAD("PDF Download"),
    VIDEO_OPENED("Video Opened"),
    VIDEO_DOWNLOAD("Video Download"),

    NAME_ENTER("Name Enter"),
    DOB_SELECTED("DOB Selected"),
    IMAGE_UPLOAD("Image Upload"),

    API_FAILED("Api Failed"),

    PERMISSION_ACCEPT("Permission accept"),
    PERMISSION_DENIED("Permission accept"),
    NOTIFICATION_RECEIVED("Notification Received"),
    NOTIFICATION_CLICKED("Notification Clicked"),
    REGISTRATION_COMPLETED("Registration_completed"),
    COURSE_STARTED("Course_Started"),






}