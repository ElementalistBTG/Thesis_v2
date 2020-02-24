package com.example.thesis_new.main

import android.content.Intent
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.thesis_new.R
import kotlinx.android.synthetic.main.main_activity.*

class MainActivity : AppCompatActivity() {

    //Declare Global Variables

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        // Initialisation phase of app

        mapperButton.setOnClickListener {
            // Handler code here.
            val intent = Intent(applicationContext, MapperActivity::class.java)
            startActivity(intent)
        }

        lostButton.setOnClickListener {
            // Handler code here.
            val intent = Intent(applicationContext, LostActivity::class.java)
            startActivity(intent)
        }
    }
}
