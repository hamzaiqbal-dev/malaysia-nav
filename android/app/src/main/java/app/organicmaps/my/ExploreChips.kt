package app.organicmaps.my

import app.organicmaps.R

/**
 * Quick search chip shown on the map: a Malaysian place type in Bahasa Melayu. The chip runs a category
 * search with [query] in the "ms" locale, so its words must exist as ms synonyms in data/categories.txt.
 * [labelRes] is a string resource id.
 */
data class ExploreChip(val emoji: String, val labelRes: Int, val query: String)

object ExploreChips {
    const val QUERY_LOCALE = "ms"

    val ALL: List<ExploreChip> = listOf(
        ExploreChip("⛽", R.string.my_chip_fuel, "stesen minyak"),
        ExploreChip("🕌", R.string.my_chip_surau, "surau"),
        ExploreChip("🍛", R.string.my_chip_mamak, "mamak"),
        ExploreChip("🅿️", R.string.my_chip_parking, "parkir"),
        ExploreChip("🔌", R.string.my_chip_ev, "pengecas ev"),
        ExploreChip("🏥", R.string.my_chip_clinic, "klinik"),
        ExploreChip("🛣️", R.string.my_chip_rest_area, "hentian rehat"),
        ExploreChip("🚻", R.string.my_chip_toilet, "tandas"),
        ExploreChip("🏧", R.string.my_chip_atm, "atm"),
    )

    /** The search engine treats a trailing space as a complete category word. */
    fun searchQuery(chip: ExploreChip): String = chip.query + " "
}
