package com.joshtalks.joshskills.core.analytics

enum class AnalyticsEvent(val NAME: String) {
    //Common
    APP_INSTALL("App Install V3"),
    APP_INSTALL_BY_REFERRAL("Install By Referral V3"),
    APP_VERSION_CODE("App version V3"),

    //parameters
    TEST_ID_PARAM("test_id"), //DONE
    SOURCE("Source V3"),
    STARTED_FROM_NOTIFICATION("Started from Notification V3"),
    NOTIFICATION_ID("Notification id V3"),
    UTM_MEDIUM("UTM Medium V3"),
    DEVICE_MANUFACTURER("Device Manufacturer V3"),
    DEVICE_MODEL("Device Model V3"),
    ANDROID_OR_IOS("Android/IOS Version V3"),
    USER_GAID("User Gaid V3"),
    USER_NAME("User name V3"),
    USER_EMAIL("User email id V3"),
    USER_PHONE_NUMBER("User Phone Number V3"),


    APP_LAUNCHED("App Launched V3"),
    NETWORK_CARRIER("Network Carrier V3"),
    ACTIVITY_CREATED("Activity Created V3"),
    ACTIVITY_START("Activity Start V3"),
    ACTIVITY_RESUME("Activity Resume V3"),
    ACTIVITY_PAUSE("Activity Pause V3"),
    ACTIVITY_STOP("Activity Stop V3"),
    ACTIVITY_DESTROY("Activity Destroy V3"),
    BACK_PRESSED("Back Pressed V3"),

    // Login Screen
    LOGIN_SCREEN_1("OnBoard Login Screen V3"),
    COURSE_EXPLORER("Course Explorer V3"),
    EXPLORE_BTN_CLICKED("Explore Button Clicked V3"),
    LOGIN_INITIATED("Login Initiated V3"),//  called in both onBoardActivity and from payment LoginDialogFragment
    LOGIN_WITH_TRUECALLER("Login With Truecaller V3"),
    LOGIN_WITH_OTP("Login With Otp V3"),

    //LOGIN with otp
    PHONENO_ENTERED_NEXT_BTN_CLICKED("Phone Number Entered Next Button Clicked V3"),
    COUNTRY_FLAG_CHANGED("CountryFlag Icon Changed V3"),
    FLAG_DIALOG_SEARCH_TEXT("Flag Searched V3"),
    FLAG_DIALOG_CLOSED("Flag Dialog Closed V3"),
    FLAG_DIALOG_ITEM_CLICKED("Flag Dialog Flag Selected V3"),
    HELP_INITIATED("Help Initiated V3"),
    TERMS_CONDITION_CLICKED("Terms&Conditions Clicked V3"),

    //New Login/SignUp Status Status
    SIGNUP_SATUS("SignUp Status V3"),
    TYPE_PARAM("Type param V3"),
    FLOW_FROM_PARAM("flow from V3"),
    MOBILE_OTP_PARAM("Via Mobile OTP V3"),
    TRUECALLER_PARAM("Via Truecaller V3"),
    STATUS("Status param V3"),
    USER_DETAILS("User Details V3"),
    SUCCESS_PARAM("Status success V3"),
    FAILED_PARAM("Status failed V3"),
    CANCELLED_PARAM("Status cancelled V3"),


    // TRUECALLER LOGIN
    TRRUECALLER_CONTINUE_CLICKED("Truecaller Continue Button Clicked V3"),
    USE_OTHER_MOBILE_CLICKED("Another Mobile Number Button Clicked V3"),

    // LOGIN FRAGMENTS

    RESEND_OTP("Resend OTP V3"),
    INCORRECT_OTP("Incorrect OTP V3"),
    NEXT_OTP_CLICKED("OTP Submit Status V3"),
    OTP_VERIFIED("OTP Verification Done V3"),
    VERIFIED_VIA_TRUECALLER("Verified via Truecaller V3"),
    NEXT_TO_OTP_SCREEN_CLICKED("Next To Otp Screen Clicked V3"),
    LOGIN_SUCCESS("Login Success V3"),
    LOGIN_SUCCESSFULLY("Login Successfully V3"),

    ENTER_OTP_SCREEN("Enter OTP Screen V3"),
    NO_OF_TIMES_OTP_SEND("Number of times OTP send V3"),
    TIME_TAKEN("Time Taken V3"),
    INCORRECT_OTP_ATTEMPTS("incorrect OTP attempts V3"),
    //ENTER_OTP_SCREEN("Enter OTP Screen V3"),


    //HELPLINE SELECTED
    CLICK_HELPLINE_SELECTED("Call Helpline Selected V3"),
    CALL_HELPLINE("Call Helpline V3"),
    HELP_SUBMITTED("Help Submitted V3"),
    HELP_BACK_CLICKED("Help back button clicked V3"),
    HELP_COMPLAINT_FOAM("Help Complain Foam Selected V3"),
    HELP_CATEGORY_CLICKED("Help Category V3"),


    //Complaint Details

