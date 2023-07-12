package elfak.mosis.caching.data

/**
 * Created by Luka Kocić on 12-Jul-23.
 */
class CacheWithId(
    val cache: Cache,
    val id: String,
    val distance: Float
) : Comparable<CacheWithId> {
    override fun compareTo(other: CacheWithId): Int {
        return distance.compareTo(other.distance)
    }

}