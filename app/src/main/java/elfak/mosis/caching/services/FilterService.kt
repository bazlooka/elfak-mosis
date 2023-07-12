package elfak.mosis.caching.services

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.datepicker.MaterialDatePicker
import elfak.mosis.caching.R
import elfak.mosis.caching.data.Cache
import elfak.mosis.caching.data.CacheType
import elfak.mosis.caching.data.Filter
import elfak.mosis.caching.model.FilterViewModel
import java.text.DateFormat
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Created by Luka Kocić on 12-Jul-23.
 */
class FilterService {
    companion object {
        fun filterCache(filter: Filter, cache: Cache): Boolean {
            if (cache.type !in filter.selectedTypes) {
                return false
            }
            if (filter.startTime != null && filter.endTime != null
                && (cache.datePublished!! < filter.startTime || cache.datePublished!! > filter.endTime)
            ) {
                return false
            }
            if (!cache.desc.contains(filter.desc)) {
                return false
            }
            return true
        }


        fun showFilter(
            filterViewModel: FilterViewModel,
            fragment: Fragment,
            callback: FilterServiceCallback
        ) {
            val dialog = Dialog(fragment.requireContext())
            dialog.setContentView(R.layout.dialog_filter)
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            dialog.setCancelable(true)

            val filter = filterViewModel.filter.value

            val label = dialog.findViewById<TextView>(R.id.tvFilterRadius)
            val sbRadius = dialog.findViewById<SeekBar>(R.id.sbRadius)
            val cbFilterEasy = dialog.findViewById<CheckBox>(R.id.cbFilterEasy)
            val cbFilterMedium = dialog.findViewById<CheckBox>(R.id.cbFilterMedium)
            val cbFilterHard = dialog.findViewById<CheckBox>(R.id.cbFilterHard)
            val etFilterDesc = dialog.findViewById<EditText>(R.id.etFilterDesc)
            val btnFilterDate = dialog.findViewById<Button>(R.id.btnFilterDate)
            val btnApplyFilter = dialog.findViewById<Button>(R.id.btnAppyFilter)
            val tvFilterDate = dialog.findViewById<TextView>(R.id.tvFilterDate)

            var start: Long? = null
            var end: Long? = null

            val progress = filter?.radius ?: 100
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
            cbFilterEasy.isChecked = filter?.selectedTypes?.contains(CacheType.EASY) ?: true
            cbFilterMedium.isChecked = filter?.selectedTypes?.contains(CacheType.MEDIUM) ?: true
            cbFilterHard.isChecked = filter?.selectedTypes?.contains(CacheType.HARD) ?: true
            etFilterDesc.setText(filter?.desc ?: "")
            if (filter?.startTime != null && filter.endTime != null) {
                tvFilterDate.text = getDateString(filter.startTime, filter.endTime)
            }

            btnFilterDate
                .setOnClickListener {
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
                    dateRangePicker.addOnPositiveButtonClickListener {
                        start = it.first
                        end = it.second
                        tvFilterDate.text = getDateString(it.first, it.second)
                    }
                    dateRangePicker
                        .show(fragment.parentFragmentManager, "tag")
                }

            btnApplyFilter
                .setOnClickListener {
                    val newProgress = sbRadius.progress
                    val dProgress = newProgress / 100.0
                    val radius =
                        if (newProgress == 100)
                            8587.0
                        else
                            dProgress.pow(5) * 8587.0


                    val cacheTypes = arrayListOf<CacheType>()

                    if (cbFilterEasy.isChecked)
                        cacheTypes.add(CacheType.EASY)
                    if (cbFilterMedium.isChecked)
                        cacheTypes.add(CacheType.MEDIUM)
                    if (cbFilterHard.isChecked)
                        cacheTypes.add(CacheType.HARD)

                    val newFilter = Filter(
                        newProgress,
                        cacheTypes.toTypedArray(),
                        etFilterDesc.text.toString(),
                        start,
                        end
                    )
                    filterViewModel.setFilter(newFilter)
                    callback.success(newFilter, radius)
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

        private fun getDateString(start: Long, end: Long): String {
            val startStr = DateFormat
                .getDateInstance()
                .format(java.util.Date(start))
            val endStr = DateFormat
                .getDateInstance()
                .format(java.util.Date(end))
            return "$startStr - $endStr"
        }
    }
}