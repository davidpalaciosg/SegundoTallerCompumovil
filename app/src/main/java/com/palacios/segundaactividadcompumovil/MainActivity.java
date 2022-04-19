package com.palacios.segundaactividadcompumovil;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

public class MainActivity extends AppCompatActivity {

    ImageButton img_contacts;
    ImageButton img_camera;
    ImageButton img_maps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Inflate
        img_contacts = findViewById(R.id.img_contacts);
        img_camera = findViewById(R.id.img_camera);
        img_maps = findViewById(R.id.img_maps);

        //startButtons
        startButtons();

    }

    void startButtons(){
        //On click contacts
        img_contacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), ContactsActivity.class);
                startActivity(intent);
            }
        });

        //On click camera
        img_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), CameraActivity.class);
                startActivity(intent);
            }
        });

        //On click maps
        img_maps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
    }
}