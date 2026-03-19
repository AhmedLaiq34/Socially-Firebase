package com.example.i230657

data class Comment(
    val commentId: String = "",
    val postId: String = "",
    val userId: String = "",
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