    COMPLAINT_EMAIL("Complaint email V3"),
    COMPLAINT_NAME("Complaint name V3"),
    COMPLAINT_NUMBER("Complaint number V3"),
    COMPLAINT_TEXT("Complaint text V3"),
    COMPLAINT_IMAGE("Contains complaint image V3"),


    // ExploreCourses Screen

    EXPLORE_OPENED("Explore Opened V3"),
    COURSE_LIST_SCROLLED("Course List Scrolled V3"),
    COURSE_CLICKED("Course Clicked V3"),
    COURSE_THUMBNAIL_CLICKED("Course Thumbnail Clicked V3"),
    BACK_BTN_EXPLORESCREEN("Back Pressed on Explore Screen V3"),
    MORE_ICON_CLICKED("More Icons Clicked V3"),
    MENU_ICON_CLICKED("Menu Icons Clicked V3"),
    LOGOUT_CLICKED("Logout Menu item clicked V3"),
    USER_LOGGED_OUT("User logged out V3"),


    //


    //CourseDetails Screen

    COURSE_OVERVIEW("Course Overview V3"),
    COURSE_PRICE("Course price V3"),
    SHOWN_COURSE_PRICE("Shown course price V3"),
    COURSE_NAME("Course Name V3"),
    COURSE_ID("Course ID V3"),
    BUY_NOW_SELECTED("Buy Now Selected V3"),
    PAYMENT_DIALOG("Payment Dialog V3"),
    COUPON_SELECTED("Coupon Selected V3"),
    COUPON_INSERTED("Coupon Inserted V3"),
    COUPON_APPLY_CLICKED("Coupon Appl btn clicked V3"),
    COUPON_VALID("Coupon Valid V3"),
    COUPON_INVALID("Coupon Invalid V3"),
    SPECIAL_DISCOUNT("Special Discount V3"),
    COUSR_VIDEO_PLAYED("Course Video Played V3"),
    COUSR_VIDEO_PRESENT("Course Video Present V3"),

    COURSE_DETAILS("Clicked Course Details V3"),
    COMPLETE_PAYMENT("Complete Payment V3"), //
    COMPLETE_PAYMENT_CLICKED("Complete Payment Btn Clicked V3"),
    START_COURSE_NOW("Start your course now V3"), //  COURSE_CLICKED same


    //Course Purchase Initiated
    COURSE_PURCHASE_INITIATED("Course purchase initiated V3"),

    // Course Purchase Confirmed

    ENTER_COUPON_SCREEN("Enter coupon Screen V3"),
    COURSE_PURCHASE_CONFIRMED("Course Purchase Confirmed V3"),

    //  Payment Status (Post response from razorpay, transaction was successful or failed)

    PAYMENT_STATUS("Payment Status V3"),
    INVALID_COUPON_POPUP("Invalid coupon popup V3"),


    // Audio Downloaded

    AUDIO_VH("Audio View Holder V3"),
    AUDIO_PLAYER_PLAYED("Audio player Played V3"),
    AUDIO_DOWNLOAD_STATUS("Audio Download Status V3"),
    AUDIO_VIEW_STATUS("Audio View Status V3"),
    AUDIO_PLAYING_STATUS("Audio playing Status V3"),
    AUDIO_ID("Audio id V3"),
    AUDIO_DURATION("Audio Duration V3"),
    TIME_TAKEN_DOWNLOAD("Time taken to download V3"),

    // Video Played and downloaded
    VIDEO_VH("Video View Holder V3"),
    VIDEO_PLAYER_PLAYED("Video player Played V3"),
    VIDEO_PLAYING_STATUS("Video playing Status V3"),
    VIDEO_DOWNLOAD_STATUS("Video Download Status V3"),
    VIDEO_VIEW_STATUS("Video View Status V3"),
    VIDEO_ID("Video id V3"),
    VIDEO_DURATION("Video Duration V3"),
    VIDEO_PAUSE("Video pause V3"),
    VIDEO_BTM_BTN("Video bottom Button V3"),
    VIDEO_PLAY("Video play V3"),
    VIDEO_REWIND("Video rewind V3"),
    VIDEO_FORWARD("Video forward V3"),
    VIDEO_MORE("Video more selected V3"),

    // PDF Events
    PDF_VH("pdf View Holder V3"),
    PDF_PLAYER_PLAYED("pdf player Played V3"),
    PDF_PLAYING_STATUS("pdf playing Status V3"),
    PDF_DOWNLOAD_STATUS("pdf Download Status V3"),
    PDF_VIEW_STATUS("pdf View Status V3"),
    PDF_ID("pdf id V3"),
    PDF_DURATION("pdf Duration V3"),

    // Practice Opened
    PRACTISE_SCREEN("Practise Screen V3"),
    PRACTISE_ID("Practise id V3"),
    PRACTISE_OPENED("Practise open clicked V3"),
    PRACTICE_SUBMITTED("Practice submitted V3"),
    PRACTICE_TYPE_SUBMITTED("Practice type submitted V3"),
    AUDIO_RECORD("Audio Record V3"),
    PRACTICE_SOLVED("Practise Solved V3"),
    PRACTICE_STATUS("Practise Status V3"),
    PRACTICE_EXTRA("Practise extra action V3"),
    PRACTICE_TYPE_PRESENT("Practise type Present V3"),
    PRACTICE_SCREEN_TIME("Practise opened to  submit time v3"),
    AUDIO_SUBMITTED("Audio Submitted V3"),


