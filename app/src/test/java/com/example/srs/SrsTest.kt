package com.example.srs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SrsTest {

    @Test
    fun testAgainRatingResetsRepsAndRequeues() {
        val state = SrsCardState(ease = 2.5, intervalDays = 10, reps = 3, lapses = 0, due = 100L)
        val result = Srs.review(state, ReviewRating.AGAIN, todayEpochDay = 100L)

        assertEquals(0, result.reps)
        assertEquals(1, result.lapses)
        assertEquals(2.3, result.ease, 0.001)
        assertEquals(101L, result.due)
        assertTrue(result.shouldRequeue)
    }

    @Test
    fun testEaseFloorAt1_3() {
        val state = SrsCardState(ease = 1.35, intervalDays = 2, reps = 1, lapses = 3, due = 100L)
        val result = Srs.review(state, ReviewRating.AGAIN, todayEpochDay = 100L)

        assertEquals(1.3, result.ease, 0.001)
    }

    @Test
    fun testHardRatingReducesEaseAndIncreasesIntervalSlightly() {
        val state = SrsCardState(ease = 2.5, intervalDays = 10, reps = 2, lapses = 0, due = 100L)
        val result = Srs.review(state, ReviewRating.HARD, todayEpochDay = 100L)

        assertEquals(3, result.reps)
        assertEquals(0, result.lapses)
        assertEquals(2.35, result.ease, 0.001)
        assertEquals(12, result.intervalDays) // round(10 * 1.2) = 12
        assertEquals(112L, result.due)
        assertFalse(result.shouldRequeue)
    }

    @Test
    fun testGoodRatingFirstAndSecondRep() {
        val rep0 = SrsCardState(ease = 2.5, intervalDays = 0, reps = 0, lapses = 0, due = 100L)
        val res0 = Srs.review(rep0, ReviewRating.GOOD, todayEpochDay = 100L)
        assertEquals(1, res0.reps)
        assertEquals(1, res0.intervalDays)

        val rep1 = SrsCardState(ease = 2.5, intervalDays = 1, reps = 1, lapses = 0, due = 101L)
        val res1 = Srs.review(rep1, ReviewRating.GOOD, todayEpochDay = 101L)
        assertEquals(2, res1.reps)
        assertEquals(3, res1.intervalDays)

        val rep2 = SrsCardState(ease = 2.5, intervalDays = 3, reps = 2, lapses = 0, due = 104L)
        val res2 = Srs.review(rep2, ReviewRating.GOOD, todayEpochDay = 104L)
        assertEquals(3, res2.reps)
        assertEquals(8, res2.intervalDays) // round(3 * 2.5) = 8
    }

    @Test
    fun testEasyRatingIncreasesEaseAndInterval() {
        val state = SrsCardState(ease = 2.5, intervalDays = 10, reps = 2, lapses = 0, due = 100L)
        val result = Srs.review(state, ReviewRating.EASY, todayEpochDay = 100L)

        assertEquals(3, result.reps)
        assertEquals(2.65, result.ease, 0.001)
        // good interval = round(10 * 2.5) = 25; easy interval = round(25 * 1.3) = 33
        assertEquals(33, result.intervalDays)
        assertEquals(133L, result.due)
        assertFalse(result.shouldRequeue)
    }
}
