package com.example.i230657

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.FirebaseDatabase
import de.hdodenhof.circleimageview.CircleImageView


class FollowRequestAdapter(
    private val requests: List<Notification>,
    private val onAccept: (Notification) -> Unit,
    private val onReject: (Notification) -> Unit
) : RecyclerView.Adapter<FollowRequestAdapter.RequestViewHolder>() {

    inner class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: CircleImageView = itemView.findViewById(R.id.default_profile)
        val messageText: TextView = itemView.findViewById(R.id.tvDisplayName)
        val btnAccept: MaterialButton = itemView.findViewById(R.id.btnAccept)
        val btnReject: MaterialButton = itemView.findViewById(R.id.btnReject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_follow_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val notif = requests[position]

        // Fetch sender info (username + pfp) from Users node
        val userRef = FirebaseDatabase.getInstance()
            .getReference("users").child(notif.fromUserId)

        userRef.get().addOnSuccessListener { snapshot ->
            val profileSnap = snapshot.child("profile")
            val username = profileSnap.child("username").value?.toString() ?: "Unknown"
            val pfp = profileSnap.child("profilePictureUrl").value?.toString() ?: ""

            holder.messageText.text = "$username sent you a follow request!"

            if (pfp.isNotEmpty()) {
                try {
                    val imageBytes = Base64.decode(pfp, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    holder.profileImage.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    holder.profileImage.setImageResource(R.drawable.placeholder_pfp)
                }
            } else {
                holder.profileImage.setImageResource(R.drawable.placeholder_pfp)
            }
        }


        holder.btnAccept.setOnClickListener { onAccept(notif) }
        holder.btnReject.setOnClickListener { onReject(notif) }
    }

    override fun getItemCount(): Int = requests.size
}
