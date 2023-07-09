package elfak.mosis.caching.data

import java.time.LocalDateTime

/**
 * Created by Luka Kocić on 09-Jul-23.
 */
data class Cache(
    var key: String?,
    var type: CacheType,
    var desc: String,
    var authorUid: String,
    var datePublished: LocalDateTime,
    var dateLastUpdated: LocalDateTime,
    var geoHash: String,
    var lat: Double,
    var lng: Double
)