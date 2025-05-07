package com.batuhanozdemir.motiwa.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MotiwaDAO {
    @Insert
    fun addText(text: Motiwa)

    @Update
    fun updateText(text: Motiwa)

    @Query("SELECT * FROM Motiwa WHERE used = 0")
    fun getTexts(): Flow<List<Motiwa>>

    @Query("SELECT * FROM Motiwa WHERE used = 1")
    fun getUsedTexts(): Flow<List<Motiwa>>

    @Query("SELECT * FROM Motiwa WHERE favorite = 1")
    fun getFavoriteAffirmations(): Flow<List<Motiwa>>

    @Query("SELECT * FROM Motiwa")
    fun getAllTexts(): Flow<List<Motiwa>>

    @Query("SELECT * FROM Motiwa WHERE used = 0 AND category =:category ORDER BY RANDOM() LIMIT 1")
    fun getRandomTexts(category: String): Motiwa?

    @Query("SELECT * FROM Motiwa WHERE id =:motiwaID ")
    fun getMotiwa(motiwaID: Int): Motiwa

    @Query("UPDATE Motiwa SET used = 0 WHERE used = 1")
    fun setAllUnused()
}