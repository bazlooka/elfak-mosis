package elfak.mosis.caching.data

import com.google.firebase.Timestamp

/**
 * Created by Luka Kocić on 09-Jul-23.
 */
data class Cache(
    var type: CacheType,
    var desc: String,
    var authorUid: String,
    var datePublished: Timestamp?,
    var dateLastUpdated: Timestamp?,
    var lat: Double,
    var lng: Double
) {
    constructor() : this(
        CacheType.EASY, "", "",
        null, null, 0.0, 0.0
    ) {
    }
}