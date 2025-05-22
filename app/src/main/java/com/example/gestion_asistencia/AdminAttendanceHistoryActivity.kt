package com.example.gestion_asistencia

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestion_asistencia.api.ApiService
import com.example.gestion_asistencia.adapters.AdminAttendanceHistoryAdapter
import com.example.gestion_asistencia.databinding.ActivityAdminAttendanceHistoryBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AdminAttendanceHistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminAttendanceHistoryBinding
    private val apiService = ApiService.create()
    private val TAG = "AdminAttendanceHistory"
    private lateinit var adapter: AdminAttendanceHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminAttendanceHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!com.example.gestion_asistencia.api.AuthManager.isLoggedIn()) {
            Toast.makeText(this, "❌ Sesión expirada. Por favor, vuelve a iniciar sesión.", Toast.LENGTH_LONG).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        setupRecyclerView()
        setupListeners()
        loadAttendanceHistory()
    }

    private fun setupRecyclerView() {
        adapter = AdminAttendanceHistoryAdapter()
        binding.rvAttendanceHistory.apply {
            layoutManager = LinearLayoutManager(this@AdminAttendanceHistoryActivity)
            adapter = this@AdminAttendanceHistoryActivity.adapter
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadAttendanceHistory() {
        Log.d(TAG, "Iniciando carga de historial de asistencia...")
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Verificando token de autenticación...")
                val token = com.example.gestion_asistencia.api.AuthManager.getToken()
                Log.d(TAG, "Token presente: ${token != null}")

                Log.d(TAG, "Realizando petición a la API...")
                val response = apiService.getAdminAttendanceHistory()
                Log.d(TAG, "Respuesta recibida - Código: ${response.code()}, Éxito: ${response.isSuccessful}")
                
                when {
                    response.isSuccessful && response.body()?.success == true -> {
                        val historyList = response.body()?.body?.history ?: emptyList()
                        Log.d(TAG, "Historial cargado exitosamente. Registros: ${historyList.size}")
                        adapter.submitList(historyList)
                        updateEmptyState(historyList.isEmpty())
                    }
                    response.isSuccessful && response.body()?.success == false -> {
                        val mensaje = "Error al cargar historial"
                        Log.e(TAG, "Error en respuesta: $mensaje")
                        Log.e(TAG, "Cuerpo de respuesta: ${response.body()}")
                        Toast.makeText(this@AdminAttendanceHistoryActivity, mensaje, Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        val errorBody = response.errorBody()?.string()
                        val mensaje = "❌ Error al cargar historial: ${response.code()} - $errorBody"
                        Log.e(TAG, "Error HTTP: $mensaje")
                        Log.e(TAG, "Headers: ${response.headers()}")
                        Toast.makeText(this@AdminAttendanceHistoryActivity, mensaje, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                val mensaje = when (e) {
                    is java.net.UnknownHostException -> "❌ No se puede acceder al servidor. Verifica tu conexión a internet."
                    is java.net.SocketTimeoutException -> "⚠️ El servidor está tardando en responder. Intenta más tarde."
                    is javax.net.ssl.SSLHandshakeException -> "❌ Error de seguridad: Certificado no válido"
                    else -> "❌ Error al cargar historial: ${e.message}"
                }
                Log.e(TAG, "Excepción al cargar historial", e)
                Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
                Toast.makeText(this@AdminAttendanceHistoryActivity, mensaje, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.tvNoHistory.visibility = if (isEmpty) android.view.View.VISIBLE else android.view.View.GONE
        binding.rvAttendanceHistory.visibility = if (isEmpty) android.view.View.GONE else android.view.View.VISIBLE
    }
} 