package com.example.gestion_asistencia

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.gestion_asistencia.api.ApiService
import com.example.gestion_asistencia.api.RegisterRequest
import com.example.gestion_asistencia.databinding.ActivityRegisterBinding
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private val apiService = ApiService.create()
    private val TAG = "RegisterActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val displayName = binding.etDisplayName.text.toString().trim()

            if (validateInputs(email, password, displayName)) {
                registerUser(email, password, displayName)
            }
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun validateInputs(email: String, password: String, displayName: String): Boolean {
        if (displayName.isEmpty()) {
            binding.etDisplayName.error = "El nombre es requerido"
            return false
        }

        if (email.isEmpty()) {
            binding.etEmail.error = "El correo es requerido"
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Correo electrónico inválido"
            return false
        }

        if (password.isEmpty()) {
            binding.etPassword.error = "La contraseña es requerida"
            return false
        }

        if (password.length < 6) {
            binding.etPassword.error = "La contraseña debe tener al menos 6 caracteres"
            return false
        }

        return true
    }

    private fun registerUser(email: String, password: String, displayName: String) {
        lifecycleScope.launch {
            try {
                val registerRequest = RegisterRequest(email, password, displayName)
                val response = apiService.register(registerRequest)

                when {
                    response.isSuccessful && response.body()?.success == true -> {
                        val usuario = response.body()?.body?.user
                        val mensaje = "✅ Registro exitoso: ${usuario?.displayName}"
                        Log.d(TAG, mensaje)
                        Toast.makeText(this@RegisterActivity, mensaje, Toast.LENGTH_LONG).show()
                        finish() // Volver a la pantalla de login
                    }
                    response.isSuccessful && response.body()?.success == false -> {
                        val mensaje = "⚠️ Error en el registro: ${response.body()?.body?.message}"
                        Log.e(TAG, mensaje)
                        Toast.makeText(this@RegisterActivity, mensaje, Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        val errorBody = response.errorBody()?.string()
                        val mensaje = "❌ Error en el registro: ${response.code()} - $errorBody"
                        Log.e(TAG, mensaje)
                        Toast.makeText(this@RegisterActivity, mensaje, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                val mensaje = when (e) {
                    is java.net.UnknownHostException -> "❌ No se puede acceder al servidor. Verifica tu conexión a internet."
                    is java.net.SocketTimeoutException -> "⚠️ El servidor está tardando en responder. Intenta más tarde."
                    is javax.net.ssl.SSLHandshakeException -> "❌ Error de seguridad: Certificado no válido"
                    else -> "❌ Error en el registro: ${e.message}"
                }
                Log.e(TAG, "Error de registro", e)
                Toast.makeText(this@RegisterActivity, mensaje, Toast.LENGTH_LONG).show()
            }
        }
    }
} 