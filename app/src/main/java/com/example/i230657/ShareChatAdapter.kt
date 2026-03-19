package com.yourapp.adapters

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.i230657.R
import com.yourapp.models.ChatPreview
import de.hdodenhof.circleimageview.CircleImageView

class ShareChatAdapter(
    private val chatList: List<ChatPreview>,
    private val onChatClick: (ChatPreview) -> Unit
) : RecyclerView.Adapter<ShareChatAdapter.ShareChatViewHolder>() {

    inner class ShareChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: CircleImageView = itemView.findViewById(R.id.ivShareChatProfile)
        val username: TextView = itemView.findViewById(R.id.tvShareChatUsername)
        val displayName: TextView = itemView.findViewById(R.id.tvShareChatDisplayName)
        val checkmark: ImageView = itemView.findViewById(R.id.ivShareCheckmark)

        fun bind(chat: ChatPreview) {
            username.text = chat.username
            displayName.text = chat.displayName

            // Load profile image
            if (chat.profileImageBase64.isNotEmpty()) {
                try {
                    val bytes = Base64.decode(chat.profileImageBase64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    profileImage.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    profileImage.setImageResource(R.drawable.placeholder_pfp)
                }
            } else {
                profileImage.setImageResource(R.drawable.placeholder_pfp)
            }

            itemView.setOnClickListener {
                onChatClick(chat)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShareChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.share_chat_item, parent, false)
        return ShareChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShareChatViewHolder, position: Int) {
        holder.bind(chatList[position])
    }

    override fun getItemCount(): Int = chatList.size
}