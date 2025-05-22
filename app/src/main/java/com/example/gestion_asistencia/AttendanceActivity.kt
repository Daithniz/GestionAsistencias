package com.example.gestion_asistencia

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestion_asistencia.adapters.AttendanceHistoryAdapter
import com.example.gestion_asistencia.api.ApiService
import com.example.gestion_asistencia.databinding.ActivityAttendanceBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AttendanceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAttendanceBinding
    private val apiService = ApiService.create()
    private val TAG = "AttendanceActivity"
    private val handler = Handler(Looper.getMainLooper())
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale("es"))
    private lateinit var attendanceAdapter: AttendanceHistoryAdapter

    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            updateDateTime()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAttendanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        startTimeUpdates()
        loadAttendanceHistory()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateTimeRunnable)
    }

    private fun setupRecyclerView() {
        attendanceAdapter = AttendanceHistoryAdapter()
        binding.rvAttendanceHistory.apply {
            layoutManager = LinearLayoutManager(this@AttendanceActivity)
            adapter = attendanceAdapter
        }
    }

    private fun setupListeners() {
        binding.btnRegisterAttendance.setOnClickListener {
            registerCheckIn()
        }

        binding.btnRegisterExit.setOnClickListener {
            registerCheckOut()
        }

        binding.btnLogout.setOnClickListener {
            logout()
        }
    }

    private fun startTimeUpdates() {
        updateDateTime()
        handler.post(updateTimeRunnable)
    }

    private fun updateDateTime() {
        val now = Calendar.getInstance()
        binding.tvCurrentTime.text = timeFormat.format(now.time)
        binding.tvCurrentDate.text = dateFormat.format(now.time)
    }

    private fun loadAttendanceHistory() {
        lifecycleScope.launch {
            try {
                val response = apiService.getUserAttendanceHistory()
                if (response.isSuccessful && response.body()?.success == true) {
                    val attendanceList = response.body()?.body?.attendance ?: emptyList()
                    attendanceAdapter.updateData(attendanceList)
                } else {
                    Log.e(TAG, "Error al cargar historial de asistencia")
                    Toast.makeText(this@AttendanceActivity, "Error al cargar historial", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar historial de asistencia", e)
                Toast.makeText(this@AttendanceActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun registerCheckIn() {
        lifecycleScope.launch {
            try {
                val response = apiService.registerCheckIn()

                when {
                    response.isSuccessful && response.body()?.success == true -> {
                        val mensaje = "✅ Entrada registrada"
                        Log.d(TAG, mensaje)
                        Toast.makeText(this@AttendanceActivity, mensaje, Toast.LENGTH_LONG).show()
                        loadAttendanceHistory()
                    }
                    response.isSuccessful && response.body()?.success == false -> {
                        val mensaje = response.body()?.body?.error ?: response.body()?.body?.message ?: "Error al registrar entrada"
                        Log.e(TAG, mensaje)
                        Toast.makeText(this@AttendanceActivity, mensaje, Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        val errorBody = response.errorBody()?.string()
                        val mensaje = "❌ Error al registrar entrada: ${response.code()} - $errorBody"
                        Log.e(TAG, mensaje)
                        Toast.makeText(this@AttendanceActivity, mensaje, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                val mensaje = when (e) {
                    is java.net.UnknownHostException -> "❌ No se puede acceder al servidor. Verifica tu conexión a internet."
                    is java.net.SocketTimeoutException -> "⚠️ El servidor está tardando en responder. Intenta más tarde."
                    is javax.net.ssl.SSLHandshakeException -> "❌ Error de seguridad: Certificado no válido"
                    else -> "❌ Error al registrar entrada: ${e.message}"
                }
                Log.e(TAG, "Error de registro", e)
                Toast.makeText(this@AttendanceActivity, mensaje, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun registerCheckOut() {
        lifecycleScope.launch {
            try {
                val response = apiService.registerCheckOut()

                when {
                    response.isSuccessful && response.body()?.success == true -> {
                        val mensaje = "✅ Salida registrada"
                        Log.d(TAG, mensaje)
                        Toast.makeText(this@AttendanceActivity, mensaje, Toast.LENGTH_LONG).show()
                        loadAttendanceHistory()
                    }
                    response.isSuccessful && response.body()?.success == false -> {
                        val mensaje = response.body()?.body?.error ?: response.body()?.body?.message ?: "Error al registrar salida"
                        Log.e(TAG, mensaje)
                        Toast.makeText(this@AttendanceActivity, mensaje, Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        val errorBody = response.errorBody()?.string()
                        val mensaje = "❌ Error al registrar salida: ${response.code()} - $errorBody"
                        Log.e(TAG, mensaje)
                        Toast.makeText(this@AttendanceActivity, mensaje, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                val mensaje = when (e) {
                    is java.net.UnknownHostException -> "❌ No se puede acceder al servidor. Verifica tu conexión a internet."
                    is java.net.SocketTimeoutException -> "⚠️ El servidor está tardando en responder. Intenta más tarde."
                    is javax.net.ssl.SSLHandshakeException -> "❌ Error de seguridad: Certificado no válido"
                    else -> "❌ Error al registrar salida: ${e.message}"
                }
                Log.e(TAG, "Error de registro", e)
                Toast.makeText(this@AttendanceActivity, mensaje, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun logout() {
        // Clear any stored session data
        getSharedPreferences("user_prefs", MODE_PRIVATE).edit().clear().apply()
        
        // Navigate to login screen
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
} 