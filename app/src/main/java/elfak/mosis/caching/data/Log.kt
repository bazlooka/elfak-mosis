package elfak.mosis.caching.data

import java.util.Date

/**
 * Created by Luka Kocić on 09-Jul-23.
 */
data class Log(
    var cacheId: String,
    var authorUid: String,
    var text: String,
    var dateLogged: Date
)
