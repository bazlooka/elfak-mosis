package elfak.mosis.caching.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import elfak.mosis.caching.R
import elfak.mosis.caching.data.Cache
import elfak.mosis.caching.data.CacheType
import elfak.mosis.caching.databinding.FragmentCreateCacheBinding
import java.time.LocalDateTime

class CreateCacheFragment : Fragment() {

    private lateinit var binding: FragmentCreateCacheBinding
    private var imageUri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFragmentResultListener(CameraFragment.REQUEST_PHOTO) { _, bundle ->
            val result = bundle.getString(CameraFragment.PHOTO_URI)
            Glide.with(requireContext()).load(result).into(binding.imageView3)
            imageUri = result
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCreateCacheBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.title = "Novi keš"

        binding.imageView3.setOnClickListener {
            findNavController().navigate(R.id.action_createCacheFragment_to_cameraFragment)
        }

        binding.button4.setOnClickListener {
            onCreateCachePressed()
        }
    }

    private fun onCreateCachePressed() {
        if (imageUri == null) {
            Toast.makeText(
                requireContext(),
                "Molimo izaberite fotografiju",
                Toast.LENGTH_SHORT,
            ).show()
            return
        }
        if (binding.editTextTextMultiLine.text.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Molimo popunite opis",
                Toast.LENGTH_SHORT,
            ).show()
            return
        }
        createCache()
    }

    private fun createCache() {
        val locationManager =
            requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
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
        val currLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        val currUser = Firebase.auth.currentUser

        if (currLocation == null || currUser == null) {
            return
        }

        val cacheType = when (binding.radioGroup.checkedRadioButtonId) {
            R.id.rbEasy -> {
                CacheType.EASY
            }

            R.id.rbMedium -> {
                CacheType.MEDIUM
            }

            R.id.rbHard -> {
                CacheType.HARD
            }

            else -> {
                throw IllegalStateException()
            }
        }
        val desc = binding.editTextTextMultiLine.text.toString()
        val currTime = LocalDateTime.now()

        val cache = Cache(
            cacheType,
            desc,
            currUser.uid,
            currTime,
            currTime,
            currLocation.latitude,
            currLocation.longitude
        )

        val db = Firebase.firestore
        db.collection("caches")
            .add(cache)
            .addOnSuccessListener {
                val cacheId = it.id
                val imagePath = "cache/$cacheId.jpg"
                val photoRef = Firebase.storage.reference.child(imagePath)
                val photoLocalUri = Uri.parse(imageUri)
                photoRef.putFile(photoLocalUri).addOnSuccessListener {
                    val cachesRef = Firebase.database.getReference("cacheLocations")
                    val geoFire = GeoFire(cachesRef)
                    val geoLocation = GeoLocation(currLocation.latitude, currLocation.longitude)
                    geoFire.setLocation(
                        cacheId, geoLocation
                    ) { _, error ->
                        if (error == null) {
                            Toast.makeText(
                                requireContext(),
                                "Keš uspešno sačuvan!",
                                Toast.LENGTH_SHORT,
                            ).show()
                            findNavController().popBackStack()
                        }
                    }
                }
            }
    }
}