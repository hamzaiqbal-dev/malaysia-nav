package app.organicmaps.my

/** One priced toll plaza from data/my_tolls.csv. */
data class TollPlaza(
    val name: String,
    val highway: String,
    /** Expressway number, e.g. "E23". */
    val roadRef: String,
    val aliases: List<String>,
    /** Price in RM for vehicle classes 1 to 5, in that order. */
    val prices: List<Double>,
    val source: String,
) {
    fun price(vehicleClass: Int): Double = prices[vehicleClass - 1]

    /**
     * False when [boothRoadRefs] put the booth on another expressway: NKVE has its own Damansara and Bukit Raja
     * plazas. Slip roads often have no number, so a booth with no expressway number keeps the plaza.
     */
    fun servesBoothOn(boothRoadRefs: List<String>): Boolean {
        val expressways = boothRoadRefs.filter { TollTable.EXPRESSWAY_REF.matches(it) }
        return expressways.isEmpty() || roadRef in expressways
    }
}

/** A toll booth the route passes, as named in the map data. */
data class RouteTollBooth(
    val name: String,
    val distanceAlongRouteM: Double,
    /** Numbers of the roads at the booth, see [TollTable.parseRoadRefs]. */
    val roadRefs: List<String> = emptyList(),
)

/**
 * Toll total of a route. [unpricedBooths] counts booths the table has no price for, so the total is a
 * lower bound whenever it is positive.
 */
data class RouteTollTotal(val amountRm: Double, val pricedBooths: Int, val unpricedBooths: Int) {
    val boothCount: Int get() = pricedBooths + unpricedBooths

    /** False when no booth has a price (closed-system highways): [amountRm] says nothing about the trip. */
    val hasPrice: Boolean get() = pricedBooths > 0
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

    /**
     * The plaza a booth name refers to, or null when it is unknown. Only a direction may follow the plaza
     * name: "Batu Pahat" is not the plaza "Batu", and a wrong price is worse than none.
     */
    fun find(boothName: String): TollPlaza? {
        val key = normalizeBoothName(boothName)
        return byKey[key] ?: byKey[withoutDirection(key)]
    }

    /**
     * Sums the plazas of a route for [vehicleClass]. The map has a node per carriageway or lane, so booths of the
     * same plaza within [DEDUPE_DISTANCE_M] count once, whatever spelling, alias or direction each node carries,
     * and an unnamed node within [SAME_PLAZA_DISTANCE_M] of a plaza is one of its lanes.
     */
    fun priceRoute(booths: List<RouteTollBooth>, vehicleClass: Int = CLASS_CAR): RouteTollTotal {
        require(vehicleClass in 1..CLASS_COUNT) { "Unknown vehicle class $vehicleClass" }
        val stops = ArrayList<TollStop>()
        for (booth in booths.sortedBy { it.distanceAlongRouteM }) {
            val plaza = find(booth.name)
            val key = normalizeBoothName(plaza?.name ?: booth.name)
            addStop(stops, TollStop(key, plaza?.takeIf { it.servesBoothOn(booth.roadRefs) }, booth.distanceAlongRouteM))
        }
        val plazas = stops.mapNotNull { it.plaza }
        return RouteTollTotal(plazas.sumOf { it.price(vehicleClass) }, plazas.size, stops.size - plazas.size)
    }

    private fun addStop(stops: MutableList<TollStop>, stop: TollStop) {
        val samePlaza = stops.lastOrNull { it.key == stop.key && stop.distanceM - it.distanceM <= DEDUPE_DISTANCE_M }
        val beside = stops.lastOrNull()?.takeIf { stop.distanceM - it.distanceM <= SAME_PLAZA_DISTANCE_M }
        when {
            samePlaza != null -> samePlaza.absorb(stop)
            beside != null && stop.key.isEmpty() -> beside.distanceM = stop.distanceM
            beside != null && beside.key.isEmpty() -> stops[stops.lastIndex] = stop
            else -> stops.add(stop)
        }
    }

    /** A plaza the route crosses; [key] is empty for a booth with no usable name. */
    private class TollStop(val key: String, var plaza: TollPlaza?, var distanceM: Double) {
        fun absorb(lane: TollStop) {
            distanceM = lane.distanceM
            // One lane on another expressway settles it: a wrong price is worse than none.
            if (lane.plaza == null) plaza = null
        }
    }

    companion object {
        const val CLASS_CAR = 1
        const val CLASS_COUNT = 5
        const val DEDUPE_DISTANCE_M = 1500.0
        const val SAME_PLAZA_DISTANCE_M = 200.0

        private val NOISE_WORDS = setOf("plaza", "tol", "toll", "tempat", "pungutan")

        /** Signposts abbreviate these, so OpenStreetMap has both spellings. "Bt" is left out: Batu or Bukit. */
        private val ABBREVIATIONS = mapOf("sg" to "sungai", "bkt" to "bukit", "kg" to "kampung", "jln" to "jalan")

        /** A direction starts here: "sunway arah klang", "gombak to kuantan". */
        private val DIRECTION_WORDS = setOf("arah", "hala", "ke", "to", "towards")
        private val PARENTHESES = Regex("\\([^)]*\\)")
        private val NON_WORD = Regex("[^\\p{L}\\p{N}]+")
        private val ROAD_REF_SEPARATOR = Regex("[;/,]")
        val EXPRESSWAY_REF = Regex("E\\d+")

        /** Lower-case plaza name with short forms expanded, without "Plaza Tol", brackets or punctuation. */
        fun normalizeBoothName(name: String): String = name
            .replace(PARENTHESES, " ")
            .lowercase()
            .split(NON_WORD)
            .filter { it.isNotEmpty() && it !in NOISE_WORDS }
            .joinToString(" ") { ABBREVIATIONS[it] ?: it }

        /** Splits the OpenStreetMap road numbers of a booth, "E 23;AH2", into ["E23", "AH2"]. */
        fun parseRoadRefs(refs: String): List<String> =
            refs.split(ROAD_REF_SEPARATOR).map { it.replace(" ", "").uppercase() }.filter { it.isNotEmpty() }

        private fun withoutDirection(key: String): String =
            key.split(" ").takeWhile { it !in DIRECTION_WORDS }.joinToString(" ")

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
            return TollPlaza(
                fields[0].trim(),
                fields[1].trim(),
                fields[COLUMN_ROAD_REF].trim(),
                aliases,
                prices,
                fields[COLUMN_SOURCE].trim(),
            )
        }

        private const val COLUMN_ROAD_REF = 2
        private const val COLUMN_ALIASES = 4
        private const val COLUMN_CLASS1 = 5
        private const val COLUMN_SOURCE = 10
        private const val COLUMN_COUNT = 12
    }
}
