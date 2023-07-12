package elfak.mosis.caching.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.os.bundleOf
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQuery
import com.firebase.geofire.GeoQueryEventListener
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import elfak.mosis.caching.R
import elfak.mosis.caching.data.CacheType
import elfak.mosis.caching.data.Filter
import elfak.mosis.caching.databinding.FragmentMapBinding
import elfak.mosis.caching.model.FilterViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlin.math.pow
import kotlin.math.roundToInt


class MapFragment : Fragment() {

    private lateinit var binding: FragmentMapBinding

    private lateinit var locationManager: LocationManager
    private var geoQuery: GeoQuery? = null

    private lateinit var pinEasy: Drawable
    private lateinit var pinMedium: Drawable
    private lateinit var pinHard: Drawable

    private val filterViewModel: FilterViewModel by viewModels()

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

        pinEasy = AppCompatResources.getDrawable(requireContext(), R.drawable.pin_easy)!!
        pinMedium = AppCompatResources.getDrawable(requireContext(), R.drawable.pin_medium)!!
        pinHard = AppCompatResources.getDrawable(requireContext(), R.drawable.pin_hard)!!

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
        geoQuery?.removeAllListeners()
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
            geoQuery?.addGeoQueryEventListener(geoQueryEventListener)
        }
    }

    private val geoQueryEventListener: GeoQueryEventListener = object : GeoQueryEventListener {
        private var markers: HashMap<String, Marker> = HashMap()

        override fun onKeyEntered(key: String, location: GeoLocation) {
            Firebase.firestore.document("caches/$key")
                .get()
                .addOnSuccessListener {
                    val type = it.getString("type")
                    if (type != null) {
                        val cacheMarker = Marker(binding.map)
                        cacheMarker.position = GeoPoint(location.latitude, location.longitude)
                        cacheMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        binding.map.overlays.add(cacheMarker)
                        markers[key] = cacheMarker
                        cacheMarker.id = key
                        cacheMarker.icon = when (CacheType.valueOf(type)) {
                            CacheType.EASY -> pinEasy
                            CacheType.MEDIUM -> pinMedium
                            CacheType.HARD -> pinHard
                        }
                        cacheMarker.setOnMarkerClickListener { marker, map ->
                            val bundle = bundleOf("cacheId" to marker.id)
                            map.findNavController()
                                .navigate(R.id.action_mapFragment_to_cacheFragment, bundle)
                            return@setOnMarkerClickListener true
                        }
                    }
                }
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
        geoQuery?.center = GeoLocation(it.latitude, it.longitude)
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

        val label = dialog.findViewById<TextView>(R.id.tvFilterRadius)

        val sbRadius = dialog.findViewById<SeekBar>(R.id.sbRadius)
        val progress = filterViewModel.filter.value?.radius ?: 100
        sbRadius.progress = progress
        label.text = getRadiusString(progress)
        sbRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            @SuppressLint("SetTextI18n")
            override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                label.text = getRadiusString(progress)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {

            }

        })

        dialog.findViewById<Button>(R.id.btnAppyFilter)
            .setOnClickListener {
                val progress = sbRadius.progress
                val dProgress = progress / 100.0
                val radius =
                    if (progress == 100)
                        8587.0
                    else
                        dProgress.pow(5) * 8587.0
                geoQuery?.radius = radius

                val filter = Filter(
                    progress,
                    arrayOf(CacheType.EASY),
                    "",
                    "",
                    0,
                    0
                )
                filterViewModel.setFilter(filter)

                dialog.dismiss()
            }
        dialog.show()
    }

    private fun getRadiusString(progress: Int): String {
        val dProgress = progress / 100.0
        val radius = dProgress.pow(5) * 8587.0
        return if (progress == 100) {
            "sve"
        } else {
            if (radius < 1.0) {
                String.format("%d m", (radius * 1000.0).roundToInt())
            } else {
                String.format("%.3f km", radius)
            }
        }
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