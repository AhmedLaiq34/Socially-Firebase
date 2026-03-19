package com.example.i230657

import User
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView

class FollowingAdapter(
    private val followersList: List<User>
) : RecyclerView.Adapter<FollowingAdapter.FollowerViewHolder>() {

    class FollowerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: CircleImageView = itemView.findViewById(R.id.default_profile)
        val usernameText: TextView = itemView.findViewById(R.id.tvUsername)
        val displayNameText: TextView = itemView.findViewById(R.id.tvDisplayName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.following_item, parent, false)
        return FollowerViewHolder(view)
    }

    override fun onBindViewHolder(holder: FollowerViewHolder, position: Int) {
        val user = followersList[position]

        holder.usernameText.text = user.username
        holder.displayNameText.text = user.displayName

        // Decode Base64 image if available
        if (!user.profilePictureUrl.isNullOrEmpty()) {
            try {
                val imageBytes = Base64.decode(user.profilePictureUrl, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                holder.profileImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.profileImage.setImageResource(R.drawable.placeholder_pfp)
            }
        } else {
            holder.profileImage.setImageResource(R.drawable.placeholder_pfp)
        }

        val clickListener = View.OnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, celebrity_follow_page::class.java)
            intent.putExtra("userId", user.userId) // pass userId
            context.startActivity(intent)
        }

        holder.profileImage.setOnClickListener(clickListener)
        holder.usernameText.setOnClickListener(clickListener)
        holder.displayNameText.setOnClickListener(clickListener)
    }

    override fun getItemCount(): Int = followersList.size
}
