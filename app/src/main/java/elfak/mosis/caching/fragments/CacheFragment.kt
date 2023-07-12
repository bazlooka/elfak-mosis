package elfak.mosis.caching.fragments

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import elfak.mosis.caching.R
import elfak.mosis.caching.adapters.LogRecyclerAdapter
import elfak.mosis.caching.data.Cache
import elfak.mosis.caching.data.CacheType
import elfak.mosis.caching.data.Log
import elfak.mosis.caching.data.LogWithAuthor
import elfak.mosis.caching.data.User
import elfak.mosis.caching.databinding.FragmentCacheBinding
import java.text.DateFormat.getDateTimeInstance

class CacheFragment : Fragment() {

    private var cacheId: String? = null
    private var cache: Cache? = null

    private lateinit var recyclerAdapter: LogRecyclerAdapter

    private lateinit var binding: FragmentCacheBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCacheBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as AppCompatActivity).supportActionBar?.title = "Pregled keša"

        cacheId = arguments?.getString("cacheId")

        binding.rvLogs.layoutManager = LinearLayoutManager(requireContext())
        recyclerAdapter = LogRecyclerAdapter(ArrayList(10), requireContext())
        binding.rvLogs.adapter = recyclerAdapter

        Firebase.firestore.document("caches/$cacheId")
            .get()
            .addOnSuccessListener {
                cache = it.toObject<Cache>()
                val c = cache
                if (c != null) {
                    Firebase.database
                        .getReference("users/${c.authorUid}")
                        .get()
                        .addOnSuccessListener { us ->
                            val author = us.getValue(User::class.java)
                            if (author != null) {
                                val photoRef =
                                    Firebase.storage.reference.child("cache/$cacheId.jpg")
                                Glide.with(requireContext())
                                    .load(photoRef)
                                    .into(binding.ivCache)
                                binding.tvCacheType.text = when (c.type) {
                                    CacheType.EASY -> "Težina: Lako"
                                    CacheType.MEDIUM -> "Težina: Srednje"
                                    CacheType.HARD -> "Težina: Teško"
                                }
                                binding.tvCacheDesc.text = c.desc
                                binding.tvCacheAuthor.text =
                                    "${author.firstName} ${author.lastName}"
                                binding.tvCacheDate.text =
                                    getDateTimeInstance().format(c.datePublished)
                                checkLocation()
                                listenToLog()
                            }
                        }
                }
            }
    }

    private fun checkLocation() {
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
        val cache = cache
        val currLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (cache != null && currLocation != null) {
            val cacheLocation = Location("cacheLocation")
            cacheLocation.latitude = cache.lat
            cacheLocation.longitude = cache.lng
            val distance = currLocation.distanceTo(cacheLocation)
            if (distance < 20.0f) {
                setupMenu()
            }
        }
    }

    private fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
            }

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_cache, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_create_log -> {
                        showCreateLog()
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun listenToLog() {
        val cacheId = cacheId
        val cache = cache

        if (cacheId != null && cache != null) {
            Firebase.database
                .getReference("logs/$cacheId")
                .addChildEventListener(
                    object : ChildEventListener {
                        override fun onChildAdded(
                            snapshot: DataSnapshot,
                            previousChildName: String?
                        ) {
                            val log = snapshot.getValue<Log>()
                            val logId = snapshot.key
                            if (log != null && logId != null) {
                                Firebase.database
                                    .getReference("users/${log.authorUid}")
                                    .get()
                                    .addOnSuccessListener {
                                        val author = it.getValue<User>()
                                        if (author != null) {
                                            val logWithAuthor = LogWithAuthor(
                                                logId, log, author
                                            )
                                            recyclerAdapter.add(logWithAuthor)
                                        }
                                    }
                            }
                        }

                        override fun onChildChanged(
                            snapshot: DataSnapshot,
                            previousChildName: String?
                        ) {

                        }

                        override fun onChildRemoved(snapshot: DataSnapshot) {
                            val logId = snapshot.key
                            if (logId != null) {
                                recyclerAdapter.remove(logId)
                            }
                        }

                        override fun onChildMoved(
                            snapshot: DataSnapshot,
                            previousChildName: String?
                        ) {

                        }

                        override fun onCancelled(error: DatabaseError) {

                        }
                    }
                )
        }
    }

    private fun showCreateLog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_log)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(true)
        dialog.setTitle("Upiši log:")

        val btnNewLog = dialog.findViewById<Button>(R.id.btnNewLog)
        val etNewLog = dialog.findViewById<EditText>(R.id.etNewLog)
        val cbNewLog = dialog.findViewById<CheckBox>(R.id.cbNewLog)

        btnNewLog.setOnClickListener {
            val logText = etNewLog.text.toString()
            if (logText.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Molimo unesite tekst unosa",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            val authorUid = Firebase.auth.currentUser?.uid
            if (authorUid != null) {
                val log = Log(
                    authorUid,
                    logText,
                    System.currentTimeMillis(),
                    cbNewLog.isChecked
                )
                val cacheLogsRef = Firebase.database
                    .getReference("logs/$cacheId")
                    .push().ref
                cacheLogsRef.setValue(log)

                val points: Int = if (!log.found) {
                    1
                } else {
                    when (cache!!.type) {
                        CacheType.EASY -> 3
                        CacheType.MEDIUM -> 6
                        CacheType.HARD -> 10
                    }
                }
                val scoreRef = Firebase.database
                    .getReference("users/$authorUid/score")
                scoreRef.get().addOnSuccessListener {
                    val score = it.getValue<Int>()
                    if (score != null) {
                        scoreRef.setValue(points + score)
                        Toast.makeText(
                            requireContext(),
                            "Osvojili ste $points poena!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            }
            dialog.dismiss()
        }

        dialog.show()
    }
}