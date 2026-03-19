data class User(
    var userId: String = "",
    var email: String = "",
    var username: String = "",
    var displayName: String = "",
    var firstName: String = "",
    var lastName: String = "",
    var dateOfBirth: String = "",
    var phoneNumber: String = "",
    var bio: String = "",
    var profilePictureUrl: String = "",
    var gender: String = "",
    var website: String = "",
    var accountPrivate: Boolean = false,
    var createdAt: Long = System.currentTimeMillis(),
    var isOnline: Boolean = true,
    var lastSeen: Long = System.currentTimeMillis(),
    var fcmToken: String = "",
    var stats: UserStats = UserStats(),
    var followers: Map<String, Boolean> = emptyMap(),
    var following: Map<String, Boolean> = emptyMap()
) {
    constructor() : this("", "", "", "", "", "", "", "", "", "", "", "", false, 0L, true, 0L, "", UserStats(), emptyMap(), emptyMap())
}

data class UserStats(
    var postCount: Int = 0,
    var followerCount: Int = 0,
    var followingCount: Int = 0
)
