package com.hs.opengl.triangle

import android.content.Context
import android.media.AudioManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

class TriangleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_triangle)
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        Log.e("=======", "isBluetoothScoOn: ${am.isBluetoothScoOn}, isBluetoothScoAvailableOffCall: ${am.isBluetoothScoAvailableOffCall}")

    }
}