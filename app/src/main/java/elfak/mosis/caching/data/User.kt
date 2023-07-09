package elfak.mosis.caching.data

/**
 * Created by Luka Kocić on 07-Jul-23.
 */
data class User(
    var uid: String,
    var username: String,
    var firstName: String,
    var lastName: String,
    var phoneNumber: String,
    var photoPath: String,
    var score: Int
)