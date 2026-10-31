package app.organicmaps.my

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ExploreChipsTest {
    /** Every word of every chip query must be a Bahasa Melayu synonym in data/categories.txt. */
    @Test
    fun chipQueriesAreMalayCategorySynonyms() {
        val synonyms = File("../../data/categories.txt")
            .readLines()
            .filter { it.startsWith("ms:") }
            .flatMap { it.removePrefix("ms:").split("|") }
            .flatMap { it.trimStart('1', '2', '3', '4', '5', '6', '7', '8', '9').lowercase().split(" ") }
            .toSet()
        for (chip in ExploreChips.ALL) {
            for (word in chip.query.split(" ")) {
                assertTrue("'$word' of chip '${chip.query}' is not an ms synonym", word in synonyms)
            }
        }
    }

    @Test
    fun searchQueryEndsWithASpaceSoTheEngineTreatsItAsACategory() {
        for (chip in ExploreChips.ALL) {
            assertTrue(ExploreChips.searchQuery(chip).endsWith(" "))
        }
    }
}
