package com.aboayman.finaltick

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DeadlineAdapter(
    private val deadlines: List<DeadlineItem>,
    private val onClick: (DeadlineItem) -> Unit
) : RecyclerView.Adapter<DeadlineAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.deadlineTitle)
        val subtitle: TextView = view.findViewById(R.id.deadlineSubtitle)
        val icon: ImageView = view.findViewById(R.id.deadlineIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_deadline, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = deadlines[position]
        holder.title.text = item.title
        holder.subtitle.text = android.text.format.DateFormat.format("EEE, MMM d • h:mm a", item.timestamp)
        holder.icon.setImageResource(R.drawable.ic_calendar) // replace with your calendar icon

        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = deadlines.size
}
