package com.joshtalks.joshskills.core.analytics

enum class AnalyticsEvent(val NAME: String) {
    //Common
    APP_INSTALL("App Install V2"),
    APP_INSTALL_WITH_DEEP_LINK("App Install With Deep Link V2"),
    APP_UNINSTALLED("App Uninstalled V2"),
    /*
    LATITUDE("Latitude V2"),  //Not yet
    LONGITUDE("Longitude V2"), //Not yet
    */
    APP_VERSION_CODE("App version V2"),
    //APP_VERSION_NAME("App version name V2"),

    //parameters
    TEST_ID_PARAM("test_id"), //DONE
    SOURCE("Source V2"),
    DEVICE_MANUFACTURER("Device V2"),
    DEVICE_MODEL("Device V2"),
    ANDROID_OR_IOS("Android/IOS V2"),
    USER_GAID("User Gaid V2"),
    USER_NAME("User name V2"),
    USER_EMAIL("User email id V2"),


    APP_LAUNCHED("App Launched V2"),
    NETWORK_CARRIER("Network Carrier V2"),
    ACTIVITY_CREATED("Activity Created V2"),
    ACTIVITY_START("Activity Start V2"),
    ACTIVITY_RESUME("Activity Resume V2"),
    ACTIVITY_PAUSE("Activity Pause V2"),
    ACTIVITY_STOP("Activity Stop V2"),
    ACTIVITY_DESTROY("Activity Destroy V2"),
    ACTIVITY_OPENED("Activity Opened V2"),
    ACTIVITY_REOPEN("Activity Reopened V2"),
    BACK_PRESSED("Back Pressed V2"),

    // Login Screen
    LOGIN_SCREEN_1("Login Screen 1 V2"),
    COURSE_EXPLORER("Course Explorer V2"),
    EXPLORE_BTN_CLICKED("Explore Button Clicked V2"),
    LOGIN_CLICKED("Login Clicked V2"),//  called in both onBoardActivity and from payment LoginDialogFragment
    LOGIN_TRUECALLER_CLICKED("Login Truecaller Clicked V2"),// called in both onBoardActivity and from payment LoginDialogFragment

    LOGIN_WITH_TRUECALLER("Login With Truecaller V2"),
    LOGIN_WITH_OTP("Login With Otp V2"),

    //LOGIN with otp
    PHONENO_ENTERED_NEXT_BTN_CLICKED("Phone Number Entered Next Button Clicked V2"),
    COUNTRY_FLAG_CHANGED("CountryFlag Icon Changed V2"),
    FLAG_DIALOG_SEARCH_TEXT("Flag Searched V2"),
    FLAG_DIALOG_CLOSED("Flag Dialog Closed V2"),
    FLAG_DIALOG_ITEM_CLICKED("Flag Dialog Flag Selected V2"),
    HELP_SELECTED("Help Selected V2"),
    TERMS_CONDITION_CLICKED("Terms&Conditions Clicked V2"),

    // TRUECALLER LOGIN
    TRRUECALLER_CONTINUE_CLICKED("Truecaller Continue Button Clicked V2"),
    USE_OTHER_MOBILE_CLICKED("Another Mobile Number Button Clicked V2"),

    // LOGIN FRAGMENTS

    RESEND_OTP("Resend OTP V2"),
    INCORRECT_OTP("Incorrect OTP V2"),
    NEXT_OTP_CLICKED("OTP Verified V2"),
    OTP_VERIFIED("OTP Verified V2"),
    VERIFIED_VIA_TRUECALLER("Verified via TruecallerV2"),
    NEXT_TO_OTP_SCREEN_CLICKED("Next To Otp Screen Clicked V2"),
    LOGIN_SUCCESS("Login Success V2"),
    LOGIN_SUCCESSFULLY("Login Successfully V2"),

    //HELPLINE SELECTED
    CLICK_HELPLINE_SELECTED("Call Helpline Selected V2"),
    CALL_HELPLINE("Call Helpline V2"),
    HELP_SUBMITTED("Help Submitted V2"),
    HELP_BACK_CLICKED("Help back button clicked V2"),
    HELP_COMPLAINT_FOAM("Help Complain Foam V2"),


    // ExploreCourses Screen

    EXPLORE_OPENED("Explore Opened V2"),
    COURSE_LIST_SCROLLED("Course List Scrolled V2"),
    COURSE_CLICKED("Course Clicked V2"),
    BACK_BTN_EXPLORESCREEN("Back Pressed on Explore Screen V2"),
    MORE_ICON_CLICKED("More Icons Clicked V2"),
    LOGOUT_CLICKED("Logout Menu item clicked V2"),
    USER_LOGGED_OUT("User logged out V2"),


    //CourseDetails Screen

    BUY_NOW_SELECTED("Buy Now Selected V2"),
    PAYMENT_DIALOG("Payment Dialog V2"),
    COUPON_SELECTED("Coupon Selected V2"),
    COUPON_INSERTED("Coupon Inserted V2"),
    COUPON_VALID("Coupon Valid V2"),
    COUPON_INVALID("Coupon Invalid V2"),

