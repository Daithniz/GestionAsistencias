package com.example.gestion_asistencia

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestion_asistencia.api.ApiService
import com.example.gestion_asistencia.api.RegisterRequest
import com.example.gestion_asistencia.api.UpdateUserRequest
import com.example.gestion_asistencia.api.UserProfile
import com.example.gestion_asistencia.api.DeleteUserRequest
import com.example.gestion_asistencia.databinding.ActivityUserManagementBinding
import com.example.gestion_asistencia.databinding.DialogAddUserBinding
import com.example.gestion_asistencia.databinding.DialogEditUserBinding
import kotlinx.coroutines.launch

class UserManagementActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserManagementBinding
    private val apiService = ApiService.create()
    private val TAG = "UserManagementActivity"
    private lateinit var adapter: UserAdapter
    private var allUsers: List<UserProfile> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        setupSearch()
        loadUsers()
    }

    private fun setupRecyclerView() {
        adapter = UserAdapter(
            onEditClick = { user -> showEditUserDialog(user) },
            onDeleteClick = { user -> showDeleteConfirmationDialog(user) }
        )
        binding.rvUsers.apply {
            layoutManager = LinearLayoutManager(this@UserManagementActivity)
            adapter = this@UserManagementActivity.adapter
        }
    }

    private fun setupListeners() {
        binding.btnAddUser.setOnClickListener {
            showAddUserDialog()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterUsers(s.toString())
            }
        })
    }

    private fun filterUsers(query: String) {
        val filteredList = if (query.isEmpty()) {
            allUsers
        } else {
            allUsers.filter { user ->
                user.displayName.contains(query, ignoreCase = true) ||
                user.email.contains(query, ignoreCase = true)
            }
        }
        adapter.submitList(filteredList)
        updateEmptyState(filteredList.isEmpty())
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.tvNoUsers.visibility = if (isEmpty) android.view.View.VISIBLE else android.view.View.GONE
        binding.rvUsers.visibility = if (isEmpty) android.view.View.GONE else android.view.View.VISIBLE
    }

    private fun loadUsers() {
        lifecycleScope.launch {
            try {
                val response = apiService.getAllProfiles()

                when {
                    response.isSuccessful && response.body()?.success == true -> {
                        allUsers = response.body()?.body?.profiles ?: emptyList()
                        adapter.submitList(allUsers)
                        updateEmptyState(allUsers.isEmpty())
                    }
                    else -> {
                        val errorBody = response.errorBody()?.string()
                        val mensaje = "❌ Error al cargar usuarios: ${response.code()} - $errorBody"
                        Log.e(TAG, mensaje)
                        Toast.makeText(this@UserManagementActivity, mensaje, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                val mensaje = when (e) {
                    is java.net.UnknownHostException -> "❌ No se puede acceder al servidor. Verifica tu conexión a internet."
                    is java.net.SocketTimeoutException -> "⚠️ El servidor está tardando en responder. Intenta más tarde."
                    is javax.net.ssl.SSLHandshakeException -> "❌ Error de seguridad: Certificado no válido"
                    else -> "❌ Error al cargar usuarios: ${e.message}"
                }
                Log.e(TAG, "Error al cargar usuarios", e)
                Toast.makeText(this@UserManagementActivity, mensaje, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showAddUserDialog() {
        val dialogBinding = DialogAddUserBinding.inflate(layoutInflater)
        
        AlertDialog.Builder(this)
            .setTitle("Agregar Usuario")
            .setView(dialogBinding.root)
            .setPositiveButton("Registrar") { _, _ ->
                val email = dialogBinding.etEmail.text.toString().trim()
                val password = dialogBinding.etPassword.text.toString().trim()
                val displayName = dialogBinding.etDisplayName.text.toString().trim()

                if (validateInputs(email, password, displayName)) {
                    registerUser(email, password, displayName)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditUserDialog(user: UserProfile) {
        val dialogBinding = DialogEditUserBinding.inflate(layoutInflater)
        
        // Pre-fill the fields with current user data
        dialogBinding.etDisplayName.setText(user.displayName)
        dialogBinding.etEmail.setText(user.email)

        AlertDialog.Builder(this)
            .setTitle("Editar Usuario")
            .setView(dialogBinding.root)
            .setPositiveButton("Guardar") { _, _ ->
                val email = dialogBinding.etEmail.text.toString().trim()
                val displayName = dialogBinding.etDisplayName.text.toString().trim()

                if (validateEditInputs(email, displayName)) {
                    updateUser(user._id, email, displayName)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteConfirmationDialog(user: UserProfile) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Eliminar Usuario")
            .setMessage("¿Estás seguro de que deseas eliminar al usuario ${user.displayName}?")
            .setPositiveButton("Eliminar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            val btnEliminar = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            btnEliminar.setOnClickListener {
                deleteUser(user._id) {
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun validateInputs(email: String, password: String, displayName: String): Boolean {
        if (displayName.isEmpty()) {
            Toast.makeText(this, "El nombre es requerido", Toast.LENGTH_SHORT).show()
            return false
        }

        if (email.isEmpty()) {
            Toast.makeText(this, "El correo es requerido", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Correo electrónico inválido", Toast.LENGTH_SHORT).show()
            return false
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "La contraseña es requerida", Toast.LENGTH_SHORT).show()
            return false
        }

        if (password.length < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun validateEditInputs(email: String, displayName: String): Boolean {
        if (displayName.isEmpty()) {
            Toast.makeText(this, "El nombre es requerido", Toast.LENGTH_SHORT).show()
            return false
        }

        if (email.isEmpty()) {
            Toast.makeText(this, "El correo es requerido", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Correo electrónico inválido", Toast.LENGTH_SHORT).show()
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
                        val mensaje = "✅ Usuario registrado exitosamente"
                        Toast.makeText(this@UserManagementActivity, mensaje, Toast.LENGTH_LONG).show()
                        loadUsers() // Recargar la lista de usuarios
                    }
                    response.isSuccessful && response.body()?.success == false -> {
                        val mensaje = "⚠️ Error en el registro: ${response.body()?.body?.message}"
                        Toast.makeText(this@UserManagementActivity, mensaje, Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        val errorBody = response.errorBody()?.string()
                        val mensaje = "❌ Error en el registro: ${response.code()} - $errorBody"
                        Toast.makeText(this@UserManagementActivity, mensaje, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                val mensaje = when (e) {
                    is java.net.UnknownHostException -> "❌ No se puede acceder al servidor. Verifica tu conexión a internet."
                    is java.net.SocketTimeoutException -> "⚠️ El servidor está tardando en responder. Intenta más tarde."
                    is javax.net.ssl.SSLHandshakeException -> "❌ Error de seguridad: Certificado no válido"
                    else -> "❌ Error en el registro: ${e.message}"
                }
                Toast.makeText(this@UserManagementActivity, mensaje, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateUser(_id: String, email: String, displayName: String) {
        lifecycleScope.launch {
            try {
                val updateRequest = UpdateUserRequest(_id, email, displayName)
                val response = apiService.updateUser(updateRequest)

                when {
                    response.isSuccessful && response.body()?.success == true -> {
                        val mensaje = "✅ Usuario actualizado exitosamente"
                        Toast.makeText(this@UserManagementActivity, mensaje, Toast.LENGTH_LONG).show()
                        loadUsers() // Recargar la lista de usuarios
                    }
                    response.isSuccessful && response.body()?.success == false -> {
                        val mensaje = "⚠️ Error al actualizar: ${response.body()?.body?.message}"
                        Toast.makeText(this@UserManagementActivity, mensaje, Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        val errorBody = response.errorBody()?.string()
                        val mensaje = "❌ Error al actualizar: ${response.code()} - $errorBody"
                        Toast.makeText(this@UserManagementActivity, mensaje, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                val mensaje = when (e) {
                    is java.net.UnknownHostException -> "❌ No se puede acceder al servidor. Verifica tu conexión a internet."
                    is java.net.SocketTimeoutException -> "⚠️ El servidor está tardando en responder. Intenta más tarde."
                    is javax.net.ssl.SSLHandshakeException -> "❌ Error de seguridad: Certificado no válido"
                    else -> "❌ Error al actualizar: ${e.message}"
                }
                Toast.makeText(this@UserManagementActivity, mensaje, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun deleteUser(_id: String, onSuccess: (() -> Unit)? = null) {
        if (_id.isBlank()) {
            Toast.makeText(this, "ID de usuario inválido", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            try {
                val response = apiService.deleteUser(_id)
                when {
                    response.isSuccessful && response.body()?.success == true -> {
                        val mensaje = "✅ Usuario eliminado exitosamente"
                        Toast.makeText(this@UserManagementActivity, mensaje, Toast.LENGTH_LONG).show()
                        loadUsers()
                        onSuccess?.invoke()
                    }
                    response.isSuccessful && response.body()?.success == false -> {
                        val mensaje = "⚠️ Error al eliminar: ${response.body()?.body?.message}"
                        Toast.makeText(this@UserManagementActivity, mensaje, Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        val errorBody = response.errorBody()?.string()
                        val mensaje = "❌ Error al eliminar: ${response.code()} - $errorBody"
                        Toast.makeText(this@UserManagementActivity, mensaje, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                val mensaje = when (e) {
                    is java.net.UnknownHostException -> "❌ No se puede acceder al servidor. Verifica tu conexión a internet."
                    is java.net.SocketTimeoutException -> "⚠️ El servidor está tardando en responder. Intenta más tarde."
                    is javax.net.ssl.SSLHandshakeException -> "❌ Error de seguridad: Certificado no válido"
                    else -> "❌ Error al eliminar: ${e.message}"
                }
                Toast.makeText(this@UserManagementActivity, mensaje, Toast.LENGTH_LONG).show()
            }
        }
    }
} 