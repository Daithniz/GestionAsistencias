package com.example.gestion_asistencia

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.gestion_asistencia.api.ApiService
import com.example.gestion_asistencia.api.AuthManager
import com.example.gestion_asistencia.api.LoginRequest
import com.example.gestion_asistencia.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val apiService = ApiService.create()
    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        testApiConnection()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (validateInputs(email, password)) {
                loginUser(email, password)
            }
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.etUsername.error = "El correo es requerido"
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etUsername.error = "Correo electrónico inválido"
            return false
        }

        if (password.isEmpty()) {
            binding.etPassword.error = "La contraseña es requerida"
            return false
        }

        return true
    }

    private fun loginUser(email: String, password: String) {
        lifecycleScope.launch {
            try {
                val loginRequest = LoginRequest(email, password)
                val response = apiService.login(loginRequest)

                when {
                    response.isSuccessful && response.body()?.success == true -> {
                        val usuario = response.body()?.body?.user
                        val token = response.body()?.body?.token
                        
                        if (token != null) {
                            AuthManager.saveToken(token)
                            val mensaje = "✅ Login exitoso: ${usuario?.displayName}"
                            Log.d(TAG, mensaje)
                            Toast.makeText(this@LoginActivity, mensaje, Toast.LENGTH_LONG).show()
                            
                            // Redirigir según el rol del usuario
                            val intent = if (usuario?.role == "admin") {
                                Intent(this@LoginActivity, AdminDashboardActivity::class.java)
                            } else {
                                Intent(this@LoginActivity, AttendanceActivity::class.java)
                            }
                            startActivity(intent)
                            finish() // Cerrar LoginActivity para que no se pueda volver atrás
                        } else {
                            val mensaje = "❌ Error: Token no recibido"
                            Log.e(TAG, mensaje)
                            Toast.makeText(this@LoginActivity, mensaje, Toast.LENGTH_LONG).show()
                        }
                    }
                    response.isSuccessful && response.body()?.success == false -> {
                        val mensaje = response.body()?.body?.error ?: "Error en el login"
                        Log.e(TAG, mensaje)
                        Toast.makeText(this@LoginActivity, mensaje, Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        val errorBody = response.errorBody()?.string()
                        val mensaje = "❌ Error en el login: ${response.code()} - $errorBody"
                        Log.e(TAG, mensaje)
                        Toast.makeText(this@LoginActivity, mensaje, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                val mensaje = when (e) {
                    is java.net.UnknownHostException -> "❌ No se puede acceder al servidor. Verifica tu conexión a internet."
                    is java.net.SocketTimeoutException -> "⚠️ El servidor está tardando en responder. Intenta más tarde."
                    is javax.net.ssl.SSLHandshakeException -> "❌ Error de seguridad: Certificado no válido"
                    else -> "❌ Error en el login: ${e.message}"
                }
                Log.e(TAG, "Error de login", e)
                Toast.makeText(this@LoginActivity, mensaje, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun testApiConnection() {
        Log.d(TAG, "Iniciando prueba de conexión API...")
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Haciendo petición a ${ApiService.BASE_URL}/api/")
                val response = apiService.testConnection()
                
                when {
                    response.isSuccessful && response.body()?.success == true -> {
                        val mensaje = "✅ Conexión exitosa al servidor"
                        Log.d(TAG, mensaje)
                        Toast.makeText(this@LoginActivity, mensaje, Toast.LENGTH_LONG).show()
                    }
                    response.isSuccessful && response.body()?.success == false -> {
                        val mensaje = "⚠️ El servidor respondió pero hubo un error: ${response.body()?.body?.message}"
                        Log.e(TAG, mensaje)
                        Toast.makeText(this@LoginActivity, mensaje, Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        val errorBody = response.errorBody()?.string()
                        val mensaje = "❌ Error de conexión: ${response.code()} - $errorBody"
                        Log.e(TAG, mensaje)
                        Toast.makeText(this@LoginActivity, mensaje, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                val mensaje = when (e) {
                    is java.net.UnknownHostException -> "❌ No se puede acceder al servidor. Verifica tu conexión a internet."
                    is java.net.SocketTimeoutException -> "⚠️ El servidor está tardando en responder. Intenta más tarde."
                    is javax.net.ssl.SSLHandshakeException -> "❌ Error de seguridad: Certificado no válido"
                    else -> "❌ Error de conexión: ${e.message}"
                }
                Log.e(TAG, "Error de conexión", e)
                Toast.makeText(this@LoginActivity, mensaje, Toast.LENGTH_LONG).show()
            }
        }
    }
} 