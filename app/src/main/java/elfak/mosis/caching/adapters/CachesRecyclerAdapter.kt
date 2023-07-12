package elfak.mosis.caching.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import elfak.mosis.caching.R
import elfak.mosis.caching.data.CacheType
import elfak.mosis.caching.data.CacheWithId
import kotlin.math.roundToInt

/**
 * Created by Luka Kocić on 10-Jul-23.
 */
class CachesRecyclerAdapter(
    private val dataSet: ArrayList<CacheWithId>,
    private val context: Context
) :
    RecyclerView.Adapter<CachesRecyclerAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCLDistance: TextView
        val tvCLType: TextView
        val ivCL: ImageView
        val clCL: ConstraintLayout

        init {
            tvCLDistance = view.findViewById(R.id.tvCLDistance)
            tvCLType = view.findViewById(R.id.tvCLType)
            ivCL = view.findViewById(R.id.ivCL)
            clCL = view.findViewById(R.id.cvCL)
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.cache_list_row, viewGroup, false)

        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val cache = dataSet[position]

        viewHolder.tvCLDistance.text = getDistanceString(cache.distance)

        when (cache.cache.type) {
            CacheType.EASY -> {
                viewHolder.tvCLType.text = "Lako"
                viewHolder.tvCLType.setTextColor(context.getColor(R.color.positive))
            }

            CacheType.MEDIUM -> {
                viewHolder.tvCLType.text = "Srednje"
                viewHolder.tvCLType.setTextColor(context.getColor(R.color.medium))
            }

            CacheType.HARD -> {
                viewHolder.tvCLType.text = "Teško"
                viewHolder.tvCLType.setTextColor(context.getColor(R.color.negative))
            }
        }

        val photoRef = Firebase.storage.reference.child("cache/${cache.id}.jpg")
        Glide.with(context).load(photoRef).into(viewHolder.ivCL)

        viewHolder.clCL.setOnClickListener {
            val bundle = bundleOf("cacheId" to cache.id)
            it.findNavController()
                .navigate(R.id.action_cacheListFragment_to_cacheFragment, bundle)
        }
    }

    override fun getItemCount() = dataSet.size

    fun add(c: CacheWithId) {
        dataSet.add(c)
        dataSet.sort()
        val i = dataSet.indexOfFirst { it.id == c.id }
        notifyItemInserted(i)
    }

    fun remove(id: String) {
        val i = dataSet.indexOfFirst { it.id == id }
        dataSet.removeAt(i)
        notifyItemRemoved(i)
    }

    private fun getDistanceString(distance: Float): String {
        return if (distance < 1000) {
            String.format("%d m", distance.roundToInt())
        } else {
            String.format("%.3f km", distance / 1000.0f)
        }
    }
}