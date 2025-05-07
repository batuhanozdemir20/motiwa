package com.batuhanozdemir.motiwa.worker

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.room.Room
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.batuhanozdemir.motiwa.R
import com.batuhanozdemir.motiwa.room.MotiwaDatabase
import com.batuhanozdemir.motiwa.view.LoadingActivity

const val CHANNEL_ID = "motiwa_notification_channel"

class ShowNotification(val context: Context, workerParams: WorkerParameters): Worker(context, workerParams) {
    private val db = Room.databaseBuilder(context, MotiwaDatabase::class.java,"motiwa_database").build()
    private val dao = db.motiwaDao()
    private val sharedPref: SharedPreferences = context.getSharedPreferences("com.batuhanozdemir.motiwa",MODE_PRIVATE)

    override fun doWork(): Result {
        val firstRun = sharedPref.getBoolean("firstRun",true)

        if (!firstRun){
            val categories = arrayListOf("general")
            val text_files = sharedPref.getStringSet("text_files",null)
            text_files?.let {
                for (ctgry in it){
                    categories.add(ctgry)
                }
            }
            val category = categories.random()
            var motiwa = dao.getRandomTexts(category)
            if (motiwa != null){
                showNotification(motiwa.text)
                motiwa.used = true
                dao.updateText(motiwa)
                sharedPref.edit().putInt("dailyTextID",motiwa.id).apply()
            }else{
                dao.setAllUnused()
                motiwa = dao.getRandomTexts(category)
                showNotification(motiwa!!.text)
                motiwa.used = true
                dao.updateText(motiwa)
                sharedPref.edit().putInt("dailyTextID",motiwa.id).apply()
            }
        } else {
            sharedPref.edit().putBoolean("firstRun",false).apply()
        }

        sharedPref.edit().putInt("changeCount",4).apply()

        return Result.success()
    }

    private fun showNotification(text: String){
        val intent = Intent(context,LoadingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context,0,intent,PendingIntent.FLAG_MUTABLE)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_emoji_emotions_24)
            .setContentTitle("Motiwa")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)){
            if (
                ActivityCompat.checkSelfPermission(
                    applicationContext,Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ){
                return
            }
            if (sharedPref.getBoolean("notification",true)){ notify(1,builder.build()) }
        }
    }
}