package com.example.i230657

import User
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView
import android.graphics.Typeface
import android.graphics.BitmapFactory
import android.util.Base64

class CommentAdapter(
    private val comments: ArrayList<Pair<Comment, User>>
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: CircleImageView = itemView.findViewById(R.id.civCommenter)
        val commentText: TextView = itemView.findViewById(R.id.commentText)

        val username: TextView = itemView.findViewById(R.id.usernameText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.comment_item, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val (comment, user) = comments[position]

        // Set profile image
        if (user.profilePictureUrl.isNotEmpty()) {
            val profileBytes = Base64.decode(user.profilePictureUrl, Base64.DEFAULT)
            val profileBitmap = BitmapFactory.decodeByteArray(profileBytes, 0, profileBytes.size)
            holder.profileImage.setImageBitmap(profileBitmap)
        } else {
            holder.profileImage.setImageResource(R.drawable.placeholder_pfp)
        }

        // Set username in bold + comment text
        val fullText = "${user.username} ${comment.text}"
        val username = user.username
        val spannable = SpannableString(comment.text)

        holder.commentText.text = spannable

        holder.username.text = username
    }

    override fun getItemCount(): Int = comments.size

    // Optional helper to add a comment dynamically
    fun addComment(comment: Pair<Comment, User>) {
        comments.add(comment)
        notifyItemInserted(comments.size - 1)
    }
}
