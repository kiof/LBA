package com.kiof.lbaklaxon

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.kiof.lbaklaxon.databinding.ListItemBinding

class SoundboardAdapter(val items: List<Item>, val onClick: (View, Int) -> Unit, val onLongClick: (View, Int) -> Unit) :
    RecyclerView.Adapter<SoundboardAdapter.MyViewHolder>() {

    inner class MyViewHolder(
        val binding: ListItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.listItemButton.setOnClickListener {
                onClick(binding.root, bindingAdapterPosition)
            }
            binding.listItemButton.setOnLongClickListener {
                onLongClick(binding.root, bindingAdapterPosition)
                return@setOnLongClickListener true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = ListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentItem = items[position]
        holder.binding.listItemSection.text = currentItem.sectionText
        holder.binding.listItemSection.setCompoundDrawablesWithIntrinsicBounds(
            currentItem.sectionImage, 0,
            currentItem.sectionImage, 0
        )
        holder.binding.listItemButton.text = currentItem.buttonText
        holder.binding.listItemButton.setBackgroundResource(currentItem.buttonImage)
        if (currentItem.sectionText != "") {
            holder.binding.listItemSection.visibility = View.VISIBLE
            holder.binding.listItemButton.visibility = View.GONE
        } else {
            holder.binding.listItemSection.visibility = View.GONE
            holder.binding.listItemButton.visibility = View.VISIBLE
            holder.binding.listItemButton.startAnimation(AnimationUtils.loadAnimation(holder.itemView.context, R.anim.zoomin))
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }
}