    COURSE_DETAILS("Clicked Course Details V2"),
    COMPLETE_PAYMENT("Complete Payment V2"), //
    COMPLETE_PAYMENT_CLICKED("Complete Payment Btn Clicked V2"),
    START_COURSE_NOW("Start your course now v2"), //  COURSE_CLICKED same





    INBOX_SCREEN("Inbox Screen V2"),
    COURSE_SELECTED("Course Selected V2"),
    AUDIO_BUTTON_CLICKED("Audio Button clicked V2"),
    AUDIO_SENT("Audio Sent V2"),
    AUDIO_CANCELLED("Audio Cancelled V2"),
    CAMERA_CLICKED("Camera Clicked V2"),
    ATTACHMENT_CLICKED("Attachment clicked V2"),
    AUDIO_SELECTED("Audio Selected V2"),
    CAMERA_SELECTED("Camera Selected V2"),
    GALLERY_SELECTED("Gallery Selected V2"),
    MEDIA_DOWNLOAD("Media Download V2"),
    VIDEO_WATCH_ACTIVITY("Video Watch Activity V2"),

    AUDIO_PLAYED("Audio Played V2"),

    EMOJI_CLICKED("Emoji Clicked V2"),

    PDF_OPENED("PDF Opened V2"),
    COURSE_OPENED("Course Opened V2"), // conversation Activity opened
    PROFILE_IMAGE_UPLOAD("Profile Image Upload V2"),
    MESSAGE_SENT_AUDIO("Message Sent Audio V2"),
    MESSAGE_SENT_TEXT("Message Sent Text V2"),
    MESSAGE_SENT_VIDEO("Message Sent Video V2"),
    MESSAGE_SENT_IMAGE("Message Sent Image V2"),

    AUDIO_OPENED("Audio Opened V2"),
    AUDIO_DOWNLOAD("Audio Download V2"),
    IMAGE_OPENED("Image Opened V2"),
    IMAGE_DOWNLOAD("Image Download V2"),
    PDF_DOWNLOAD("PDF Download V2"),
    VIDEO_OPENED("Video Opened V2"),
    VIDEO_DOWNLOAD("Video Download V2"),

    NAME_ENTER("Name Enter V2"),
    DOB_SELECTED("DOB Selected V2"),
    IMAGE_UPLOAD("Image Upload V2"),

    API_FAILED("Api Failed V2"),

    PERMISSION_ACCEPT("Permission accept V2"),
    PERMISSION_DENIED("Permission accept V2"),
    NOTIFICATION_RECEIVED("Notification Received V2"),
    NOTIFICATION_CLICKED("Notification Clicked V2"),
    REGISTRATION_COMPLETED("Registration_completed V2"),
    COURSE_STARTED("Course_Started V2"),
    PURCHASE_COURSE("Purchase Course V2"),


    TEST_ID_OPENED("Test ID Opened V2"),

    RAZORPAY_SDK("Razorpay SDK V2"),
    PAYMENT_COMPLETED("Payment Completed V2"),
    PAYMENT_INITIATED("Payment Initiated V2"),
    PAYMENT_FAILED("Payment Fail V2"),

    PRACTISE_OPENED("Practise Opened V2"),
    AUDIO_SUBMITTED("Audio Submitted V2"),
    VIDEO_SUBMITTED("Video Submitted V2"),
    TEXT_SUBMITTED("Text Submitted V2"),
    DOCUMENT_SUBMITTED("Document Submitted V2"),
    REFERRAL_SELECTED("Referral Selected V2"),
    CODE_COPIED("Code Copied V2"),
    SHARE_ON_WHATSAPP("Share On Whatsapp V2"),
    SHARE_ON_ALL("Share On All V2"),

    AUDIO_RECORD("Audio Record V2"),
    HAVE_COUPON_CODE("Have Coupon Code V2"),
    COURSE_OVERVIEW("Course Overview V2"),// CourseProgressListingScreen
    CERTIFICATE_PROGRESS_CLICKED("Certificate Progress Clicked V2"),
    PERFORMANCE_CLICKED("Performance Clicked V2"),
    VIEW_SAMPLE_CERTIFICATE_OPEN("View Sample Certificate Open V2"),
    VIEW_SAMPLE_CERTIFICATE_CLOSE("View Sample Certificate Close V2"),
    VIDEO_CLICKED_COURSE_OVERVIEW("Video Click Course Overview V2"),
    PRACTISE_CLICKED_COURSE_OVERVIEW("Practise Clicked Course Overview V2"),
    FEEDBACK_INITIATED("Feedback Initiated V2"),
    FEEDBACK_SUBMITTED("Feedback Submitted V2"),
    FEEDBACK_IGNORE("Feedback Ignore V2"),


}