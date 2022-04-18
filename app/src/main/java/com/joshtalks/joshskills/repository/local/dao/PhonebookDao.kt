package com.joshtalks.joshskills.repository.local.dao

import androidx.room.*
import com.joshtalks.joshskills.repository.local.entity.PhonebookContact

@Dao
interface PhonebookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: PhonebookContact)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(contacts: List<PhonebookContact>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(contact: PhonebookContact)

    @Query("SELECT * FROM `phonebook_contacts`")
    suspend fun getAll(): List<PhonebookContact>

    @Query("SELECT * FROM `phonebook_contacts` WHERE isSynchronized = 0")
    suspend fun getAllUnsynchronized(): List<PhonebookContact>

    @Query("UPDATE `phonebook_contacts` SET isSynchronized = 1 WHERE id IN (:ids)")
    suspend fun updateSyncStatus(ids: List<String>)
}