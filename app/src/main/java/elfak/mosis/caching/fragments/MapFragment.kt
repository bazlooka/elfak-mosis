package elfak.mosis.caching.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQuery
import com.firebase.geofire.GeoQueryEventListener
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import elfak.mosis.caching.R
import elfak.mosis.caching.databinding.FragmentMapBinding
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay


class MapFragment : Fragment() {

    private lateinit var binding: FragmentMapBinding

    private lateinit var locationManager: LocationManager
    private lateinit var geoQuery: GeoQuery

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMenu()
        (requireActivity() as AppCompatActivity).supportActionBar?.title = "Mapa"

        binding.fabCreateCache.setOnClickListener {
            findNavController().navigate(R.id.action_mapFragment_to_createCacheFragment)
        }

        val ctx = requireContext()
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))

        val map = binding.map
        map.setMultiTouchControls(true)
        map.controller.setZoom(15.0)
        val startPoint = GeoPoint(43.3209, 21.8958)
        map.controller.setCenter(startPoint)
        map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

        if (isLocationPermissionGranted()) {
            setMyLocationOverlay()
            initLocationTracking()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    override fun onResume() {
        super.onResume()
        binding.map.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.map.onPause()
        geoQuery.removeAllListeners()
    }

    @SuppressLint("MissingPermission")
    private fun initLocationTracking() {
        locationManager =
            requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            0L,
            0f,
            locationListener
        )
        val cachesRef = Firebase.database.getReference("cacheLocations")
        val geoFire = GeoFire(cachesRef)
        val currLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

        if (currLocation != null) {
            binding.map.controller.setCenter(
                GeoPoint(
                    currLocation.latitude,
                    currLocation.longitude
                )
            )
            geoQuery = geoFire.queryAtLocation(
                GeoLocation(currLocation.latitude, currLocation.longitude),
                1000000.0
            )
            geoQuery.addGeoQueryEventListener(geoQueryEventListener)
        }
    }

    private val geoQueryEventListener: GeoQueryEventListener = object : GeoQueryEventListener {
        private var markers: HashMap<String, Marker> = HashMap()

        override fun onKeyEntered(key: String, location: GeoLocation) {
            val cacheMarker = Marker(binding.map)
            cacheMarker.position = GeoPoint(location.latitude, location.longitude)
            cacheMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            binding.map.overlays.add(cacheMarker)
            markers[key] = cacheMarker
        }

        override fun onKeyExited(key: String?) {
            binding.map.overlays.remove(markers[key])
            markers.remove(key)
        }

        override fun onKeyMoved(key: String, location: GeoLocation) {
            markers[key]?.position = GeoPoint(location.latitude, location.longitude)
        }

        override fun onGeoQueryReady() {
        }

        override fun onGeoQueryError(error: DatabaseError?) {
        }
    }

    private val locationListener: LocationListener = LocationListener {
        geoQuery.center = GeoLocation(it.latitude, it.longitude)
    }

    private fun setMyLocationOverlay() {
        val myLocationProvider = GpsMyLocationProvider(requireContext())
        val myLocationOverlay = MyLocationNewOverlay(myLocationProvider, binding.map)
        myLocationOverlay.enableMyLocation()
        binding.map.overlays.add(myLocationOverlay)
    }

    private fun isLocationPermissionGranted(): Boolean {
        val ctx = requireActivity()
        val granted = PackageManager.PERMISSION_GRANTED
        return ActivityCompat.checkSelfPermission(
            ctx,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == granted || ActivityCompat.checkSelfPermission(
            ctx,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == granted
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        )
        { isGranted ->
            if (isGranted) {
                setMyLocationOverlay()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Permission request denied",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                val item = menu.findItem(R.id.action_show_map)
                item.isVisible = false
            }

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_main, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_show_list -> {
                        findNavController().navigate(R.id.action_mapFragment_to_cacheListFragment)
                        true
                    }

                    R.id.action_show_leaderboard -> {
                        findNavController().navigate(R.id.action_mapFragment_to_leaderboardFragment)
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

    private fun showFilter() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_filter)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(true)
        dialog.show()
    }

    private fun showDatePicker() {
        val dateRangePicker =
            MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Kreirano")
                .setSelection(
                    androidx.core.util.Pair(
                        MaterialDatePicker.thisMonthInUtcMilliseconds(),
                        MaterialDatePicker.todayInUtcMilliseconds()
                    )
                )
                .build()
        dateRangePicker.show(parentFragmentManager, "tag")
    }
}