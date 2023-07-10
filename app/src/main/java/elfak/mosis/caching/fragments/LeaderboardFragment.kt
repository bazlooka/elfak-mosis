package elfak.mosis.caching.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import elfak.mosis.caching.R
import elfak.mosis.caching.adapters.LeaderboardRecyclerAdapter
import elfak.mosis.caching.data.User

class LeaderboardFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerAdapter: LeaderboardRecyclerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_leaderboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.title = "Rang lista"
        recyclerView = view.findViewById(R.id.rvLeaderboard)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerAdapter = LeaderboardRecyclerAdapter(ArrayList(10), requireContext())
        recyclerView.adapter = recyclerAdapter


        Firebase.database.getReference("users")
            .orderByChild("score")
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val user = snapshot.getValue<User>()
                    if (user != null) {
                        recyclerAdapter.add(user)
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    val user = snapshot.getValue<User>()
                    if (user != null) {
                        recyclerAdapter.change(user)
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val user = snapshot.getValue<User>()
                    if (user != null) {
                        recyclerAdapter.remove(user)
                    }
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
    }
}