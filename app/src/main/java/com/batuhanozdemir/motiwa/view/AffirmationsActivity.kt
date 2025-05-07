package com.batuhanozdemir.motiwa.view

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.batuhanozdemir.motiwa.adapter.ShowedAffirmationsAdapter
import com.batuhanozdemir.motiwa.databinding.ActivityAffirmationsBinding
import com.batuhanozdemir.motiwa.room.Motiwa
import com.batuhanozdemir.motiwa.room.MotiwaDAO
import com.batuhanozdemir.motiwa.room.MotiwaDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AffirmationsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAffirmationsBinding
    private lateinit var db: MotiwaDatabase
    private lateinit var dao: MotiwaDAO
    private var affirmations: ArrayList<Motiwa> = ArrayList()
    //private var favorite = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAffirmationsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        db = Room.databaseBuilder(this,MotiwaDatabase::class.java,"motiwa_database").build()
        dao = db.motiwaDao()

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        CoroutineScope(Dispatchers.Main).launch {
            dao.getFavoriteAffirmations().collect{ motiwas ->
                motiwas.forEach { affirmations.add(it) }
                binding.recyclerView.adapter = ShowedAffirmationsAdapter(affirmations,this@AffirmationsActivity)
                if (affirmations.isEmpty()){ binding.henuzBirFavoriYok.visibility = View.VISIBLE }
            }
        }
    }
}