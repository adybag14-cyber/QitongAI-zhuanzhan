package com.qtwl.YitongAIzhuanzhan

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReplyBridgeTest {
    @Before
    fun setUp() {
        ReplyBridge.clearForTests()
    }

    @After
    fun tearDown() {
        ReplyBridge.clearForTests()
    }

    @Test
    fun deliverRoutesReplyOnceAndRemovesPendingCallback() {
        var received = ""
        ReplyBridge.register("request-1") { received = it }

        assertEquals(1, ReplyBridge.pendingCount())
        assertTrue(ReplyBridge.deliver("request-1", "  captured reply  "))
        assertEquals("captured reply", received)
        assertEquals(0, ReplyBridge.pendingCount())
        assertFalse(ReplyBridge.deliver("request-1", "duplicate"))
        assertEquals("captured reply", received)
    }

    @Test
    fun unregisterPreventsLateJavascriptReplyFromCompletingRequest() {
        var called = false
        ReplyBridge.register("request-2") { called = true }
        ReplyBridge.unregister("request-2")

        assertFalse(ReplyBridge.deliver("request-2", "late reply"))
        assertFalse(called)
        assertEquals(0, ReplyBridge.pendingCount())
    }
}
