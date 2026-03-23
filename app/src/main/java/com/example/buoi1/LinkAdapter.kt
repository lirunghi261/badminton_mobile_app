package com.example.buoi1

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
data class Link(
    val title: String,
    val url: String
)

class LinkAdapter(private val context: Context, private val dataSource: List<Link>) : BaseAdapter() {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {
        return dataSource.size
    }

    override fun getItem(position: Int): Any {
        return dataSource[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val binding: com.example.buoi1.databinding.ItemLinkBinding
        if (convertView == null) {
            binding = com.example.buoi1.databinding.ItemLinkBinding.inflate(inflater, parent, false)
            binding.root.tag = binding
        } else {
            binding = convertView.tag as com.example.buoi1.databinding.ItemLinkBinding
        }

        val link = getItem(position) as Link
        binding.tvTitle.text = link.title
        binding.tvUrl.text = link.url

        return binding.root
    }
}
