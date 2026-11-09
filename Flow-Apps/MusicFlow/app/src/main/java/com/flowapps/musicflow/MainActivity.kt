package com.flowapps.musicflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    private lateinit var ps: PlayerState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ps = PlayerState(applicationContext)
        setContent { FlowTheme { MusicApp(ps) } }
    }

    override fun onDestroy() {
        ps.release()
        super.onDestroy()
    }
}