    //Chat Entered
    CHAT_ENTERED("Chat entered V3"),
    CHAT_TEXT("Chat text V3"),
    CHAT_LENGTH("Chat text length V3"),

    //Certificates
    CERTIFICATE_SCREEN("CERTIFICATE Screen V3"),
    CERTIFICATE_PROGRESS_CLICKED("Certificate Progress Clicked V3"),
    PERFORMANCE_CLICKED("Performance Clicked V3"),
    VIEW_SAMPLE_CERTIFICATE_OPEN("View Sample Certificate Open V3"),
    VIEW_SAMPLE_CERTIFICATE_CLOSE("View Sample Certificate Close V3"),
    VIDEO_CLICKED_COURSE_OVERVIEW("Video Click Course Overview V3"),
    PRACTISE_CLICKED_COURSE_OVERVIEW("Practise Clicked Course Overview V3"),
    CLAIM_CERTIFICATE("Certifcate claim clicked V3"),

    // FIND more Course

    FIND_MORE_COURSE_CLICKED("Find more course clicked V3"),

    INBOX_SCREEN("Inbox Screen V3"),
    COURSE_SELECTED("Course Selected V3"),
    AUDIO_BUTTON_CLICKED("Audio Button clicked V3"),
    AUDIO_SENT("Audio Sent V3"),
    AUDIO_CANCELLED("Audio Cancelled V3"),
    CAMERA_CLICKED("Camera Clicked V3"),
    ATTACHMENT_CLICKED("Attachment clicked V3"),
    AUDIO_SELECTED("Audio Selected V3"),
    CAMERA_SELECTED("Camera Selected V3"),
    GALLERY_SELECTED("Gallery Selected V3"),
    MEDIA_DOWNLOAD("Media Download V3"),
    VIDEO_WATCH_ACTIVITY("Video Watch Activity V3"),

    AUDIO_PLAYED("Audio Played V3"),

    EMOJI_CLICKED("Emoji Clicked V3"),

    PDF_OPENED("PDF Opened V3"),
    COURSE_OPENED("Course Opened V3"), // conversation Activity opened
    PROFILE_IMAGE_UPLOAD("Profile Image Upload V3"),
    MESSAGE_SENT_AUDIO("Message Sent Audio V3"),
    MESSAGE_SENT_TEXT("Message Sent Text V3"),
    MESSAGE_SENT_VIDEO("Message Sent Video V3"),
    MESSAGE_SENT_IMAGE("Message Sent Image V3"),

    AUDIO_OPENED("Audio Opened V3"),
    AUDIO_DOWNLOAD("Audio Download V3"),
    IMAGE_OPENED("Image Opened V3"),
    IMAGE_DOWNLOAD("Image Download V3"),
    PDF_DOWNLOAD("PDF Download V3"),
    VIDEO_OPENED("Video Opened V3"),
    VIDEO_DOWNLOAD("Video Download V3"),

    NAME_ENTER("Name Enter V3"),
    DOB_SELECTED("DOB Selected V3"),
    IMAGE_UPLOAD("Image Upload V3"),

    API_FAILED("Api Failed V3"),

    PERMISSION_ACCEPT("Permission accept V3"),
    PERMISSION_DENIED("Permission accept V3"),
    NOTIFICATION_RECEIVED("Notification Received V3"),
    NOTIFICATION_CLICKED("Notification Clicked V3"),
    REGISTRATION_COMPLETED("Registration_completed V3"),
    COURSE_STARTED("Course_Started V3"),
    PURCHASE_COURSE("Purchase Course V3"),


    TEST_ID_OPENED("Test ID Opened V3"),

    RAZORPAY_SDK("Razorpay SDK V3"),
    PAYMENT_COMPLETED("Payment Completed V3"),
    PAYMENT_INITIATED("Payment Initiated V3"),
    PAYMENT_FAILED("Payment Fail V3"),

    VIDEO_SUBMITTED("Video Submitted V3"),
    TEXT_SUBMITTED("Text Submitted V3"),
    DOCUMENT_SUBMITTED("Document Submitted V3"),
    REFER_BUTTON_CLICKED("Refer Button Clicked V3"),
    CODE_COPIED("Code Copied V3"),
    SHARE_ON_WHATSAPP("Share On Whatsapp V3"),
    SHARE_ON_ALL("Share On All V3"),

    HAVE_COUPON_CODE_CLICKED("Have Coupon Code Clicked V3"),
    HAVE_COUPON_CODE("Have Coupon Code V3"),
    COURSE_PROGRESS_OVERVIEW("Course progress Overview V3"),// CourseProgressListingScreen
    FEEDBACK_INITIATED("Feedback Initiated V3"),
    FEEDBACK_SUBMITTED("Feedback Submitted V3"),
    FEEDBACK_IGNORE("Feedback Ignore V3"),

    REFERRAL_CODE("Referral Code V3"),

}
