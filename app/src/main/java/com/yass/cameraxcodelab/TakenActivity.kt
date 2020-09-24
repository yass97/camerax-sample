package com.yass.cameraxcodelab

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.squareup.picasso.Picasso
import com.yass.cameraxcodelab.databinding.ActivityTakenBinding

class TakenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTakenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_taken)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_taken)

        val uri = intent.data

        Picasso.with(this).load(uri).fit().centerCrop().into(binding.takenPhoto)
    }
}