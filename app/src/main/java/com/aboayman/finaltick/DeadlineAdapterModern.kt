package com.aboayman.finaltick

import android.text.format.DateFormat
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aboayman.finaltick.databinding.ItemDeadlineModernBinding

class DeadlineAdapterModern(
    private val onClick: (DeadlineItem) -> Unit,
    private val onMenuClick: (DeadlineItem, View) -> Unit,
    private val onLongPress: (DeadlineItem, View) -> Unit
) : ListAdapter<DeadlineItem, DeadlineAdapterModern.ViewHolder>(DeadlineDiffCallback()) {

    inner class ViewHolder(val binding: ItemDeadlineModernBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // Progress updates are centralized in MainActivity

        fun bind(item: DeadlineItem) {
            binding.deadlineTitle.text = item.title
            // This line now works correctly because of the fixed import
            binding.deadlineSubtitle.text = DateFormat.format("EEE, MMM d • h:mm a", item.timestamp)

            // Setup listeners
            binding.root.setOnClickListener {
                Haptics.perform(it.context, binding.root, HapticFeedbackConstants.CLOCK_TICK)
                onClick(item)
            }
            binding.root.setOnLongClickListener {
                Haptics.perform(it.context, binding.root, HapticFeedbackConstants.LONG_PRESS)
                onLongPress(item, binding.root)
                true
            }
            binding.btnMenu.setOnClickListener { view -> onMenuClick(item, view) }
            updateProgress()
        }

        fun updateProgress() {
            val pos = bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return
            val item = getItem(pos)
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

            val context = binding.root.context
            val colorRes = when (progress) {
                in 0..24 -> R.color.progressSoftGreen
                in 25..49 -> R.color.progressCyanBlue
                in 50..74 -> R.color.progressAmber
                else -> R.color.colorDanger
            }
            val color = context.getColor(colorRes)
            binding.deadlineProgress.setIndicatorColor(color)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemDeadlineModernBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    // Per-item timers removed; nothing to clean on recycle

    class DeadlineDiffCallback : DiffUtil.ItemCallback<DeadlineItem>() {
        override fun areItemsTheSame(oldItem: DeadlineItem, newItem: DeadlineItem): Boolean {
            return oldItem.createdAt == newItem.createdAt
        }

        override fun areContentsTheSame(oldItem: DeadlineItem, newItem: DeadlineItem): Boolean {
            return oldItem == newItem
        }
    }
}
