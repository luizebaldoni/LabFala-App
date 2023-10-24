package com.example.fono_project

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val inicio = findViewById<View>(R.id.botao1)
        inicio.setOnClickListener {
            val intent = Intent(this, Second::class.java)
            startActivity(intent)
        }
    }

}