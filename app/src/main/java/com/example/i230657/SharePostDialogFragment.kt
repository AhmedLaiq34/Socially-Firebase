package com.example.i230657

import User
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.yourapp.adapters.ShareChatAdapter
import com.yourapp.models.ChatPreview
import com.yourapp.models.Message
import com.yourapp.models.SharedPost

class SharePostDialogFragment : DialogFragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ShareChatAdapter
    private lateinit var emptyText: TextView
    private lateinit var searchEdit: EditText

    private var chatList = mutableListOf<ChatPreview>()
    private var filteredChatList = mutableListOf<ChatPreview>()

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    // Post data to share
    private var postId: String = ""
    private var userId: String = ""
    private var userProfilePicture: String = ""
    private var postCaption: String = ""
    private var postImage: String = ""
    private var timestamp: Long = 0L

    companion object {
        fun newInstance(
            postId: String,
            userId: String,
            userProfilePicture: String,
            postCaption: String,
            postImage: String,
            timestamp: Long
        ): SharePostDialogFragment {
            val fragment = SharePostDialogFragment()
            val args = Bundle()
            args.putString("postId", postId)
            args.putString("userId", userId)
            args.putString("userProfilePicture", userProfilePicture)
            args.putString("postCaption", postCaption)
            args.putString("postImage", postImage)
            args.putLong("timestamp", timestamp)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Get post data from arguments
        arguments?.let {
            postId = it.getString("postId", "")
            userId = it.getString("userId", "")
            userProfilePicture = it.getString("userProfilePicture", "")
            postCaption = it.getString("postCaption", "")
            postImage = it.getString("postImage", "")
            timestamp = it.getLong("timestamp", 0L)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_share_post, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews(view)
        setupRecyclerView()
        setupSearch()
        fetchChats()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupViews(view: View) {
        view.findViewById<ImageView>(R.id.btnCloseDialog).setOnClickListener {
            dismiss()
        }

        recyclerView = view.findViewById(R.id.rvShareChats)
        emptyText = view.findViewById(R.id.tvEmptyChats)
        searchEdit = view.findViewById(R.id.etSearchChats)
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = ShareChatAdapter(filteredChatList) { selectedChat ->
            sharePostToChat(selectedChat)
        }
        recyclerView.adapter = adapter
    }

    private fun setupSearch() {
        searchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterChats(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterChats(query: String) {
        filteredChatList.clear()
        if (query.isEmpty()) {
            filteredChatList.addAll(chatList)
        } else {
            val lowercaseQuery = query.lowercase()
            filteredChatList.addAll(
                chatList.filter {
                    it.username.lowercase().contains(lowercaseQuery) ||
                            it.displayName.lowercase().contains(lowercaseQuery)
                }
            )
        }
        adapter.notifyDataSetChanged()
        updateEmptyState()
    }

    private fun fetchChats() {
        val currentUserId = auth.currentUser?.uid ?: return

        val userChatsRef = database.getReference("user_chats").child(currentUserId)
        val usersRef = database.getReference("users")

        userChatsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                chatList.clear()

                if (!snapshot.exists()) {
                    filteredChatList.clear()
                    adapter.notifyDataSetChanged()
                    updateEmptyState()
                    return
                }

                var processedCount = 0
                val totalChats = snapshot.childrenCount.toInt()

                for (chatSnapshot in snapshot.children) {
                    val partnerUserId = chatSnapshot.key ?: continue

                    usersRef.child(partnerUserId).child("profile")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(profileSnap: DataSnapshot) {
                                val username = profileSnap.child("username").getValue(String::class.java) ?: "Unknown"
                                val profileImageBase64 = profileSnap.child("profilePictureUrl").getValue(String::class.java) ?: ""
                                val displayName = profileSnap.child("displayName").getValue(String::class.java) ?: ""

                                val chatPreview = ChatPreview(
                                    userId = partnerUserId,
                                    username = username,
                                    displayName = displayName,
                                    profileImageBase64 = profileImageBase64,
                                    lastMessage = "",
                                    lastMessageTime = ""
                                )

                                chatList.add(chatPreview)
                                processedCount++

                                if (processedCount == totalChats) {
                                    filteredChatList.clear()
                                    filteredChatList.addAll(chatList)
                                    adapter.notifyDataSetChanged()
                                    updateEmptyState()
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                processedCount++
                                if (processedCount == totalChats) {
                                    updateEmptyState()
                                }
                            }
                        })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error loading chats", Toast.LENGTH_SHORT).show()
                updateEmptyState()
            }
        })
    }

    private fun sharePostToChat(chat: ChatPreview) {
        val currentUserId = auth.currentUser?.uid ?: return
        val receiverId = chat.userId

        // Create SharedPost object
        val sharedPost = SharedPost(
            postId = postId,
            userId = userId,
            userProfilePicture = userProfilePicture,
            postCaption = postCaption,
            postImage = postImage,
            timestamp = timestamp
        )

        // Create Message with post type
        val message = Message(
            senderId = currentUserId,
            receiverId = receiverId,
            messageText = "",
            timestamp = System.currentTimeMillis(),
            type = "post",
            imageUrl = "",
            post = sharedPost,
            messageId = database.reference.push().key
        )

        // Save to both sender and receiver chat nodes
        val chatsRef = database.getReference("Chats")

        message.messageId?.let { msgId ->
            // Save to sender's chat
            chatsRef.child(currentUserId).child(receiverId).child(msgId).setValue(message)
                .addOnSuccessListener {
                    // Save to receiver's chat
                    chatsRef.child(receiverId).child(currentUserId).child(msgId).setValue(message)
                        .addOnSuccessListener {
                            // Update user_chats for both users
                            updateUserChats(currentUserId, receiverId)
                            updateUserChats(receiverId, currentUserId)

                            Toast.makeText(requireContext(), "Post shared with ${chat.username}", Toast.LENGTH_SHORT).show()
                            dismiss()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Failed to share post", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to share post", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateUserChats(userId: String, partnerId: String) {
        database.getReference("user_chats")
            .child(userId)
            .child(partnerId)
            .setValue(true)
    }

    private fun updateEmptyState() {
        if (filteredChatList.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
}