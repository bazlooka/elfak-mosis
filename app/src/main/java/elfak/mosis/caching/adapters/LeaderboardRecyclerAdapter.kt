package elfak.mosis.caching.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import elfak.mosis.caching.R
import elfak.mosis.caching.data.User

/**
 * Created by Luka Kocić on 10-Jul-23.
 */
class LeaderboardRecyclerAdapter(
    private val dataSet: ArrayList<User>,
    private val context: Context
) :
    RecyclerView.Adapter<LeaderboardRecyclerAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvLbRank: TextView
        val tvLbName: TextView
        val tvLbScore: TextView
        val ivLb: ImageView

        init {
            tvLbRank = view.findViewById(R.id.tvLbRank)
            tvLbName = view.findViewById(R.id.tvLbName)
            tvLbScore = view.findViewById(R.id.tvLbScore)
            ivLb = view.findViewById(R.id.ivLb)
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.leaderboard_row, viewGroup, false)

        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val user = dataSet[position]

        viewHolder.tvLbRank.text = (1 + position).toString()
        viewHolder.tvLbName.text = "${user.firstName} ${user.lastName}"
        viewHolder.tvLbScore.text = user.score.toString()

        val photoRef = Firebase.storage.reference.child(user.photoPath)
        Glide.with(context).load(photoRef).into(viewHolder.ivLb)
    }

    override fun getItemCount() = dataSet.size

    fun add(user: User) {
        dataSet.add(user)
        dataSet.sortDescending()
        val i = dataSet.indexOfFirst { it.uid == user.uid }
        notifyItemInserted(i)
    }

    fun change(user: User) {
        val i = dataSet.indexOfFirst { it.uid == user.uid }
        dataSet[i] = user
        dataSet.sortDescending()
        val newI = dataSet.indexOfFirst { it.uid == user.uid }
        if (i != newI) {
            notifyItemMoved(i, newI)
        }
        notifyItemChanged(newI)
    }

    fun remove(user: User) {
        val i = dataSet.indexOfFirst { it.uid == user.uid }
        dataSet.removeAt(i)
        notifyItemRemoved(i)
    }
}