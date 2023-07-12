package elfak.mosis.caching.services

import elfak.mosis.caching.data.Filter

/**
 * Created by Luka Kocić on 12-Jul-23.
 */
interface FilterServiceCallback {
    fun success(filter: Filter, radius: Double)
}