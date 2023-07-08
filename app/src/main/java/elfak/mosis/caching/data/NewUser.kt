package elfak.mosis.caching.data

/**
 * Created by Luka Kocić on 08-Jul-23.
 */
data class NewUser(
    var username: String, var firstName: String, var lastName: String,
    var phoneNumber: String, var localPhotoPath: String, var password: String
) {
    constructor() : this("", "", "", "", "", "")
}