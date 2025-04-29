package com.aboayman.finaltick

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.CircularProgressIndicator

class DeadlineAdapter(
    private val deadlines: List<DeadlineItem>,
    private val onClick: (DeadlineItem) -> Unit
) : RecyclerView.Adapter<DeadlineAdapter.ViewHolder>() {

    private var selectedPosition = RecyclerView.NO_POSITION

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.deadlineTitle)
        val subtitle: TextView = view.findViewById(R.id.deadlineSubtitle)
        val progressBar: CircularProgressIndicator = view.findViewById(R.id.deadlineProgress)
        val progressText: TextView = view.findViewById(R.id.deadlineProgressText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_deadline, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = deadlines.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = deadlines[position]

        holder.title.text = item.title
        holder.subtitle.text =
            android.text.format.DateFormat.format("EEE, MMM d • h:mm a", item.timestamp)

        // Progress Calculation
        val totalDuration = item.timestamp - item.createdAt
        val elapsed = System.currentTimeMillis() - item.createdAt
        val progress = if (totalDuration > 0) {
            (elapsed.toFloat() / totalDuration * 100).coerceIn(0f, 100f)
        } else {
            100f
        }

        holder.progressBar.setProgressCompat(progress.toInt(), true)
        holder.progressText.text = "${progress.toInt()}%"

        // Dynamic Progress Color
        val context = holder.itemView.context
        val colorRes = when (progress) {
            in 0f..24f -> R.color.progressSoftGreen
            in 25f..49f -> R.color.progressCyanBlue
            in 50f..74f -> R.color.progressAmber
            else -> R.color.colorDanger
        }
        val color = context.getColor(colorRes)
        holder.progressBar.setIndicatorColor(color)
        holder.progressText.setTextColor(color)

        // 🎯 NEW: Highlight selected item
        if (position == selectedPosition) {
            holder.itemView.setBackgroundColor(Color.LTGRAY)
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        }

        holder.itemView.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)

            onClick(item)
        }
    }
}