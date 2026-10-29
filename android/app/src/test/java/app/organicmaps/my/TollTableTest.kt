package app.organicmaps.my

import java.io.File
import org.junit.Assert.assertEquals
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
    fun findsByNameAliasAndContainedName() {
        assertEquals("Penchala", table.find("Plaza Tol Penchala")?.name)
        assertEquals("Penchala", table.find("Bukit Lanjan Toll Plaza")?.name)
        assertEquals("Sunway", table.find("Plaza Tol Sunway Arah Klang")?.name)
    }

    @Test
    fun ambiguousOrUnknownNamesAreNotPriced() {
        // "sungai besi tambahan" contains both PLUS plazas by name: the exact key wins.
        assertEquals("Sungai Besi Tambahan", table.find("Plaza Tol Sungai Besi Tambahan")?.name)
        // Nothing contains "sungai besi" uniquely once a direction suffix is added without an exact key.
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
