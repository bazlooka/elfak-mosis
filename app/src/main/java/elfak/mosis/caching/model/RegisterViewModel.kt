package elfak.mosis.caching.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import elfak.mosis.caching.data.NewUser

/**
 * Created by Luka Kocić on 08-Jul-23.
 */
class RegisterViewModel : ViewModel() {
    private val _newUser = MutableLiveData(NewUser())
    val newUser: LiveData<NewUser> get() = _newUser
}