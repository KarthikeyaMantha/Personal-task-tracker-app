package com.personal.daymark.core.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceActionParserTest {

    @Test
    fun parse_waterLogging_variants() {
        val action1 = VoiceActionParser.parse("I drank 2 glasses of water")
        assertTrue(action1 is VoiceAction.LogHabitProgress)
        assertEquals("water", (action1 as VoiceAction.LogHabitProgress).habitQuery)
        assertEquals(2, action1.amount)

        val action2 = VoiceActionParser.parse("3 glasses of water")
        assertTrue(action2 is VoiceAction.LogHabitProgress)
        assertEquals("water", (action2 as VoiceAction.LogHabitProgress).habitQuery)
        assertEquals(3, action2.amount)

        val action3 = VoiceActionParser.parse("add water")
        assertTrue(action3 is VoiceAction.LogHabitProgress)
        assertEquals(1, (action3 as VoiceAction.LogHabitProgress).amount)
    }

    @Test
    fun parse_sleepLogging() {
        val action = VoiceActionParser.parse("logged 8 hours of sleep")
        assertTrue(action is VoiceAction.LogHabitProgress)
        val habitAction = action as VoiceAction.LogHabitProgress
        assertEquals("sleep", habitAction.habitQuery)
        assertEquals(8, habitAction.amount)
        assertEquals("hours", habitAction.unit)
    }

    @Test
    fun parse_completeTask() {
        val action1 = VoiceActionParser.parse("complete task buy groceries")
        assertTrue(action1 is VoiceAction.CompleteTask)
        assertEquals("buy groceries", (action1 as VoiceAction.CompleteTask).taskQuery)

        val action2 = VoiceActionParser.parse("mark task finish presentation as done")
        assertTrue(action2 is VoiceAction.CompleteTask)
        assertEquals("finish presentation", (action2 as VoiceAction.CompleteTask).taskQuery)
    }

    @Test
    fun parse_createHabit() {
        val action1 = VoiceActionParser.parse("new habit read 15 pages every day")
        assertTrue(action1 is VoiceAction.CreateHabit)
        val habit1 = action1 as VoiceAction.CreateHabit
        assertEquals("Read 15 pages", habit1.name)
        assertEquals("DAILY", habit1.frequency)
        assertEquals(127, habit1.targetWeekdays)

        val action2 = VoiceActionParser.parse("create habit gym on weekdays")
        assertTrue(action2 is VoiceAction.CreateHabit)
        val habit2 = action2 as VoiceAction.CreateHabit
        assertEquals("Gym", habit2.name)
        assertEquals("SPECIFIC_DAYS", habit2.frequency)
        assertEquals(31, habit2.targetWeekdays)
    }

    @Test
    fun parse_toggleHabit() {
        val action1 = VoiceActionParser.parse("check off morning meditation")
        assertTrue(action1 is VoiceAction.ToggleHabit)
        assertEquals("morning meditation", (action1 as VoiceAction.ToggleHabit).habitQuery)

        val action2 = VoiceActionParser.parse("did meditation")
        assertTrue(action2 is VoiceAction.ToggleHabit)
        assertEquals("meditation", (action2 as VoiceAction.ToggleHabit).habitQuery)
    }

    @Test
    fun parse_taskCreation_withNaturalLanguageDates() {
        val action1 = VoiceActionParser.parse("remind me to buy groceries tomorrow at 5pm")
        assertTrue(action1 is VoiceAction.CreateTask)
        val task1 = action1 as VoiceAction.CreateTask
        assertEquals("Buy groceries", task1.title)
        assertNotNull(task1.dueDate)
        assertEquals(1020, task1.dueTimeMinutes) // 17:00 -> 17*60 = 1020

        val action2 = VoiceActionParser.parse("add task finish slides !urgent")
        assertTrue(action2 is VoiceAction.CreateTask)
        val task2 = action2 as VoiceAction.CreateTask
        assertEquals("Finish slides", task2.title)
        assertEquals(4, task2.priority) // !urgent -> 4

        val action3 = VoiceActionParser.parse("call mom at 6 pm")
        assertTrue(action3 is VoiceAction.CreateTask)
        val task3 = action3 as VoiceAction.CreateTask
        assertEquals("Call mom", task3.title)
        assertEquals(18 * 60, task3.dueTimeMinutes) // 6pm -> 18:00 = 1080

        val action4 = VoiceActionParser.parse("call mom at 6 p.m.")
        assertTrue(action4 is VoiceAction.CreateTask)
        val task4 = action4 as VoiceAction.CreateTask
        assertEquals("Call mom", task4.title)
        assertEquals(18 * 60, task4.dueTimeMinutes) // 6 p.m. -> 18:00 = 1080
    }
}

