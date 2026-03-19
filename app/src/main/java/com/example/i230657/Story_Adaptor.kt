package com.example.i230657

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

class StoryAdapter(
    private val userStories: ArrayList<UserStories>,  // grouped stories per user
    private val onStoryClick: (UserStories) -> Unit
) : RecyclerView.Adapter<StoryAdapter.StoryViewHolder>() {

    private val database = FirebaseDatabase.getInstance().getReference("stories")
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    inner class StoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val storyImage: CircleImageView = itemView.findViewById(R.id.storyImage)
        val storyUsername: TextView = itemView.findViewById(R.id.storyLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.story_item_layout, parent, false)
        return StoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        val userStory = userStories[position]
        holder.storyUsername.text = userStory.username

        // Decode and display the user's profile picture
        if (!userStory.profilePictureBase64.isNullOrEmpty()) {
            try {
                val cleaned = userStory.profilePictureBase64.replace("\n", "").replace("\r", "").trim()
                val bytes = Base64.decode(cleaned, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.storyImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.storyImage.setImageResource(R.drawable.placeholder_pfp)
            }
        } else {
            holder.storyImage.setImageResource(R.drawable.placeholder_pfp)
        }

        // Default state → gray (assume viewed)
        holder.storyImage.setBackgroundResource(R.drawable.story_border_viewed)

        // Check Firebase for story view status
        if (currentUserId != null) {
            val userId = userStory.userId
            database.child(userId).child("stories")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var viewedAll = true
                        for (storySnap in snapshot.children) {
                            val viewedBy = storySnap.child("viewedBy").child(currentUserId)
                            if (!viewedBy.exists()) {
                                viewedAll = false
                                break
                            }
                        }

                        if (!viewedAll) {
                            // Unviewed → purple ring
                            holder.storyImage.setBackgroundResource(R.drawable.story_border_unviewed)
                        } else {
                            // Viewed → gray ring
                            holder.storyImage.setBackgroundResource(R.drawable.story_border_viewed)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        // Click → open story viewer
        holder.storyImage.setOnClickListener {
            onStoryClick(userStory)
        }
    }

    override fun getItemCount(): Int = userStories.size
}
