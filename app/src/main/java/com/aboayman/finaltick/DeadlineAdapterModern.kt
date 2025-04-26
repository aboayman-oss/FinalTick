package com.aboayman.finaltick

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aboayman.finaltick.databinding.ItemDeadlineModernBinding

class DeadlineAdapterModern(
    private val deadlines: MutableList<DeadlineItem>,
    private val onClick: (DeadlineItem) -> Unit,
    private val onDelete: (DeadlineItem) -> Unit,
    private val onLongPress: (DeadlineItem, View) -> Unit
) : RecyclerView.Adapter<DeadlineAdapterModern.ViewHolder>() {

    inner class ViewHolder(val binding: ItemDeadlineModernBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemDeadlineModernBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = deadlines.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = deadlines[position]
        holder.binding.deadlineTitle.text = item.title
        holder.binding.deadlineSubtitle.text =
            android.text.format.DateFormat.format("EEE, MMM d • h:mm a", item.timestamp)
        holder.binding.deadlineIcon.setImageResource(R.drawable.event)
        holder.binding.root.setOnClickListener { onClick(item) }
        holder.binding.root.setOnLongClickListener {
            onLongPress(item, holder.binding.root) // <-- added
            true
        }
        holder.binding.btnDelete.setOnClickListener {
            onDelete(item)
        }
    }

    fun updateData(newList: List<DeadlineItem>) {
        deadlines.clear()
        deadlines.addAll(newList)
        notifyDataSetChanged()
    }
}
