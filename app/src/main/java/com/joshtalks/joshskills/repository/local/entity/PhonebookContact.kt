package com.joshtalks.joshskills.repository.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "phonebook_contacts",
    indices = [Index(value = ["phoneNumber"], unique = true)]
)
data class PhonebookContact(
    @PrimaryKey
    val id: String,
    val name: String,
    val phoneNumber: String,
    val isSynchronized: Boolean = false
)