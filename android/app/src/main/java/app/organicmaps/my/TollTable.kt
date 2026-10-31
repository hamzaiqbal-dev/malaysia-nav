package app.organicmaps.my

/** One priced toll plaza from data/my_tolls.csv. */
data class TollPlaza(
    val name: String,
    val highway: String,
    val aliases: List<String>,
    /** Price in RM for vehicle classes 1 to 5, in that order. */
    val prices: List<Double>,
    val source: String,
) {
    fun price(vehicleClass: Int): Double = prices[vehicleClass - 1]
}

/** A toll booth the route passes, as named in the map data. */
data class RouteTollBooth(val name: String, val distanceAlongRouteM: Double)

/**
 * Toll total of a route. [unpricedBooths] counts booths the table has no price for, so the total is a
 * lower bound whenever it is positive.
 */
data class RouteTollTotal(val amountRm: Double, val pricedBooths: Int, val unpricedBooths: Int) {
    val boothCount: Int get() = pricedBooths + unpricedBooths
}

/**
 * Prices of data/my_tolls.csv keyed by plaza name, matched against toll-booth names from the map
 * (OpenStreetMap), which come in many spellings: "Plaza Tol Penchala", "Penchala Toll Plaza",
 * "PLAZA TOL PENCHALA (ARAH KL)".
 */
class TollTable(val plazas: List<TollPlaza>) {
    private val byKey: Map<String, TollPlaza> = buildMap {
        for (plaza in plazas) {
            put(normalizeBoothName(plaza.name), plaza)
            for (alias in plaza.aliases) put(normalizeBoothName(alias), plaza)
        }
    }

    /** The plaza a booth name refers to, or null when it is unknown or ambiguous. */
    fun find(boothName: String): TollPlaza? {
        val key = normalizeBoothName(boothName)
        return when {
            key.isEmpty() -> null

            key in byKey -> byKey.getValue(key)

            else -> {
                // "sunway" inside "sunway arah klang": a contained plaza name counts only when it is unique.
                val padded = " $key "
                byKey.filterKeys { padded.contains(" $it ") }.values.distinct().singleOrNull()
            }
        }
    }

    /**
     * Sums the booths of a route for [vehicleClass]. Booths with the same name within
     * [DEDUPE_DISTANCE_M] of each other are one plaza (the map has a node per carriageway or lane).
     */
    fun priceRoute(booths: List<RouteTollBooth>, vehicleClass: Int = CLASS_CAR): RouteTollTotal {
        require(vehicleClass in 1..CLASS_COUNT) { "Unknown vehicle class $vehicleClass" }
        var amount = 0.0
        var priced = 0
        var unpriced = 0
        var lastKey = ""
        var lastDistance = Double.NEGATIVE_INFINITY
        for (booth in booths.sortedBy { it.distanceAlongRouteM }) {
            val key = normalizeBoothName(booth.name)
            val duplicate = key == lastKey && booth.distanceAlongRouteM - lastDistance <= DEDUPE_DISTANCE_M
            lastKey = key
            lastDistance = booth.distanceAlongRouteM
            if (duplicate) continue
            val plaza = find(booth.name)
            if (plaza == null) {
                unpriced++
            } else {
                priced++
                amount += plaza.price(vehicleClass)
            }
        }
        return RouteTollTotal(amount, priced, unpriced)
    }

    companion object {
        const val CLASS_CAR = 1
        const val CLASS_COUNT = 5
        const val DEDUPE_DISTANCE_M = 1500.0

        private val NOISE_WORDS = setOf("plaza", "tol", "toll", "tempat", "pungutan")
        private val PARENTHESES = Regex("\\([^)]*\\)")
        private val NON_WORD = Regex("[^\\p{L}\\p{N}]+")

        /** Lower-case plaza name without "Plaza Tol"/"Toll Plaza", bracketed directions and punctuation. */
        fun normalizeBoothName(name: String): String = name
            .replace(PARENTHESES, " ")
            .lowercase()
            .split(NON_WORD)
            .filter { it.isNotEmpty() && it !in NOISE_WORDS }
            .joinToString(" ")

        /** Parses the CSV text of data/my_tolls.csv; comment and header lines are skipped. */
        fun parse(lines: Sequence<String>): TollTable {
            val plazas = lines
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("plaza,") }
                .map(::parseLine)
                .toList()
            return TollTable(plazas)
        }

        private fun parseLine(line: String): TollPlaza {
            val fields = line.split(",")
            require(fields.size >= COLUMN_COUNT) { "Bad toll line: $line" }
            val aliases = fields[COLUMN_ALIASES].split("|").map { it.trim() }.filter { it.isNotEmpty() }
            val prices = (0 until CLASS_COUNT).map { fields[COLUMN_CLASS1 + it].trim().toDouble() }
            return TollPlaza(fields[0].trim(), fields[1].trim(), aliases, prices, fields[COLUMN_SOURCE].trim())
        }

        private const val COLUMN_ALIASES = 4
        private const val COLUMN_CLASS1 = 5
        private const val COLUMN_SOURCE = 10
        private const val COLUMN_COUNT = 12
    }
}
