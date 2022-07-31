package com.joshtalks.joshskills.base.storage.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FavoriteCallerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteCaller(favoriteCaller: FavoriteCaller)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteCallers(favoriteCallerList: List<FavoriteCaller>): List<Long>

    @Query("UPDATE favorite_caller SET is_deleted =1  WHERE id IN (:ids)")
    suspend fun updateFavoriteCallerStatus(ids: List<Int>)

    @Query("DELETE FROM  favorite_caller WHERE id IN (:ids)")
    suspend fun removeFromFavorite(ids: List<Int>)

    @Query("DELETE FROM  favorite_caller WHERE id=:id")
    suspend fun removeFppUser(id: String)

    @Query(value = "SELECT * from favorite_caller WHERE is_deleted=0 ")
    fun getFavoriteCallers(): List<FavoriteCaller>

    @Query(value = "SELECT * from favorite_caller  WHERE id=:id")
    fun getFavoriteCaller(id: Int): FavoriteCaller?

    @Query(value = "SELECT id from favorite_caller WHERE is_deleted=1 ")
    fun getRemoveFromFavoriteCallers(): List<Int>

    @Query(value = "SELECT COUNT(id) FROM favorite_caller  ")
    suspend fun getCountOfFavoriteCaller(): Long

    @Query("DELETE FROM  favorite_caller")
    suspend fun removeAllFavorite()
}