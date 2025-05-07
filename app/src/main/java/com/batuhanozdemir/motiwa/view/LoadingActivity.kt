package com.batuhanozdemir.motiwa.view

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.batuhanozdemir.motiwa.R
import com.batuhanozdemir.motiwa.room.Motiwa
import com.batuhanozdemir.motiwa.room.MotiwaDAO
import com.batuhanozdemir.motiwa.room.MotiwaDatabase
import com.batuhanozdemir.motiwa.worker.CHANNEL_ID
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException

class LoadingActivity : AppCompatActivity() {
    private lateinit var db: MotiwaDatabase
    private lateinit var dao: MotiwaDAO
    private val handler: Handler = Handler(Looper.getMainLooper())
    private var delay: Long = 5000  //ms

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_loading)
        createNotificationChannel()
        db = Room.databaseBuilder(applicationContext,MotiwaDatabase::class.java,"motiwa_database").build()
        dao = db.motiwaDao()

        val sharedPref = getSharedPreferences("com.batuhanozdemir.motiwa", MODE_PRIVATE)
        val firstRun = sharedPref.getBoolean("firstRun",true)

        if (firstRun){
            sharedPref.edit().clear().apply()
            getData()
            CoroutineScope(Dispatchers.IO).launch{
                delay(3000)
                val motiwa = dao.getRandomTexts("general")

                motiwa?.let {
                    it.used = true
                    dao.updateText(it)
                    sharedPref.edit().putInt("dailyTextID",it.id).apply()
                }
            }
        }else{
            delay = 2000  //ms
        }

        CoroutineScope(Dispatchers.IO).launch { MobileAds.initialize(this@LoadingActivity) }

        handler.postDelayed(Runnable {
            val loading = Intent(this@LoadingActivity,MainActivity::class.java)
            startActivity(loading)
            finish()
        },delay)
    }

    private fun getData(){
        CoroutineScope(Dispatchers.IO).launch{
            try {
                val assetsFile = applicationContext.assets.list("")?.filter { it.endsWith(".txt") }
                assetsFile?.let {
                    it.forEach { asset ->
                        val textFile = readTextFromFile(applicationContext, asset)
                        val lines = textFile.lines()
                        val category = asset.substring(0, asset.indexOf("."))
                        for (line in lines){
                            dao.addText(Motiwa(text = line, used = false, favorite = false, category = category))
                        }
                    }
                }
            }catch (e: IOException){
                println("Dosya bulunamadı.")
            }
        }
    }

    private fun readTextFromFile(context: Context, fileName: String): String {
        return try {
            context.assets.open(fileName).bufferedReader().use {
                it.readText()
            }
        }catch (e: IOException){
            "Error: File could not be read."
        }
    }

    private fun createNotificationChannel(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel(CHANNEL_ID,"Motiwa Bildirim Kanali", NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = "Bildirim kanalı açıklaması"

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}