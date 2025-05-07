package com.batuhanozdemir.motiwa.adapter

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.room.Room
import com.batuhanozdemir.motiwa.R
import com.batuhanozdemir.motiwa.databinding.RecyclerviewRowBinding
import com.batuhanozdemir.motiwa.room.Motiwa
import com.batuhanozdemir.motiwa.room.MotiwaDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ShowedAffirmationsAdapter(
    private val affirmations: ArrayList<Motiwa>,
    private val context: Context,
    //private val favorite: Boolean
): Adapter<ShowedAffirmationsAdapter.AffirmationsHolder>() {
    private val db = Room.databaseBuilder(context,MotiwaDatabase::class.java,"motiwa_database").build()
    private val dao = db.motiwaDao()

    class AffirmationsHolder(val binding: RecyclerviewRowBinding): ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AffirmationsHolder {
        val binding = RecyclerviewRowBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return AffirmationsHolder(binding)
    }

    override fun getItemCount(): Int { return affirmations.size }

    override fun onBindViewHolder(holder: AffirmationsHolder, position: Int) {
        val affirmation = affirmations[position]
        holder.binding.favIcon.contentDescription = "delete $position"

        if (affirmation.favorite){
            holder.binding.favIcon.visibility = View.GONE
            holder.itemView.visibility = View.VISIBLE
        }else{
            holder.itemView.visibility = View.GONE
        }

        holder.binding.motivationText.text = affirmation.text

        val alert = AlertDialog.Builder(context,R.style.alertDialogTheme)
            .setTitle("Uyarı")
            .setMessage("Bu metin favorilerden kaldırılacak\n\n" + affirmation.text)
            .setPositiveButton("Tamam") { dialogInterface, i ->
                CoroutineScope(Dispatchers.IO).launch {
                    affirmation.favorite = false
                    dao.updateText(affirmation)
                }
                affirmations.removeAt(position)
                this.notifyItemRemoved(position)
                Toast.makeText(context, "Favorilerden kaldırıldı", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("İptal") { dialogInterface, i ->
                holder.binding.favIcon.visibility = View.GONE
            }

        holder.itemView.setOnLongClickListener {
            holder.binding.favIcon.visibility = View.VISIBLE
            true
        }

        holder.binding.favIcon.setOnClickListener{
            alert.show()
        }
    }
}