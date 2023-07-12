package elfak.mosis.caching.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import elfak.mosis.caching.data.CacheType
import elfak.mosis.caching.data.Filter

/**
 * Created by Luka Kocić on 12-Jul-23.
 */
class FilterViewModel : ViewModel() {
    private val _filter = MutableLiveData(
        Filter(
            100,
            arrayOf(CacheType.EASY, CacheType.MEDIUM, CacheType.HARD),
            "",
            "",
            0,
            Int.MAX_VALUE
        )
    )
    val filter: LiveData<Filter> get() = _filter

    fun setFilter(f: Filter) {
        _filter.value = f
    }
}