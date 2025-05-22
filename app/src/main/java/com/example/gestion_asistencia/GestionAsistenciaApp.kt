package com.example.gestion_asistencia

import android.app.Application
import com.example.gestion_asistencia.api.AuthManager

class GestionAsistenciaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AuthManager.init(this)
    }
} 