package com.aboayman.finaltick

import android.os.Handler
import android.os.Looper
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
        RecyclerView.ViewHolder(binding.root) {

        private val handler = Handler(Looper.getMainLooper())
        private var updateRunnable: Runnable? = null

        fun bind(item: DeadlineItem) {
            binding.deadlineTitle.text = item.title
            binding.deadlineSubtitle.text =
                android.text.format.DateFormat.format("EEE, MMM d • h:mm a", item.timestamp)

            fun updateProgress() {
                val now = System.currentTimeMillis()
                val totalDuration = item.timestamp - item.createdAt
                val elapsed = now - item.createdAt

                val progress = when {
                    totalDuration <= 0L -> 100
                    elapsed <= 0L -> 0
                    elapsed >= totalDuration -> 100
                    else -> ((elapsed * 100) / totalDuration).toInt()
                }

                binding.deadlineProgress.progress = progress
                binding.deadlineProgressText.text = "$progress%"

                // 🛠 Dynamic color based on progress %
                val context = binding.root.context
                val colorRes = when (progress) {
                    in 0..24 -> R.color.progressSoftGreen
                    in 25..49 -> R.color.progressCyanBlue
                    in 50..74 -> R.color.progressAmber
                    else -> R.color.colorDanger
                }
                val color = context.getColor(colorRes)
                binding.deadlineProgress.setIndicatorColor(color)
                binding.deadlineProgressText.setTextColor(color) // Optional if you want text dynamic too
            }

            // Run immediately then every 5 seconds
            updateRunnable = object : Runnable {
                override fun run() {
                    updateProgress()
                    handler.postDelayed(this, 1000) // Update every 5 seconds
                }
            }
            handler.post(updateRunnable!!)
        }

        fun unbind() {
            updateRunnable?.let { handler.removeCallbacks(it) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemDeadlineModernBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = deadlines.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = deadlines[position]
        holder.bind(item)

        holder.binding.root.setOnClickListener { onClick(item) }
        holder.binding.root.setOnLongClickListener {
            onLongPress(item, holder.binding.root)
            true
        }
        holder.binding.btnDelete.setOnClickListener {
            onDelete(item)
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

    fun updateData(newList: List<DeadlineItem>) {
        deadlines.clear()
        deadlines.addAll(newList)
        notifyDataSetChanged()
    }
}