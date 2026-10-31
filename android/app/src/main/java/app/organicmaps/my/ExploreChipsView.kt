package app.organicmaps.my

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import app.organicmaps.R

/** Horizontal strip of [ExploreChips] over the map; tapping a chip reports its search query. */
class ExploreChipsView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    HorizontalScrollView(context, attrs) {

    fun interface Listener {
        fun onChipClicked(query: String, locale: String)
    }

    var listener: Listener? = null

    init {
        isHorizontalScrollBarEnabled = false
        clipToPadding = false
        val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        val inflater = LayoutInflater.from(context)
        for (chip in ExploreChips.ALL) {
            val view = inflater.inflate(R.layout.my_explore_chip, row, false) as TextView
            val label = context.getString(chip.labelRes)
            view.text = chip.emoji + " " + label
            // Screen readers get the place type only, not the emoji name.
            view.contentDescription = label
            view.setOnClickListener {
                listener?.onChipClicked(ExploreChips.searchQuery(chip), ExploreChips.QUERY_LOCALE)
            }
            row.addView(view)
        }
        addView(row, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL))
        val gutter = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, GUTTER_DP, resources.displayMetrics).toInt()
        setPaddingRelative(gutter, 0, gutter, 0)
    }

    private companion object {
        const val GUTTER_DP = 8f
    }
}
