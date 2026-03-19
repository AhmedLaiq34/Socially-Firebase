package com.yourapp.adapters

import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.i230657.R
import com.example.i230657.post_view_page
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.yourapp.models.Message
import de.hdodenhof.circleimageview.CircleImageView

class MessageAdapter(
    private val messageList: MutableList<Message>,
    private val onMessageLongClick: (Message) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val MSG_TYPE_SENT = 1
    private val MSG_TYPE_RECEIVED = 2
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    override fun getItemViewType(position: Int): Int {
        val message = messageList[position]
        return if (message.senderId == currentUserId) MSG_TYPE_SENT else MSG_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == MSG_TYPE_SENT) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_sent, parent, false)
            SentMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_received, parent, false)
            ReceivedMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messageList[position]
        if (holder is SentMessageViewHolder) {
            holder.bind(message)
        } else if (holder is ReceivedMessageViewHolder) {
            holder.bind(message)
        }

        holder.itemView.setOnLongClickListener {
            onMessageLongClick(message)
            true
        }
    }

    override fun getItemCount(): Int = messageList.size

    // ---------- Sent ----------
    inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.text_message_sent)
        private val timeText: TextView = itemView.findViewById(R.id.text_message_time_sent)
        private val imageMessage: ImageView = itemView.findViewById(R.id.image_message_sent)

        // Post-related views
        private val postContainer: ViewGroup? = itemView.findViewById(R.id.postItemContainer)
        private val postImage: ImageView? = itemView.findViewById(R.id.vpPostImages)
        private val postProfilePicture: CircleImageView? = itemView.findViewById(R.id.postProfileImage)
        private val postCaption: TextView? = itemView.findViewById(R.id.postCaption)
        private val postUsername: TextView? = itemView.findViewById(R.id.tvUsername)

        fun bind(message: Message) {
            when (message.type) {
                "image" -> {
                    showImageMessage(message)
                }
                "post" -> {
                    showPostMessage(message)
                }
                else -> {
                    showTextMessage(message)
                }
            }
            timeText.text = formatTime(message.timestamp)
        }

        private fun showTextMessage(message: Message) {
            messageText.visibility = View.VISIBLE
            imageMessage.visibility = View.GONE
            postContainer?.visibility = View.GONE

            messageText.text = message.messageText
        }

        private fun showImageMessage(message: Message) {
            messageText.visibility = View.GONE
            imageMessage.visibility = View.VISIBLE
            postContainer?.visibility = View.GONE

            try {
                val cleaned = message.imageUrl.replace("\n", "").replace("\r", "").trim()
                val bytes = Base64.decode(cleaned, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                imageMessage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                imageMessage.setImageResource(R.drawable.placeholder_pfp)
            }
        }

        private fun showPostMessage(message: Message) {
            messageText.visibility = View.GONE
            imageMessage.visibility = View.GONE
            postContainer?.visibility = View.VISIBLE

            message.post?.let { post ->
                try {
                    // Set profile picture
                    val cleanedPfp = post.userProfilePicture.replace("\n", "").replace("\r", "").trim()
                    val bytesPfp = Base64.decode(cleanedPfp, Base64.DEFAULT)
                    val bitmapPfp = BitmapFactory.decodeByteArray(bytesPfp, 0, bytesPfp.size)
                    postProfilePicture?.setImageBitmap(bitmapPfp)

                    // Set post image
                    val cleanedImg = post.postImage.replace("\n", "").replace("\r", "").trim()
                    val bytesImg = Base64.decode(cleanedImg, Base64.DEFAULT)
                    val bitmapImg = BitmapFactory.decodeByteArray(bytesImg, 0, bytesImg.size)
                    postImage?.setImageBitmap(bitmapImg)

                    // Set caption
                    postCaption?.text = post.postCaption

                    // Retrieve and set username
                    fetchUsername(post.userId)

                    // CLICK HANDLER — open post view
                    postContainer?.setOnClickListener {
                        val intent = Intent(itemView.context, post_view_page::class.java)
                        intent.putExtra("postId", post.postId)
                        itemView.context.startActivity(intent)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    postProfilePicture?.setImageResource(R.drawable.placeholder_pfp)
                }
            }
        }

        private fun fetchUsername(userId: String) {
            val userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("profile")
                .child("username")

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val username = snapshot.getValue(String::class.java) ?: userId
                    postUsername?.text = username
                }

                override fun onCancelled(error: DatabaseError) {
                    postUsername?.text = userId // Fallback to userId
                }
            })
        }
    }

    // ---------- Received ----------
    inner class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.text_message_received)
        private val timeText: TextView = itemView.findViewById(R.id.text_message_time_received)
        private val imageMessage: ImageView = itemView.findViewById(R.id.image_message_received)

        // Post-related views
        private val postContainer: ViewGroup? = itemView.findViewById(R.id.postItemContainer)
        private val postImage: ImageView? = itemView.findViewById(R.id.vpPostImages)
        private val postProfilePicture: CircleImageView? = itemView.findViewById(R.id.postProfileImage)
        private val postCaption: TextView? = itemView.findViewById(R.id.postCaption)
        private val postUsername: TextView? = itemView.findViewById(R.id.tvUsername)

        fun bind(message: Message) {
            when (message.type) {
                "image" -> {
                    showImageMessage(message)
                }
                "post" -> {
                    showPostMessage(message)
                }
                else -> {
                    showTextMessage(message)
                }
            }
            timeText.text = formatTime(message.timestamp)
        }

        private fun showTextMessage(message: Message) {
            messageText.visibility = View.VISIBLE
            imageMessage.visibility = View.GONE
            postContainer?.visibility = View.GONE

            messageText.text = message.messageText
        }

        private fun showImageMessage(message: Message) {
            messageText.visibility = View.GONE
            imageMessage.visibility = View.VISIBLE
            postContainer?.visibility = View.GONE

            try {
                val cleaned = message.imageUrl.replace("\n", "").replace("\r", "").trim()
                val bytes = Base64.decode(cleaned, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                imageMessage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                imageMessage.setImageResource(R.drawable.placeholder_pfp)
            }
        }

        private fun showPostMessage(message: Message) {
            messageText.visibility = View.GONE
            imageMessage.visibility = View.GONE
            postContainer?.visibility = View.VISIBLE

            message.post?.let { post ->
                try {
                    // Set profile picture
                    val cleanedPfp = post.userProfilePicture.replace("\n", "").replace("\r", "").trim()
                    val bytesPfp = Base64.decode(cleanedPfp, Base64.DEFAULT)
                    val bitmapPfp = BitmapFactory.decodeByteArray(bytesPfp, 0, bytesPfp.size)
                    postProfilePicture?.setImageBitmap(bitmapPfp)

                    // Set post image
                    val cleanedImg = post.postImage.replace("\n", "").replace("\r", "").trim()
                    val bytesImg = Base64.decode(cleanedImg, Base64.DEFAULT)
                    val bitmapImg = BitmapFactory.decodeByteArray(bytesImg, 0, bytesImg.size)
                    postImage?.setImageBitmap(bitmapImg)

                    // Set caption
                    postCaption?.text = post.postCaption

                    // Retrieve and set username
                    fetchUsername(post.userId)

                } catch (e: Exception) {
                    e.printStackTrace()
                    postProfilePicture?.setImageResource(R.drawable.placeholder_pfp)
                }
            }
        }

        private fun fetchUsername(userId: String) {
            val userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("profile")
                .child("username")

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val username = snapshot.getValue(String::class.java) ?: userId
                    postUsername?.text = username
                }

                override fun onCancelled(error: DatabaseError) {
                    postUsername?.text = userId // Fallback to userId
                }
            })
        }
    }

    // ---------- Time ----------
    private fun formatTime(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        return sdf.format(date)
    }
}