package com.example.a5g_4galerts;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnStart, btnStop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);

        btnStart.setOnClickListener(v -> {
            Intent serviceIntent = new Intent(this, NetworkMonitorService.class);
            startForegroundService(serviceIntent);
        });

        btnStop.setOnClickListener(v -> {
            Intent serviceIntent = new Intent(this, NetworkMonitorService.class);
            stopService(serviceIntent);
        });
    }
}
