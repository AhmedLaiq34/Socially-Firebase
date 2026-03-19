package com.example.i230657

import User
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth

class SearchAdapter(
    private val context: Context,
    private var userList: List<User>
) : RecyclerView.Adapter<SearchAdapter.UserViewHolder>() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.default_profile)
        val displayName: TextView = itemView.findViewById(R.id.tvDisplayName)
        val username: TextView = itemView.findViewById(R.id.tvUsername)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_search, parent, false)
        return UserViewHolder(view)
    }

    override fun getItemCount(): Int = userList.size

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]

        // Set displayName & username
        holder.displayName.text = user.displayName
        holder.username.text = user.username

        // Decode base64 profile image (if exists)
        if (user.profilePictureUrl.isNotEmpty()) {
            try {
                val imageBytes = Base64.decode(user.profilePictureUrl, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                holder.profileImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.profileImage.setImageResource(R.drawable.placeholder_pfp) // fallback
            }
        } else {
            holder.profileImage.setImageResource(R.drawable.placeholder_pfp)
        }

        // On user click → go to correct page
        holder.itemView.setOnClickListener {
            if (user.userId == currentUserId) {
                // If it's the logged-in user → profile_page
                val intent = Intent(context, profile_page::class.java)
                context.startActivity(intent)
            } else {
                // Otherwise → celebrity_follow_page
                val intent = Intent(context, celebrity_follow_page::class.java)
                intent.putExtra("userId", user.userId) // pass UID to load profile
                context.startActivity(intent)
            }
        }
    }

    fun updateList(newList: List<User>) {
        userList = newList
        notifyDataSetChanged()
    }
}
