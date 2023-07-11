package elfak.mosis.caching.data

/**
 * Created by Luka Kocić on 11-Jul-23.
 */
data class LogWithAuthor(
    val logId: String,
    val log: Log,
    val author: User
) : Comparable<LogWithAuthor> {
    override fun compareTo(other: LogWithAuthor): Int {
        return log.compareTo(other.log)
    }
}