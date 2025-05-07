package com.batuhanozdemir.motiwa.view

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.batuhanozdemir.motiwa.R
import com.batuhanozdemir.motiwa.databinding.ActivitySettingsBinding
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var sharedPref: SharedPreferences
    private var checkBoxes: ArrayList<CheckBox> = ArrayList()
    private var TEXT_FILES: ArrayList<String> = ArrayList()
    private var mInterstitialAd: InterstitialAd? = null
    private val TAG = "SettingsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        sharedPref = getSharedPreferences("com.batuhanozdemir.motiwa", MODE_PRIVATE)
        val strSet = sharedPref.getStringSet("text_files",null)
        strSet?.let {
            val viewGroup = binding.categories
            for (i in 0 until viewGroup.childCount){
                val view = viewGroup.getChildAt(i)
                if (view is CheckBox && view.tag.toString() in it){
                    view.isChecked = true
                }
            }
        }

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this, getString(R.string.settings_activity_ad_interstitial_id), adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, adError.toString())
                mInterstitialAd = null
            }
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                Log.d(TAG, "Ad was loaded.")
                mInterstitialAd = interstitialAd
            }
        })

        val notificationCheck = sharedPref.getBoolean("notification",true)
        binding.notificationSwitch.isChecked = notificationCheck
        binding.notificationSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked){
                sharedPref.edit().putBoolean("notification",true).apply()
                Toast.makeText(this,"Bildirimler etkinleştirildi.",Toast.LENGTH_LONG).show()
            }else{
                AlertDialog.Builder(this,R.style.alertDialogTheme)
                    .setTitle("Bildirimler Kapanacak!")
                    .setMessage("Bildirimleri kapatmak istediğinize emin misiniz?")
                    .setPositiveButton("Evet") { dialogInterface, i ->
                        sharedPref.edit().putBoolean("notification", false).apply()
                        Toast.makeText(this, "Bildirimler kapatıldı.", Toast.LENGTH_LONG).show()
                    }
                    .setNegativeButton("Hayır") { dialogInterface, i ->
                        binding.notificationSwitch.isChecked = true
                    }
                    .show()
            }
        }
    }

    fun activeApplyButton(view: View){ binding.applyButton.visibility = View.VISIBLE }

    fun goFavorites(view: View){
        val goFav = Intent(this,AffirmationsActivity::class.java)
        startActivity(goFav)
    }

    fun apply(view: View){
        TEXT_FILES.clear()
        checkBoxes.clear()
        checkCheckBoxes(binding.categories)

        if (checkBoxes.size > 2){
            AlertDialog.Builder(this,R.style.alertDialogTheme)
                .setTitle("Uyarı")
                .setMessage("Lütfen en fazla 2 kategori seçin")
                .setPositiveButton("Tamam",null)
                .show()
        }else{
            val strSet = TEXT_FILES.toSet()
            sharedPref.edit().putStringSet("text_files",strSet).apply()
            val intent = Intent(this,MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            Toast.makeText(this,"Değişiklikler kaydedildi",Toast.LENGTH_LONG).show()

            if (mInterstitialAd != null && checkBoxes.isNotEmpty()) {
                mInterstitialAd?.show(this)
            } else {
                Log.d("TAG", "The interstitial ad wasn't ready yet.")
            }
        }
    }

    fun checkCheckBoxes(viewGroup: ViewGroup){
        for (i in 0 until viewGroup.childCount){
            val view = viewGroup.getChildAt(i)
            if (view is CheckBox && view.isChecked){
                TEXT_FILES.add(view.tag.toString())
                checkBoxes.add(view)
            }
        }
    }
}