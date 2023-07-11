package elfak.mosis.caching.data

/**
 * Created by Luka Kocić on 09-Jul-23.
 */
data class Log(
    var authorUid: String,
    var text: String,
    var dateLogged: Long,
    var found: Boolean
) : Comparable<Log> {
    constructor() : this("", "", 0, false)

    override fun compareTo(other: Log): Int {
        return dateLogged.compareTo(other.dateLogged)
    }
}
