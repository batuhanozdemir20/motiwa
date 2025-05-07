package com.batuhanozdemir.motiwa.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Motiwa(
    @ColumnInfo("text") val text: String,
    @ColumnInfo("used") var used: Boolean = false,
    @ColumnInfo("favorite") var favorite: Boolean = false,
    @ColumnInfo("category") val category: String
){  @PrimaryKey(autoGenerate = true) var id = 0 }