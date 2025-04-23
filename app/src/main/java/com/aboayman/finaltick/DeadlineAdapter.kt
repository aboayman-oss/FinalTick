package com.aboayman.finaltick

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aboayman.finaltick.databinding.ItemDeadlineBinding

class DeadlineAdapter(
    private val deadlines: List<DeadlineItem>,
    private val onClick: (DeadlineItem) -> Unit
) : RecyclerView.Adapter<DeadlineAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemDeadlineBinding) : RecyclerView.ViewHolder(binding.root)

    fun updateData(newList: List<DeadlineItem>) {
        (deadlines as MutableList).clear()
        deadlines.addAll(newList)
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemDeadlineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = deadlines[position]

        holder.binding.deadlineTitle.text = item.title
        holder.binding.deadlineSubtitle.text =
            android.text.format.DateFormat.format("EEE, MMM d • h:mm a", item.timestamp)

        holder.binding.deadlineIcon.setImageResource(R.drawable.ic_calendar)
        holder.binding.root.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = deadlines.size
}
