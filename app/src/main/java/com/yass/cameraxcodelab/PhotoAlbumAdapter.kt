package com.yass.cameraxcodelab

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.yass.cameraxcodelab.databinding.ItemPhotoBinding
import java.io.File

class PhotoAlbumAdapter : RecyclerView.Adapter<PhotoAlbumAdapter.ViewHolder>() {

    private val photos: MutableList<File> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return ViewHolder(
            ItemPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun getItemCount(): Int = photos.count()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val photo = photos[position]

        holder.bindTo(photo)
    }

    fun addPhoto(photo: File) {
        photos.add(photo)
        notifyDataSetChanged()
    }

    class ViewHolder(private val binding: ItemPhotoBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bindTo(photo: File) {
            binding.photo = photo
        }
    }
}