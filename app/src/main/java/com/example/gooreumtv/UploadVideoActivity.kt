package com.example.gooreumtv

import android.os.Bundle
import android.text.TextWatcher
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.gooreumtv.databinding.ActivityUploadVideoBinding

class UploadVideoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUploadVideoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityUploadVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        addTextChangedListener()
        setVideoClickListener()
    }

    private fun setVideoClickListener() {

    }

    private var metRequirements = false

    private fun addTextChangedListener() {
//        binding.title.addTextChangedListener(object : TextWatcher {
//
//        })
    }
}