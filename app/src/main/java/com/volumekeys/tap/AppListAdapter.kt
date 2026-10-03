package com.volumekeys.tap

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.volumekeys.tap.databinding.ItemAppBinding

/**
 * RecyclerView adapter for [AppInfo]. Emits a click callback when an app is picked.
 */
class AppListAdapter(
    private val onClick: (AppInfo) -> Unit,
) : ListAdapter<AppInfo, AppListAdapter.AppVH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppVH {
        val binding = ItemAppBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AppVH(binding)
    }

    override fun onBindViewHolder(holder: AppVH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AppVH(private val binding: ItemAppBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AppInfo) {
            binding.appIcon.setImageDrawable(item.icon)
            binding.appName.text = item.label
            binding.appPackage.text = item.packageName
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<AppInfo>() {
            override fun areItemsTheSame(o: AppInfo, n: AppInfo) = o.packageName == n.packageName
            override fun areContentsTheSame(o: AppInfo, n: AppInfo) = o == n
        }
    }
}
