package com.example.gestion_asistencia.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gestion_asistencia.R
import com.example.gestion_asistencia.api.AttendanceRecord
import java.text.SimpleDateFormat
import java.util.*

class AttendanceHistoryAdapter : RecyclerView.Adapter<AttendanceHistoryAdapter.ViewHolder>() {
    private var attendanceList: List<AttendanceRecord> = emptyList()

    fun updateData(newList: List<AttendanceRecord>) {
        attendanceList = newList.sortedByDescending { it.date }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = attendanceList[position]
        holder.bind(record)
    }

    override fun getItemCount() = attendanceList.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvCheckIn: TextView = itemView.findViewById(R.id.tvCheckIn)
        private val tvCheckOut: TextView = itemView.findViewById(R.id.tvCheckOut)

        private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("es"))
        private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        fun bind(record: AttendanceRecord) {
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                .parse(record.date)
            
            tvDate.text = date?.let { dateFormat.format(it) }
            tvCheckIn.text = formatTime(record.checkIn)
            tvCheckOut.text = record.checkOut?.let { formatTime(it) } ?: "-"
        }

        private fun formatTime(timeStr: String): String {
            return try {
                val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                    .parse(timeStr)
                timeFormat.format(date!!)
            } catch (e: Exception) {
                "-"
            }
        }
    }
} 