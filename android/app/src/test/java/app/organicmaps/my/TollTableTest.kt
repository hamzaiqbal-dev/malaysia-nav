package app.organicmaps.my

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TollTableTest {
    private val csv = """
        # comment
        plaza,highway,road_ref,concession,aliases,class1,class2,class3,class4,class5,source,checked
        Penchala,LDP,E11,Litrak,Bukit Lanjan,2.10,4.20,6.30,1.10,1.60,https://example.org/ldp,2026-05-22
        Sunway,KESAS,E5,KESAS,,2.00,3.00,4.00,1.00,1.50,https://example.org/kesas,2026-05-22
        Sungai Besi,PLUS,E2,PLUS,,1.00,2.00,3.00,0.50,0.80,https://example.org/plus,2026-05-22
        Sungai Besi Tambahan,PLUS,E2,PLUS,,0.50,1.00,1.50,0.25,0.40,https://example.org/plus,2026-05-22
    """.trimIndent()
    private val table = TollTable.parse(csv.lineSequence())

    @Test
    fun parsesPlazasAndPrices() {
        assertEquals(4, table.plazas.size)
        val penchala = table.plazas[0]
        assertEquals("Penchala", penchala.name)
        assertEquals("LDP", penchala.highway)
        assertEquals(listOf("Bukit Lanjan"), penchala.aliases)
        assertEquals(2.10, penchala.price(TollTable.CLASS_CAR), 0.0)
        assertEquals(1.60, penchala.price(5), 0.0)
        assertEquals("https://example.org/ldp", penchala.source)
    }

    @Test
    fun normalizesBoothNames() {
        assertEquals("penchala", TollTable.normalizeBoothName("Plaza Tol Penchala"))
        assertEquals("penchala", TollTable.normalizeBoothName("PENCHALA TOLL PLAZA (ARAH KL)"))
        assertEquals("sungai besi arah seremban", TollTable.normalizeBoothName("Plaza Tol Sungai Besi – Arah Seremban"))
        assertEquals("", TollTable.normalizeBoothName("Toll Plaza"))
    }

    @Test
    fun findsByNameAliasAndDirection() {
        assertEquals("Penchala", table.find("Plaza Tol Penchala")?.name)
        assertEquals("Penchala", table.find("Bukit Lanjan Toll Plaza")?.name)
        assertEquals("Sunway", table.find("Plaza Tol Sunway Arah Klang")?.name)
        assertEquals("Sunway", table.find("Sunway Toll Plaza to Klang")?.name)
    }

    @Test
    fun otherPlazasWithAPricedWordInTheirNameAreNotPriced() {
        val batu = TollTable.parse(sequenceOf("Batu,DUKE,E33,Ekovest,,2.50,3.80,5.00,1.30,1.30,https://example.org,x"))
        assertEquals("Batu", batu.find("Plaza Tol Batu")?.name)
        assertNull(batu.find("Plaza Tol Batu Pahat"))
        assertNull(batu.find("Plaza Tol Batu Kawan Arah Pulau Pinang"))
        assertNull(table.find("Plaza Tol Kota Sunway"))
    }

    @Test
    fun ambiguousOrUnknownNamesAreNotPriced() {
        // Both PLUS plazas start with "sungai besi": the whole name decides.
        assertEquals("Sungai Besi Tambahan", table.find("Plaza Tol Sungai Besi Tambahan")?.name)
        assertNull(table.find("Plaza Tol Sungai Besi Tambahan Utara"))
        assertNull(table.find("Plaza Tol Jalan Duta"))
        assertNull(table.find("Toll Plaza"))
    }

    @Test
    fun priceRouteSumsAndDeduplicatesCarriagewayBooths() {
        val booths = listOf(
            RouteTollBooth("Plaza Tol Penchala", 1000.0),
            RouteTollBooth("Plaza Tol Penchala", 1030.0),
            RouteTollBooth("Plaza Tol Sunway (Arah KL)", 9000.0),
            RouteTollBooth("Plaza Tol Jalan Duta", 15000.0),
            RouteTollBooth("Plaza Tol Penchala", 40000.0),
        )
        val total = table.priceRoute(booths)
        assertEquals(2.10 + 2.00 + 2.10, total.amountRm, 1e-9)
        assertEquals(3, total.pricedBooths)
        assertEquals(1, total.unpricedBooths)
        assertEquals(4, total.boothCount)
    }

    @Test
    fun unnamedBoothsCountOnceAsUnpriced() {
        val booths = listOf(
            RouteTollBooth("", 1000.0),
            RouteTollBooth("", 1020.0),
            RouteTollBooth("Plaza Tol Sunway", 9000.0),
            RouteTollBooth("", 20000.0),
        )
        val total = table.priceRoute(booths)
        assertEquals(1, total.pricedBooths)
        assertEquals(2, total.unpricedBooths)
    }

    @Test
    fun spellingsOfOnePlazaCountOnce() {
        val booths = listOf(
            RouteTollBooth("Plaza Tol Sunway", 1000.0),
            RouteTollBooth("Plaza Tol Sunway Arah Klang", 1020.0),
            RouteTollBooth("Penchala Toll Plaza", 9000.0),
            RouteTollBooth("Plaza Tol Bukit Lanjan", 9030.0),
        )
        val total = table.priceRoute(booths)
        assertEquals(2.00 + 2.10, total.amountRm, 1e-9)
        assertEquals(2, total.pricedBooths)
        assertEquals(0, total.unpricedBooths)
    }

    @Test
    fun unnamedLaneOfANamedPlazaAddsNothing() {
        val after = listOf(RouteTollBooth("Plaza Tol Sunway", 1000.0), RouteTollBooth("", 1020.0))
        val before = listOf(RouteTollBooth("", 1000.0), RouteTollBooth("Plaza Tol Sunway", 1020.0))
        val between = listOf(
            RouteTollBooth("Plaza Tol Sunway", 1000.0),
            RouteTollBooth("", 1010.0),
            RouteTollBooth("Plaza Tol Sunway", 1020.0),
        )
        for (booths in listOf(after, before, between)) {
            val total = table.priceRoute(booths)
            assertEquals(2.00, total.amountRm, 1e-9)
            assertEquals(1, total.boothCount)
        }
    }

    @Test
    fun unnamedBoothFarFromAPlazaStaysUnpriced() {
        val total = table.priceRoute(listOf(RouteTollBooth("Plaza Tol Sunway", 1000.0), RouteTollBooth("", 2000.0)))
        assertEquals(1, total.pricedBooths)
        assertEquals(1, total.unpricedBooths)
    }

    @Test
    fun differentPlazasCloseTogetherBothCount() {
        val booths = listOf(RouteTollBooth("Plaza Tol Sunway", 1000.0), RouteTollBooth("Plaza Tol Penchala", 1100.0))
        assertEquals(2.00 + 2.10, table.priceRoute(booths).amountRm, 1e-9)
    }

    @Test
    fun hasPriceOnlyWithAPricedBooth() {
        assertFalse(table.priceRoute(listOf(RouteTollBooth("Plaza Tol Ayer Keroh", 0.0))).hasPrice)
        assertTrue(table.priceRoute(listOf(RouteTollBooth("Plaza Tol Sunway", 0.0))).hasPrice)
    }

    @Test
    fun abbreviatedNamesMatch() {
        assertEquals(TollTable.normalizeBoothName("Sungai Ramal"), TollTable.normalizeBoothName("Sg. Ramal"))
        assertEquals("bukit kajang", TollTable.normalizeBoothName("Plaza Tol Bkt Kajang"))
        assertEquals("sungai besi tambahan", TollTable.normalizeBoothName("PLAZA TOL SG BESI TAMBAHAN (ARAH KL)"))
    }

    @Test
    fun priceRouteUsesVehicleClass() {
        val total = table.priceRoute(listOf(RouteTollBooth("Plaza Tol Sunway", 0.0)), vehicleClass = 3)
        assertEquals(4.00, total.amountRm, 0.0)
    }

    @Test
    fun shippedTableParsesAndHasFivePricesPerPlaza() {
        val file = File("../../data/my_tolls.csv")
        val shipped = TollTable.parse(file.readLines().asSequence())
        assertTrue("Only ${shipped.plazas.size} plazas", shipped.plazas.size >= 50)
        for (plaza in shipped.plazas) {
            assertEquals(TollTable.CLASS_COUNT, plaza.prices.size)
            assertTrue("${plaza.name} has no source", plaza.source.startsWith("https://"))
        }
        assertEquals("Penchala", shipped.find("Plaza Tol Penchala")?.name)
    }
}
