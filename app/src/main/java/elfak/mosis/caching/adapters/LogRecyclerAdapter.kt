package elfak.mosis.caching.adapters;

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
import elfak.mosis.caching.data.LogWithAuthor
import java.text.DateFormat

/**
 * Created by Luka Kocić on 11-Jul-23.
 */
class LogRecyclerAdapter(
    private val logs: ArrayList<LogWithAuthor>,
    private val context: Context
) :
    RecyclerView.Adapter<LogRecyclerAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvLog: TextView
        val tvLogAuthor: TextView
        val tvLogDate: TextView
        val tvLogFound: TextView
        val ivLog: ImageView

        init {
            tvLog = view.findViewById(R.id.tvLog)
            tvLogAuthor = view.findViewById(R.id.tvLogAuthor)
            tvLogDate = view.findViewById(R.id.tvLogDate)
            ivLog = view.findViewById(R.id.ivLog)
            tvLogFound = view.findViewById(R.id.tvLogFound)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.joural_row, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = logs.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val l = logs[position]

        holder.tvLog.text = l.log.text
        holder.tvLogAuthor.text = "${l.author.firstName} ${l.author.lastName}"
        holder.tvLogDate.text = DateFormat
            .getDateTimeInstance()
            .format(java.util.Date(l.log.dateLogged))

        if (l.log.found) {
            holder.tvLogFound.text = "Pronađen"
            holder.tvLogFound.setTextColor(context.getColor(R.color.positive))
        } else {
            holder.tvLogFound.text = "Nije pronađen"
            holder.tvLogFound.setTextColor(context.getColor(R.color.negative))
        }

        val photoRef = Firebase.storage.reference.child(l.author.photoPath)
        Glide.with(context).load(photoRef).into(holder.ivLog)
    }

    fun add(log: LogWithAuthor) {
        logs.add(log)
        logs.sortDescending()
        val i = logs.indexOfFirst { it.logId == log.logId }
        notifyItemInserted(i)
    }

    fun remove(logId: String) {
        val i = logs.indexOfFirst { it.logId == logId }
        logs.removeAt(i)
        notifyItemRemoved(i)
    }
}
