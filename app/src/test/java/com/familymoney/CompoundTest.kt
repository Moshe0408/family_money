package com.familymoney

import com.familymoney.engine.Compound
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class CompoundTest {

    @Test
    fun `zero rate is just the sum of deposits`() {
        val p = Compound.project(initial = 10_000.0, monthlyContribution = 1_000.0,
            annualRatePercent = 0.0, years = 10)
        assertEquals(130_000.0, p.finalValue, 0.01)
        assertEquals(130_000.0, p.totalContributed, 0.01)
        assertEquals(0.0, p.totalProfit, 0.01)
    }

    @Test
    fun `lump sum with no deposits matches the closed form`() {
        val years = 10
        val rate = 6.0
        val p = Compound.project(10_000.0, 0.0, rate, years)
        val expected = 10_000.0 * (1 + rate / 100 / 12).pow(years * 12)
        assertEquals(expected, p.finalValue, 0.01)
    }

    @Test
    fun `monthly deposits match the annuity formula`() {
        val months = 120
        val r = 0.06 / 12
        val p = Compound.project(0.0, 1_000.0, 6.0, 10)
        // Future value of an ordinary annuity.
        val expected = 1_000.0 * ((1 + r).pow(months) - 1) / r
        assertEquals(expected, p.finalValue, 0.5)
    }

    @Test
    fun `one point per year plus the starting point`() {
        val p = Compound.project(1_000.0, 100.0, 5.0, 10)
        assertEquals(11, p.points.size)
        assertEquals(0, p.points.first().year)
        assertEquals(10, p.points.last().year)
    }

    @Test
    fun `zero years returns the initial amount untouched`() {
        val p = Compound.project(5_000.0, 1_000.0, 6.0, 0)
        assertEquals(5_000.0, p.finalValue, 0.01)
        assertEquals(1, p.points.size)
    }

    @Test
    fun `value grows monotonically with a positive rate`() {
        val p = Compound.project(1_000.0, 500.0, 7.0, 15)
        p.points.zipWithNext { a, b ->
            assertTrue("value must never shrink", b.value >= a.value)
        }
    }

    @Test
    fun `higher rate always yields more`() {
        val low = Compound.project(10_000.0, 1_000.0, 4.0, 20).finalValue
        val mid = Compound.project(10_000.0, 1_000.0, 6.0, 20).finalValue
        val high = Compound.project(10_000.0, 1_000.0, 8.0, 20).finalValue
        assertTrue(low < mid)
        assertTrue(mid < high)
    }

    @Test
    fun `scenarios produce the three requested rates`() {
        val s = Compound.scenarios(10_000.0, 1_000.0, 10)
        assertEquals(setOf(4.0, 6.0, 8.0), s.keys)
        s.forEach { (rate, projection) -> assertEquals(rate, projection.annualRatePercent, 0.01) }
    }

    // ------------------------------------------------------------ child maths

    @Test
    fun `child projection spans current age to target age`() {
        val p = Compound.childProjection(
            currentAge = 5, targetAge = 18,
            existing = 4_000.0, monthly = 500.0, annualRatePercent = 6.0
        )
        assertEquals(13, p.years)
        assertEquals(4_000.0 + 500.0 * 13 * 12, p.totalContributed, 0.01)
        assertTrue("compounding must beat plain saving", p.finalValue > p.totalContributed)
    }

    @Test
    fun `child already at the target age gets no growth`() {
        val p = Compound.childProjection(18, 18, 4_000.0, 500.0, 6.0)
        assertEquals(4_000.0, p.finalValue, 0.01)
    }

    @Test
    fun `age above target does not go negative`() {
        val p = Compound.childProjection(20, 18, 4_000.0, 500.0, 6.0)
        assertEquals(0, p.years)
        assertEquals(4_000.0, p.finalValue, 0.01)
    }

    // ------------------------------------------------------- required monthly

    @Test
    fun `required monthly with no rate is a plain division`() {
        val needed = Compound.requiredMonthly(
            target = 15_000.0, current = 3_000.0, months = 12, annualRatePercent = 0.0
        )
        assertEquals(1_000.0, needed, 0.01)
    }

    @Test
    fun `required monthly reaches the target when applied`() {
        val target = 50_000.0
        val current = 10_000.0
        val months = 60
        val rate = 6.0
        val needed = Compound.requiredMonthly(target, current, months, rate)

        val achieved = Compound.project(current, needed, rate, months / 12).finalValue
        assertEquals(target, achieved, 1.0)
    }

    @Test
    fun `required monthly is zero when the target is already reachable`() {
        val needed = Compound.requiredMonthly(
            target = 10_000.0, current = 20_000.0, months = 12, annualRatePercent = 5.0
        )
        assertEquals(0.0, needed, 0.01)
    }

    @Test
    fun `required monthly with zero months returns the whole gap`() {
        val needed = Compound.requiredMonthly(10_000.0, 4_000.0, 0, 6.0)
        assertEquals(6_000.0, needed, 0.01)
    }
}
