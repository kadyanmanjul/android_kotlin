package com.joshtalks.joshskills.core.analytics

import android.content.Context
import android.os.Bundle
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.AppEventsLogger
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.JoshSkillExecutors
import com.joshtalks.joshskills.core.RegistrationMethods
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.repository.server.CourseExploreModel
import com.joshtalks.joshskills.repository.server.OrderDetailResponse
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.util.*
import java.math.BigDecimal
import java.util.*


object MarketingAnalytics {

    fun completeRegistrationAnalytics(
        userExist: Boolean,
        registrationMethod: RegistrationMethods
    ) {
        JoshSkillExecutors.BOUNDED.submit {
            if (userExist) {
                BranchIOAnalytics.pushToBranch(BRANCH_STANDARD_EVENT.LOGIN)
            } else {
                val params = Bundle().apply {
                    putString(
                        AppEventsConstants.EVENT_PARAM_REGISTRATION_METHOD,
                        registrationMethod.type
                    )
                    putString(
                        AppEventsConstants.EVENT_PARAM_SUCCESS,
                        AppEventsConstants.EVENT_PARAM_VALUE_YES
                    )
                    putString(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
                    putString(AppEventsConstants.EVENT_PARAM_CURRENCY, CurrencyType.INR.name)
                }
                AppObjectController.facebookEventLogger.logEvent(
                    AppEventsConstants.EVENT_NAME_COMPLETED_REGISTRATION,
                    params
                )
                AppAnalytics.create(AppEventsConstants.EVENT_NAME_COMPLETED_REGISTRATION).addDeviceId().push()
                BranchEvent(BRANCH_STANDARD_EVENT.COMPLETE_REGISTRATION).addCustomDataProperty(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
                BranchIOAnalytics.pushToBranch(BRANCH_STANDARD_EVENT.COMPLETE_REGISTRATION)
            }
        }
    }

    fun callComplete20Min() {
        JoshSkillExecutors.BOUNDED.submit {
            val context = AppObjectController.joshApplication
            val params = Bundle().apply {
                putString(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
            }
            val facebookEventLogger = AppEventsLogger.newLogger(context)
            facebookEventLogger.logEvent(AppEventsConstants.EVENT_NAME_VIEWED_CONTENT,params)

            BranchEvent(BRANCH_STANDARD_EVENT.VIEW_ITEM.name)
                .setDescription(BranchEventName.CALL_COMPLETED_20MIN.name)
                .addCustomDataProperty(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
                .logEvent(context)

            AppAnalytics.create(BRANCH_STANDARD_EVENT.VIEW_ITEM.name).addDeviceId().push()
        }
    }

    fun initPurchaseEvent(
        data: MutableMap<String, String>,
        mPaymentDetailsResponse: OrderDetailResponse
    ) {
        JoshSkillExecutors.BOUNDED.submit {

            val context = AppObjectController.joshApplication
            val testId = data["test_id"]

            //fb init event
            val params = Bundle()
            //params.putString(AppEventsConstants.EVENT_PARAM_CONTENT, mPaymentDetailsResponse.email)
            params.putString(AppEventsConstants.EVENT_PARAM_CONTENT_ID, testId)
            params.putString(AppEventsConstants.EVENT_PARAM_CONTENT_TYPE, "E-learning")
            params.putInt(AppEventsConstants.EVENT_PARAM_NUM_ITEMS, 1)
            params.putInt(AppEventsConstants.EVENT_PARAM_PAYMENT_INFO_AVAILABLE, 1)
            params.putString(
                AppEventsConstants.EVENT_PARAM_CURRENCY,
                mPaymentDetailsResponse.currency
            )
            params.putString(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
            val facebookEventLogger = AppEventsLogger.newLogger(context)
            facebookEventLogger.logEvent(
                AppEventsConstants.EVENT_NAME_INITIATED_CHECKOUT,
                mPaymentDetailsResponse.amount,
                params
            )

            // branch init event
            val buo = BranchUniversalObject()
                .setCanonicalIdentifier(testId.toString())
                .setTitle("joshskills")
                .setContentMetadata(
                    ContentMetadata()
                        .setPrice(mPaymentDetailsResponse.amount, CurrencyType.INR)
                        .setQuantity(1.0)
                        .setContentSchema(BranchContentSchema.COMMERCE_PRODUCT)
                )

            BranchEvent(BRANCH_STANDARD_EVENT.INITIATE_PURCHASE)
                .setCurrency(CurrencyType.INR)
                .setDescription("Customer init purchase ")
                .addCustomDataProperty(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
                .addContentItems(buo)
                .logEvent(context)
        }
    }

    fun callComplete5Min() {
        JoshSkillExecutors.BOUNDED.submit {
            val context = AppObjectController.joshApplication
            val params = Bundle().apply {
                putString(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
            }
            val facebookEventLogger = AppEventsLogger.newLogger(context)
            facebookEventLogger.logEvent(AppEventsConstants.EVENT_NAME_CONTACT,params)

            BranchEvent(AppEventsConstants.EVENT_NAME_CONTACT)
                .setDescription(BranchEventName.CALL_COMPLETED_5MIN.name)
                .addCustomDataProperty(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
                .logEvent(context)

            AppAnalytics.create(AppEventsConstants.EVENT_NAME_CONTACT).addDeviceId().push()
        }
    }

    fun openInboxPage() {
        val context = AppObjectController.joshApplication
        // Facebook Event
        AppEventsLogger.activateApp(context)
        val params = Bundle().apply {
            putString(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
        }
        val facebookEventLogger = AppEventsLogger.newLogger(context)
        facebookEventLogger.logEvent(AppEventsConstants.EVENT_NAME_SCHEDULE,params)

        // Branch Events
        BranchEvent(AppEventsConstants.EVENT_NAME_SCHEDULE)
            .setCustomerEventAlias("inbox_screen")
            .addCustomDataProperty(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
            .logEvent(context)

        // Firebase Events
        AppAnalytics.create(AppEventsConstants.EVENT_NAME_SCHEDULE)
            .addBasicParam()
            .addUserDetails()
            .push()
    }

    fun coursePurchased(amount : BigDecimal, logFacebook: Boolean = false) {
        val context = AppObjectController.joshApplication
        val params = Bundle().apply {
            putString(AppEventsConstants.EVENT_PARAM_CURRENCY, CurrencyType.INR.name)
            putString(AppEventsConstants.EVENT_PARAM_CONTENT, "Course")
            putString(AppEventsConstants.EVENT_PARAM_CONTENT_TYPE, "E-learning")
            putInt(AppEventsConstants.EVENT_PARAM_NUM_ITEMS, 1)
            putString(
                AppEventsConstants.EVENT_PARAM_SUCCESS,
                AppEventsConstants.EVENT_PARAM_VALUE_YES
            )
            putString(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
        }

        // Facebook Event
        if (logFacebook) {
            AppEventsLogger.activateApp(context)
            val facebookEventLogger = AppEventsLogger.newLogger(context)
            facebookEventLogger.logPurchase(
                amount,
                Currency.getInstance(CurrencyType.INR.name),
                params
            )
        }

        // Firebase Events
        AppAnalytics.create(BRANCH_STANDARD_EVENT.PURCHASE.name)
            .addBasicParam()
            .addUserDetails()
            .push()
    }

    fun logAchievementLevelEvent(achievementLevel: Int) {
        JoshSkillExecutors.BOUNDED.submit {
            val context = AppObjectController.joshApplication

            val params = Bundle()
            params.putString(AppEventsConstants.EVENT_PARAM_LEVEL, achievementLevel.toString())
            params.putString(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
            val facebookEventLogger = AppEventsLogger.newLogger(context)
            facebookEventLogger.logEvent(AppEventsConstants.EVENT_NAME_ACHIEVED_LEVEL, params)

            BranchEvent(BRANCH_STANDARD_EVENT.ACHIEVE_LEVEL)
                .setCustomerEventAlias("achieve_level")
                .addCustomDataProperty("level", achievementLevel.toString())
                .logEvent(context)

            AppAnalytics.create(BRANCH_STANDARD_EVENT.ACHIEVE_LEVEL.name)
                .addBasicParam()
                .addUserDetails()
                .addParam("achieve_level", achievementLevel)
                .push()
        }
    }

    fun logLessonCompletedEvent(lessonNumber: Int,lessonId: Int) {
        MixPanelTracker.publishEvent(MixPanelEvent.LESSON_COMPLETE)
            .addParam(ParamKeys.LESSON_ID,lessonId)
            .addParam(ParamKeys.LESSON_NUMBER,lessonNumber)
            .push()

        JoshSkillExecutors.BOUNDED.submit {
            val context = AppObjectController.joshApplication
            val params = Bundle().apply {
                putString(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
            }
            val facebookEventLogger = AppEventsLogger.newLogger(context)
            facebookEventLogger.logEvent(AppEventsConstants.EVENT_NAME_SUBMIT_APPLICATION,params)

            BranchEvent(BRANCH_STANDARD_EVENT.COMPLETE_TUTORIAL)
                .setDescription("User has completed his lesson")
                .addCustomDataProperty("lesson_number",lessonNumber.toString())
                .addCustomDataProperty(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
                .logEvent(context)

            AppAnalytics.create(AnalyticsEvent.LESSON_COMPLETED.name)
                .addBasicParam()
                .addUserDetails()
                .addParam("lesson_number", lessonNumber)
                .addDeviceId()
                .push()
        }
    }

    fun callInitiated() {
        JoshSkillExecutors.BOUNDED.submit {
            val context = AppObjectController.joshApplication
            val params = Bundle().apply {
                putString(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
            }
            val facebookEventLogger = AppEventsLogger.newLogger(context)
            facebookEventLogger.logEvent(AppEventsConstants.EVENT_PARAM_SEARCH_STRING,params)

            BranchEvent(AppEventsConstants.EVENT_PARAM_SEARCH_STRING)
                .setDescription(BranchEventName.CALL_INITIATED.name)
                .addCustomDataProperty(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
                .logEvent(context)

            AppAnalytics.create(AppEventsConstants.EVENT_PARAM_SEARCH_STRING)
                .addBasicParam()
                .addUserDetails()
                .addDeviceId()
                .push()
        }
    }

    fun openAppFirstTime(){
        JoshSkillExecutors.BOUNDED.submit {
            val context = AppObjectController.joshApplication
            val params = Bundle().apply {
                putString(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
            }
            val facebookEventLogger = AppEventsLogger.newLogger(context)
            facebookEventLogger.logEvent(AppEventsConstants.EVENT_NAME_ACTIVATED_APP,params)

            BranchEvent(AppEventsConstants.EVENT_NAME_ACTIVATED_APP)
                .setDescription(BranchEventName.APP_OPENED_FIRST_TIME.name)
                .addCustomDataProperty(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
                .logEvent(AppObjectController.joshApplication)

            AppAnalytics.create(AppEventsConstants.EVENT_NAME_ACTIVATED_APP)
                .addBasicParam()
                .addUserDetails()
                .addDeviceId()
                .push()
        }
    }

    fun logSpeakingSectionCompleted() {
        JoshSkillExecutors.BOUNDED.submit {
            val context = AppObjectController.joshApplication
            val params = Bundle().apply {
                putString(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
            }
            val facebookEventLogger = AppEventsLogger.newLogger(context)
            facebookEventLogger.logEvent(AppEventsConstants.EVENT_NAME_CUSTOMIZE_PRODUCT,params)

            BranchEvent(AppEventsConstants.EVENT_NAME_CUSTOMIZE_PRODUCT)
                .setDescription(BranchEventName.SPEAKING_COMPLETED.name)
                .addCustomDataProperty(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
                .logEvent(context)

            AppAnalytics.create(AppEventsConstants.EVENT_NAME_CUSTOMIZE_PRODUCT)
                .addBasicParam()
                .addUserDetails()
                .addDeviceId()
                .push()

            AppAnalytics.create(BranchEventName.SPEAKING_COMPLETED.name).addDeviceId().push()
        }
    }

    fun openPreCheckoutPage() {
        JoshSkillExecutors.BOUNDED.submit {
            val context = AppObjectController.joshApplication
            val params = Bundle().apply {
                putString(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
            }
            val facebookEventLogger = AppEventsLogger.newLogger(context)
            facebookEventLogger.logEvent(AppEventsConstants.EVENT_NAME_DONATE,params)

            BranchEvent(AppEventsConstants.EVENT_NAME_DONATE)
                .setDescription(BranchEventName.OPENED_PRE_CHECKOUT_PAGE.name)
                .addCustomDataProperty(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
                .logEvent(context)

            AppAnalytics.create(AppEventsConstants.EVENT_NAME_DONATE)
                .addBasicParam()
                .addUserDetails()
                .addDeviceId()
                .push()
        }
    }


    fun paymentFail(razorpayOrderId: String, testId: String) {
        JoshSkillExecutors.BOUNDED.submit {
            val context = AppObjectController.joshApplication
            val params = Bundle().apply {
                putString(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
            }
            val facebookEventLogger = AppEventsLogger.newLogger(context)
            facebookEventLogger.logEvent(BRANCH_STANDARD_EVENT.SEARCH.name,params)

            BranchEvent(BRANCH_STANDARD_EVENT.SEARCH)
                .setDescription(BranchEventName.PAYMENT_FAILED.name)
                .addCustomDataProperty(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
                .addCustomDataProperty(ParamKeys.TEST_ID.name,testId)
                .addCustomDataProperty("RAZORPAY_ORDER_ID",razorpayOrderId)
                .logEvent(context)

            AppAnalytics.create(BRANCH_STANDARD_EVENT.SEARCH.name).addDeviceId().push()
        }
    }

    fun viewContentEvent(context: Context, courseExploreModel: CourseExploreModel) {
        JoshSkillExecutors.BOUNDED.submit {
            //Fb view event
                val params = Bundle()
                params.putString(AppEventsConstants.EVENT_PARAM_CONTENT_TYPE, "E-learning")
                params.putString(AppEventsConstants.EVENT_PARAM_CONTENT, courseExploreModel.toString())
                params.putString(
                    AppEventsConstants.EVENT_PARAM_CONTENT_ID,
                    courseExploreModel.id?.toString()
                )
                params.putString(AppEventsConstants.EVENT_PARAM_CURRENCY, CurrencyType.INR.name)
                params.putString(
                    ParamKeys.DEVICE_ID.name,
                    Utils.getDeviceId()
                )
                val facebookEventLogger = AppEventsLogger.newLogger(context)
                facebookEventLogger.logEvent(BRANCH_STANDARD_EVENT.VIEW_CART.name, params)

            //Branch view event
            val buo = BranchUniversalObject()
                .setCanonicalIdentifier(courseExploreModel.id?.toString() ?: "")
                .setTitle(courseExploreModel.courseName)
                .setContentDescription(courseExploreModel.testName)
                .setContentImageUrl(courseExploreModel.imageUrl)
                .setContentIndexingMode(BranchUniversalObject.CONTENT_INDEX_MODE.PUBLIC)
                .setLocalIndexMode(BranchUniversalObject.CONTENT_INDEX_MODE.PUBLIC)
            BranchEvent(BRANCH_STANDARD_EVENT.VIEW_CART).addContentItems(buo).logEvent(context)
        }
    }
}
