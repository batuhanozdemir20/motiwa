package com.batuhanozdemir.motiwa.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Motiwa::class],version = 1)
abstract class MotiwaDatabase: RoomDatabase() { abstract fun motiwaDao(): MotiwaDAO }