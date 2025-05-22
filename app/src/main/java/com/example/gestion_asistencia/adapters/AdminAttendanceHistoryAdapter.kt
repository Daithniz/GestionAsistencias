package com.example.gestion_asistencia.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestion_asistencia.api.AdminAttendanceRecord
import com.example.gestion_asistencia.databinding.ItemAdminAttendanceHistoryBinding
import java.text.SimpleDateFormat
import java.util.*

class AdminAttendanceHistoryAdapter : ListAdapter<AdminAttendanceRecord, AdminAttendanceHistoryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdminAttendanceHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemAdminAttendanceHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        fun bind(record: AdminAttendanceRecord) {
            binding.apply {
                tvUserName.text = record.user.displayName
                tvUserEmail.text = record.user.email
                
                try {
                    val date = isoFormat.parse(record.date)
                    val checkIn = isoFormat.parse(record.checkIn)
                    val checkOut = record.checkOut?.let { isoFormat.parse(it) }
                    
                    tvDate.text = date?.let { dateFormat.format(it) } ?: "-"
                    tvCheckIn.text = checkIn?.let { timeFormat.format(it) } ?: "-"
                    tvCheckOut.text = checkOut?.let { timeFormat.format(it) } ?: "No registrado"
                } catch (e: Exception) {
                    tvDate.text = "-"
                    tvCheckIn.text = "-"
                    tvCheckOut.text = "No registrado"
                }
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<AdminAttendanceRecord>() {
        override fun areItemsTheSame(oldItem: AdminAttendanceRecord, newItem: AdminAttendanceRecord): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AdminAttendanceRecord, newItem: AdminAttendanceRecord): Boolean {
            return oldItem == newItem
        }
    }
} 