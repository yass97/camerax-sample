package com.yass.cameraxcodelab

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.squareup.picasso.Picasso
import java.io.File

object BindingAdapter {

    @BindingAdapter("loadImageFromFile")
    @JvmStatic
    fun loadImageFromFile(imageView: ImageView, file: File?) {

        file ?: return

        Picasso.with(imageView.context)
            .load(file)
            .fit()
            .centerCrop()
            .into(imageView)
    }
}