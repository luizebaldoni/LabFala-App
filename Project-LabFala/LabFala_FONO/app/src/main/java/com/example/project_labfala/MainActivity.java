package com.example.project_labfala;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // definindo botao inicial como o drawble play
        View btn_inicio = findViewById(R.id.botao1);
        // criando intenção de abrir nova pagina ao clicar n botao
        btn_inicio.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, Second_Activity.class);
            startActivity(intent);
        });
    }
}