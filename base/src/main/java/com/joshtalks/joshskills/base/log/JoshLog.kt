package com.joshtalks.joshskills.base.log

import android.app.Application
import android.os.Handler
import android.os.Looper

/**
 * TODO: Will be improved
 */

class JoshLog private constructor(private val feature: Feature) {
    val handler =  Handler(Looper.getMainLooper())
    fun log(msg: String) {
        val stack = Throwable().fillInStackTrace()
        val trace = stack.stackTrace
        val classname = trace[1].className
        val methodName = trace[1].methodName
        val number = trace[1].lineNumber
        handler.post {
            println("${feature.tag} [$classname : $methodName : $number] : ${msg}")
        }
    }

//    fun log(msg: String) {
//        val stack = Throwable().fillInStackTrace()
//        val trace = stack.stackTrace
//        val classname = trace[1].className
//        Log.d(feature.tag, "$classname : ${msg}")
//    }

    companion object {
        private val objectHolder = mutableMapOf<Feature, JoshLog>()
        private val enabledFeatureSet = mutableSetOf<Feature>()

        fun Application.enableLog(feature: Feature) {
            synchronized(this) {
                enabledFeatureSet.add(feature)
            }
        }

        fun getInstanceIfEnable(feature: Feature) : JoshLog? {
            if(feature in enabledFeatureSet) {
                val joshLog = objectHolder[feature]
                return joshLog ?: synchronized(this) {
                    if (joshLog == null) {
                        val newJoshLog = JoshLog(feature)
                        objectHolder[feature] = newJoshLog
                        newJoshLog
                    } else
                        joshLog
                }
            } else
                return null
        }
    }
}

enum class Feature(val tag : String) {
    VOIP("New Voip Arch"),
    APP("Main Application VOIP")
}

// VOIP