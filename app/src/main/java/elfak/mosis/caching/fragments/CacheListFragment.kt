package elfak.mosis.caching.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQuery
import com.firebase.geofire.GeoQueryEventListener
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import elfak.mosis.caching.MainActivity
import elfak.mosis.caching.R
import elfak.mosis.caching.adapters.CachesRecyclerAdapter
import elfak.mosis.caching.data.Cache
import elfak.mosis.caching.data.CacheWithId
import elfak.mosis.caching.data.Filter
import elfak.mosis.caching.databinding.FragmentCacheListBinding
import elfak.mosis.caching.services.FilterService
import elfak.mosis.caching.services.FilterServiceCallback

class CacheListFragment : Fragment() {

    private lateinit var binding: FragmentCacheListBinding

    private lateinit var locationManager: LocationManager
    private var geoQuery: GeoQuery? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerAdapter: CachesRecyclerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCacheListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()
        (requireActivity() as AppCompatActivity).supportActionBar?.title = "Lista keševa"
        recyclerView = view.findViewById(R.id.rvCacheList)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerAdapter = CachesRecyclerAdapter(ArrayList(10), requireContext())
        recyclerView.adapter = recyclerAdapter
        setupLocation()
    }

    private fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                val item = menu.findItem(R.id.action_show_list)
                item.isVisible = false
            }

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_main, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_show_map -> {
                        findNavController().popBackStack()
                        true
                    }

                    R.id.action_show_leaderboard -> {
                        findNavController().navigate(R.id.action_cacheListFragment_to_leaderboardFragment)
                        true
                    }

                    R.id.action_show_filter -> {
                        showFilter()
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupLocation() {
        val locationManager = requireActivity()
            .getSystemService(Context.LOCATION_SERVICE) as LocationManager
        this.locationManager = locationManager
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        locationManager.requestLocationUpdates(
            LocationManager.FUSED_PROVIDER,
            0L,
            0f,
            locationListener
        )

        val cachesRef = Firebase.database.getReference("cacheLocations")
        val geoFire = GeoFire(cachesRef)
        val currLocation = locationManager.getLastKnownLocation(LocationManager.FUSED_PROVIDER)

        if (currLocation != null) {
            geoQuery = geoFire.queryAtLocation(
                GeoLocation(currLocation.latitude, currLocation.longitude),
                1000000.0
            )
            geoQuery?.addGeoQueryEventListener(geoQueryEventListener)
        }
    }

    private val locationListener: LocationListener = LocationListener {
        geoQuery?.center = GeoLocation(it.latitude, it.longitude)
    }

    private var caches: HashMap<String, Cache> = HashMap()

    private val geoQueryEventListener: GeoQueryEventListener = object : GeoQueryEventListener {
        @SuppressLint("MissingPermission")
        override fun onKeyEntered(key: String, location: GeoLocation) {
            Firebase.firestore.document("caches/$key")
                .get()
                .addOnSuccessListener {
                    val cache = it.toObject(Cache::class.java)
                    if (cache != null) {
                        caches[key] = cache
                        val f = (activity as MainActivity).filterViewModel.filter.value
                        if (f != null && !FilterService.filterCache(f, cache)) {
                            return@addOnSuccessListener
                        }
                        val loc = Location("listLoc")
                        loc.longitude = location.longitude
                        loc.latitude = location.latitude
                        val distance = locationManager
                            .getLastKnownLocation(LocationManager.FUSED_PROVIDER)
                            ?.distanceTo(loc)

                        recyclerAdapter.add(CacheWithId(cache, key, distance!!))
                    }
                }
        }

        override fun onKeyExited(key: String?) {
            if (key != null) {
                recyclerAdapter.remove(key)
                caches.remove(key)
            }
        }

        override fun onKeyMoved(key: String, location: GeoLocation) {

        }

        override fun onGeoQueryReady() {
        }

        override fun onGeoQueryError(error: DatabaseError?) {
        }
    }

    override fun onPause() {
        super.onPause()
        geoQuery?.removeAllListeners()
    }

    private fun showFilter() {
        FilterService.showFilter(
            (activity as MainActivity).filterViewModel,
            this,
            object : FilterServiceCallback {
                @SuppressLint("MissingPermission")
                override fun success(filter: Filter, radius: Double) {
                    recyclerAdapter.deleteAll()
                    caches.forEach {
                        if (FilterService.filterCache(filter, it.value)) {
                            val loc = Location("listLoc")
                            loc.longitude = it.value.lng
                            loc.latitude = it.value.lat
                            val distance = locationManager
                                .getLastKnownLocation(LocationManager.FUSED_PROVIDER)
                                ?.distanceTo(loc)
                            recyclerAdapter.add(CacheWithId(it.value, it.key, distance ?: 0.0f))
                        }
                    }
                }
            })
    }
}