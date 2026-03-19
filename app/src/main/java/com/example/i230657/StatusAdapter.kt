package com.example.i230657

import User
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import de.hdodenhof.circleimageview.CircleImageView

class StatusAdapter(
    private val users: List<User>, // this will be the list of following users
    private val onUserClick: (User) -> Unit
) : RecyclerView.Adapter<StatusAdapter.StatusViewHolder>() {

    inner class StatusViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: CircleImageView = itemView.findViewById(R.id.storyImage)
        val username: TextView = itemView.findViewById(R.id.storyLabel)
        val statusIndicator: View = itemView.findViewById(R.id.onlineStatusIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatusViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.status_item_layout, parent, false)
        return StatusViewHolder(view)
    }

    override fun onBindViewHolder(holder: StatusViewHolder, position: Int) {
        val user = users[position]

        // Set username
        holder.username.text = user.username

        // Decode Base64 profile picture
        if (!user.profilePictureUrl.isNullOrEmpty()) {
            try {
                val cleaned = user.profilePictureUrl.replace("\n", "").replace("\r", "").trim()
                val bytes = Base64.decode(cleaned, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.profileImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.profileImage.setImageResource(R.drawable.placeholder_pfp)
            }
        } else {
            holder.profileImage.setImageResource(R.drawable.placeholder_pfp)
        }

        // Online/offline indicator
        if (user.isOnline) {
            holder.statusIndicator.setBackgroundResource(R.drawable.status_circle_online)
        } else {
            holder.statusIndicator.setBackgroundResource(R.drawable.status_circle_offline)
        }

        // Handle click
        holder.itemView.setOnClickListener {
            onUserClick(user)
        }
    }

    override fun getItemCount(): Int = users.size
}

