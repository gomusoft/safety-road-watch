package watch.saferoad

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParseTest {
    @Test
    fun plainPlateProvinceViolations() {
        val r = parse("ทะเบียน 8กค3692 นครราชสีมา ล้อล้น ควันดำ บันทึกเลย")
        assertEquals("8กค 3692", r.plate)
        assertEquals("นครราชสีมา", r.province)
        assertEquals(listOf("ล้อล้นตัวถัง", "ควันดำ"), r.violations)
    }

    @Test
    fun spelledLettersAndAlias() {
        val r = parse("กอไก่ คอควาย 1234 กทม ฝ่าไฟแดง")
        assertEquals("กค 1234", r.plate)
        assertEquals("กรุงเทพมหานคร", r.province)
        assertEquals(listOf("ฝ่าไฟแดง"), r.violations)
    }

    @Test
    fun thaiDigitsAndSplitName() {
        val r = parse("ทะเบียน ๑ กอ ไก่ ขอไข่ ๑๒๓๔ โคราช")
        assertEquals("1กข 1234", r.plate)
        assertEquals("นครราชสีมา", r.province)
    }

    @Test
    fun nothingParsed() {
        val r = parse("บันทึกเลย")
        assertNull(r.plate)
        assertNull(r.province)
        assertEquals(emptyList<String>(), r.violations)
    }
}
