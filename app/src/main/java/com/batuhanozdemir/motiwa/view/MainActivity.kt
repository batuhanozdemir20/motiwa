package com.batuhanozdemir.motiwa.view

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.batuhanozdemir.motiwa.R
import com.batuhanozdemir.motiwa.worker.ShowNotification
import com.batuhanozdemir.motiwa.databinding.ActivityMainBinding
import com.batuhanozdemir.motiwa.room.Motiwa
import com.batuhanozdemir.motiwa.room.MotiwaDAO
import com.batuhanozdemir.motiwa.room.MotiwaDatabase
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var db: MotiwaDatabase
    private lateinit var dao: MotiwaDAO
    private lateinit var sharedPref: SharedPreferences
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var dailyMotiwa: Motiwa
    private var dailyTextID = 1
    private var mInterstitialAd: InterstitialAd? = null
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        registerLauncher()
        requestPermission()
        sharedPref = getSharedPreferences("com.batuhanozdemir.motiwa", MODE_PRIVATE)
        db = Room.databaseBuilder(this,MotiwaDatabase::class.java,"motiwa_database").build()
        dao = db.motiwaDao()
        val constraints = Constraints.Builder().setRequiresBatteryNotLow(true).setRequiresCharging(false).build()
        val myWorkRequest = PeriodicWorkRequestBuilder<ShowNotification>(24, TimeUnit.HOURS)
            .setInitialDelay(5,TimeUnit.SECONDS)
            .setConstraints(constraints)
            .build()

        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
        InterstitialAd.load(this, getString(R.string.main_activity_ad_interstitial_id), adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(p0: LoadAdError) {
                    Log.d(TAG,p0.toString())
                    mInterstitialAd = null
                }
                override fun onAdLoaded(p0: InterstitialAd) {
                    Log.d(TAG,"Ad was loaded.")
                    mInterstitialAd = p0
                }
            })

        dailyTextID = sharedPref.getInt("dailyTextID",1)

        val handler = CoroutineExceptionHandler{ _,exception ->
            Log.e("AnaAktivite","Caught exception: ${exception.message}", exception)
        }

        CoroutineScope(Dispatchers.IO).launch(handler) {
            dailyMotiwa = dao.getMotiwa(dailyTextID)
            isFav()
            delay(400)
            launch(Dispatchers.Main){
                changeBackground()
                binding.textView.text = dailyMotiwa.text
            }
        }

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "notificationWork",
            ExistingPeriodicWorkPolicy.KEEP,
            myWorkRequest
        )
    }

    fun goSettings(view: View){
        val goSettings = Intent(this, SettingsActivity::class.java)
        startActivity(goSettings)
    }

    fun setFav(view: View){
        if (dailyMotiwa.favorite){
            dailyMotiwa.favorite = false
            Toast.makeText(this,"Favorilerden kaldırıldı",Toast.LENGTH_SHORT).show()
        }else{
            dailyMotiwa.favorite = true
            Toast.makeText(this,"Favorilere eklendi",Toast.LENGTH_SHORT).show()
        }
        isFav()
        CoroutineScope(Dispatchers.IO).launch {
            dao.updateText(dailyMotiwa)
        }
    }

    fun changeDailyText(view: View){
        var motiwa: Motiwa?
        val text_files = sharedPref.getStringSet("text_files",null)
        var changeCount = sharedPref.getInt("changeCount",0)
        //println("changeCount = $changeCount")

        if (changeCount > 0){
            CoroutineScope(Dispatchers.IO).launch {
                val categories: ArrayList<String> = arrayListOf("general")
                if (!text_files.isNullOrEmpty()){ text_files.forEach { categories.add(it) } }
                val category = categories.random()
                motiwa = dao.getRandomTexts(category)

                motiwa?.let {
                    it.used = true
                    dao.updateText(it)
                    sharedPref.edit().putInt("dailyTextID",it.id).apply()
                }
                if (motiwa == null){
                    motiwa = setAllUnUsed(category)
                }

                delay(500)
                dailyMotiwa = motiwa!!
                launch(Dispatchers.Main) {
                    changeBackground()
                    binding.textView.text = motiwa!!.text
                    isFav()
                }

                changeCount--
                sharedPref.edit().putInt("changeCount", changeCount).apply()
                if (changeCount == 0) {
                    launch(Dispatchers.Main) {
                        if (mInterstitialAd != null) {
                            mInterstitialAd?.show(this@MainActivity)
                        } else {
                            Log.d("TAG", "The interstitial ad wasn't ready yet.")
                        }
                    }
                }
            }
        }else{
            Toast.makeText(this@MainActivity, "Günlük değiştirme sınırını doldurdunuz.", Toast.LENGTH_LONG).show()
        }

    }

    private fun changeBackground(){
        when(dailyMotiwa.category){
            "art_and_creativity" -> binding.main.setBackgroundResource(R.drawable.pexels_art)
            "business_and_career" -> binding.main.setBackgroundResource(R.drawable.pexels_business)
            "love_and_relationships" -> binding.main.setBackgroundResource(R.drawable.pexels_love)
            "money_and_success" -> binding.main.setBackgroundResource(R.drawable.pexels_money)
            "read_and_learn" -> binding.main.setBackgroundResource(R.drawable.pexels_read_3)
            else -> binding.main.setBackgroundResource(R.drawable.pexels_general)
        }
    }

    private fun isFav(){
        if (dailyMotiwa.favorite){
            binding.favButton.setImageResource(R.drawable.star)
        }else{
            binding.favButton.setImageResource(R.drawable.star_empty)
        }
    }

    private fun setAllUnUsed(category: String): Motiwa{
        dao.setAllUnused()
        val motiwa = dao.getRandomTexts(category)
        motiwa?.let {
            it.used = true
            dao.updateText(it)
            sharedPref.edit().putInt("dailyTextID",it.id).apply()
        }
        return motiwa!!
    }

    private fun requestPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            if (ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED){
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.POST_NOTIFICATIONS)){
                    Snackbar.make(binding.root,"Bildirimler için izin gerekli",Snackbar.LENGTH_INDEFINITE).setAction("İZİN VER"){
                        // İzin iste
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }.show()
                }else{
                    // İzin iste
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun registerLauncher(){
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ result ->
            if (!result){
                Toast.makeText(this,"İzin Gerekli!",Toast.LENGTH_LONG).show()
            }
        }
    }
}