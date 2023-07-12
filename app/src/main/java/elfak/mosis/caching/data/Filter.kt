package elfak.mosis.caching.data

/**
 * Created by Luka Kocić on 12-Jul-23.
 */
data class Filter(
    val radius: Int,
    val selectedTypes: Array<CacheType>,
    val desc: String,
    val startTime: Long?,
    val endTime: Long?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Filter

        if (radius != other.radius) return false
        if (!selectedTypes.contentEquals(other.selectedTypes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = radius
        result = 31 * result + selectedTypes.contentHashCode()
        return result
    }
}