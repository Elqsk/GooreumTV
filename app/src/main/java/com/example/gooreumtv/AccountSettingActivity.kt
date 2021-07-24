package com.example.gooreumtv

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.gooreumtv.databinding.ActivityAccountSettingBinding

class AccountSettingActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityAccountSettingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAccountSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

//        val navController = findNavController(R.id.nav_host_fragment_content_account_setting)
//        appBarConfiguration = AppBarConfiguration(navController.graph)
//        setupActionBarWithNavController(navController, appBarConfiguration)
//
//        binding.fab.setOnClickListener { view ->
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                .setAction("Action", null).show()
//        }
    }

//    override fun onSupportNavigateUp(): Boolean {
//        val navController = findNavController(R.id.nav_host_fragment_content_account_setting)
//        return navController.navigateUp(appBarConfiguration)
//                || super.onSupportNavigateUp()
//    }

    fun logout(view: View) {
        // Use the Builder class for convenient dialog construction
        val builder = AlertDialog.Builder(this)
        builder.setMessage("로그아웃합니다")
            .setPositiveButton("확인",
                DialogInterface.OnClickListener { dialog, id ->
                    val session = getSharedPreferences("session", AppCompatActivity.MODE_PRIVATE)
                    if (session != null) {
                        val editor = session.edit()
                        editor.remove("user")
                        editor.apply()

                        Log.d(MainActivity.TAG, "LoginFragment > Logged out")
                        Log.d(MainActivity.TAG, " ")

                        val intent = Intent()
                        intent.putExtra("login", false)
                        setResult(RESULT_OK, intent)
                        finish()
                    }
                })
            .setNegativeButton("취소",
                DialogInterface.OnClickListener { dialog, id ->
                    // User cancelled the dialog
                })
        // Create the AlertDialog object and return it
        builder.create()
            .show()
    }
}