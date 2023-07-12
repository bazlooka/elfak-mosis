package elfak.mosis.caching.services

import elfak.mosis.caching.data.Cache
import elfak.mosis.caching.data.Filter

/**
 * Created by Luka Kocić on 12-Jul-23.
 */
class FilterService {
    companion object {
        fun filterCache(filter: Filter, cache: Cache): Boolean {
            if (cache.type !in filter.selectedTypes) {
                return false
            }
            if (filter.startTime != null && filter.endTime != null
                && (cache.datePublished!! < filter.startTime || cache.datePublished!! > filter.endTime)
            ) {
                return false
            }
            if (!cache.desc.contains(filter.desc)) {
                return false
            }
            return true
        }
    }
}