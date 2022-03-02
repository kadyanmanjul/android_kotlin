package com.joshtalks.joshskills.ui.call.lib

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AgoraCallingServiceTest {

    @Test
    fun initCallingService() {
        runBlocking {
            val obj1 = AgoraCallingService.getAgoraEngine()
            println(obj1)
            AgoraCallingService.disconnectCall()
            delay(5000)
            assertEquals(obj1, AgoraCallingService.getAgoraEngine())
        }
    }

    @Test
    fun connectCall() {
    }

    @Test
    fun getAgoraEngine() {
    }

    @Test
    fun disconnectCall() {
    }

    @Test
    fun observeCallingEvents() {
    }
}