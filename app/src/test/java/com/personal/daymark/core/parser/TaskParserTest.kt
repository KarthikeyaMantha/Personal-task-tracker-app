package com.personal.daymark.core.parser

import com.personal.daymark.core.common.DateFormatter
import com.personal.daymark.data.local.entity.CategoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class TaskParserTest {

    private val sampleCategories = listOf(
        CategoryEntity(id = 1L, name = "Personal", colorHex = "#4CAF50"),
        CategoryEntity(id = 2L, name = "Work", colorHex = "#2196F3"),
        CategoryEntity(id = 3L, name = "Fitness", colorHex = "#FF9800")
    )

    @Test
    fun parse_fullString_extractsAllMetadataAndCleansTitle() {
        val input = "Buy milk #Personal !high tomorrow at 6pm"
        val result = TaskParser.parse(input, sampleCategories)

        assertEquals("Buy milk", result.cleanTitle)
        assertNotNull(result.matchedCategory)
        assertEquals("Personal", result.matchedCategory?.name)
        assertEquals(1L, result.matchedCategory?.id)
        assertEquals(3, result.priority) // 3 = High
        assertNotNull(result.dueDate)
        assertEquals(18 * 60, result.dueTimeMinutes) // 6:00 PM = 1080 minutes
        assertTrue(result.hasDetections)

        val chips = result.getExplanationChips()
        assertTrue(chips.any { it.contains("Personal", ignoreCase = true) })
        assertTrue(chips.any { it.contains("High", ignoreCase = true) })
        assertTrue(chips.any { it.contains("Tomorrow", ignoreCase = true) })
        assertTrue(chips.any { it.contains("6:00 PM", ignoreCase = true) })
    }

    @Test
    fun parse_categoryCaseInsensitive_matchesCorrectly() {
        val input = "Draft quarterly roadmap #work"
        val result = TaskParser.parse(input, sampleCategories)

        assertEquals("Draft quarterly roadmap", result.cleanTitle)
        assertEquals("Work", result.matchedCategory?.name)
        assertEquals(2L, result.matchedCategory?.id)
    }

    @Test
    fun parse_priorityVariations_mapsCorrectly() {
        val urgentResult = TaskParser.parse("Fix production crash !urgent")
        assertEquals(4, urgentResult.priority)
        assertEquals("Fix production crash", urgentResult.cleanTitle)

        val highResult = TaskParser.parse("Prepare client demo !high")
        assertEquals(3, highResult.priority)

        val medResult = TaskParser.parse("Reply to non-critical email !med")
        assertEquals(2, medResult.priority)

        val lowResult = TaskParser.parse("Water house plants !low")
        assertEquals(1, lowResult.priority)
    }

    @Test
    fun parse_time24Hour_extractsMinutes() {
        val input = "Team standup at 17:30"
        val result = TaskParser.parse(input)

        assertEquals("Team standup", result.cleanTitle)
        assertEquals(17 * 60 + 30, result.dueTimeMinutes)
    }

    @Test
    fun parse_time12HourMorningAndNoon() {
        val morning = TaskParser.parse("Morning sync at 9:15am")
        assertEquals(9 * 60 + 15, morning.dueTimeMinutes)
        assertEquals("Morning sync", morning.cleanTitle)

        val noon = TaskParser.parse("Lunch with team at 12pm")
        assertEquals(12 * 60, noon.dueTimeMinutes)
        assertEquals("Lunch with team", noon.cleanTitle)

        val midnight = TaskParser.parse("System backup at 12:00am")
        assertEquals(0, midnight.dueTimeMinutes)
        assertEquals("System backup", midnight.cleanTitle)
    }

    @Test
    fun parse_relativeDates_todayTomorrowNextWeek() {
        val startOfToday = DateFormatter.getStartOfTodayMillis()

        val todayResult = TaskParser.parse("Pay electric bill today")
        assertEquals("Pay electric bill", todayResult.cleanTitle)
        assertEquals(startOfToday, todayResult.dueDate)

        val tomorrowResult = TaskParser.parse("Schedule dentist appointment tomorrow")
        assertEquals("Schedule dentist appointment", tomorrowResult.cleanTitle)
        assertEquals(startOfToday + 86400000L, tomorrowResult.dueDate)

        val nextWeekResult = TaskParser.parse("Submit tax return next week")
        assertEquals("Submit tax return", nextWeekResult.cleanTitle)
        assertEquals(startOfToday + 7 * 86400000L, nextWeekResult.dueDate)
    }

    @Test
    fun parse_weekday_schedulesUpcomingOccurrence() {
        val result = TaskParser.parse("Call accountant on friday")
        assertEquals("Call accountant", result.cleanTitle)
        assertNotNull(result.dueDate)

        val cal = Calendar.getInstance().apply { timeInMillis = result.dueDate!! }
        assertEquals(Calendar.FRIDAY, cal.get(Calendar.DAY_OF_WEEK))
    }

    @Test
    fun parse_noFalsePositives_onRegularWords() {
        // Words like "money" shouldn't trigger "mon", "blow" shouldn't trigger "low", etc.
        val input = "Check money in bank and blow candles"
        val result = TaskParser.parse(input)

        assertEquals("Check money in bank and blow candles", result.cleanTitle)
        assertNull(result.dueDate)
        assertNull(result.priority)
        assertNull(result.matchedCategory)
        assertFalse(result.hasDetections)
    }

    @Test
    fun parse_blankInput_returnsEmptyResult() {
        val result = TaskParser.parse("   ")
        assertEquals("", result.cleanTitle)
        assertFalse(result.hasDetections)
    }
}
