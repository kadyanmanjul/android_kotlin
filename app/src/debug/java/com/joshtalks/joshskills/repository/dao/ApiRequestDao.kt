package com.joshtalks.joshskills.repository.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.joshtalks.joshskills.repository.entity.ApiRequest

@Dao
interface ApiRequestDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ApiRequest)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(list: List<ApiRequest>)

    @Query("SELECT * FROM ApiRequest")
    fun getAll(): LiveData<List<ApiRequest>>

    @Query("SELECT * FROM ApiRequest ORDER BY time DESC LIMIT 5")
    fun getLatest(): List<ApiRequest>

    @Query("SELECT * FROM ApiRequest WHERE id = :id")
    fun getById(id: Long): LiveData<ApiRequest>

    @Delete
    suspend fun delete(entity: ApiRequest)

    @Query("DELETE FROM ApiRequest")
    suspend fun deleteAll()

}