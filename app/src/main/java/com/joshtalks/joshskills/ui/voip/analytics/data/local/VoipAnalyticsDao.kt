package com.joshtalks.joshskills.ui.voip.analytics.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Dao
interface VoipAnalyticsDao {
    @Insert
    suspend fun saveAnalytics(data: VoipAnalyticsEntity)

    @Transaction
    @Query("SELECT * from voip_analytics")
    suspend fun getAnalytics(): List<VoipAnalyticsEntity>

    @Transaction
    @Query("DELETE from voip_analytics WHERE id =:id")
    suspend fun deleteAnalytics(id: Int)
}

fun main() {
    /*CoroutineScope(Dispatchers.IO).launch {
        delay(2000)
        SyncTesting.start(100)
    }

    Thread {
        CoroutineScope(Dispatchers.IO).launch {
            delay(3000)
            SyncTesting.start(200)
        }
    }.start()
    for(i in 1..5) {
        //CoroutineScope(Dispatchers.IO).launch {
            //print("START - $i \n")
            SyncTesting.start(i)
            //print("END - $i \n")
        //}
    }*/
    coroutineTesting()
    runBlocking {
        delay(10000)
    }
}

fun coroutineTesting() {
    println("coroutineTesting -- START")
    CoroutineScope(Dispatchers.IO).launch {
        println("START -- A")
        a()
        println("START -- B")
        b()
    }
    println("coroutineTesting -- END")
}

suspend fun a() {
    delay(5000)
    println("A()")
}

suspend fun b() {
    delay(1000)
    println("B()")
}

object SyncTesting {
    val mutex = Mutex()
    val parentJob = Job()
    //val scope = CoroutineScope()

    fun start(value: Int) {
        //print("Start Method Called --> $value \n")
        CoroutineScope(Dispatchers.IO).launch {
            mutex.withLock {
                print("STARTING CODE RUN - $value \n")
                //Log.d("TAG", "start: ")
                print("Waiting for delay to finish - $value \n")
                delay(1000)
                print("Waiting for delay to finish - $value \n")
                //delay(5000)
                print("ENDING CODE RUN - $value \n")
            }
        }
    }
}