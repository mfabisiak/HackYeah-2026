package io.github.mfabisiak.hubmi.tester

import io.github.mfabisiak.hubmi.api.TestRequestStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class TestRequestTransitionsTest {
    @Test
    fun onlyNewRequestsCanBeDecidedAndDecisionsAreFinal() {
        val allowed =
            TestRequestStatus.entries.flatMap { from ->
                TestRequestStatus.entries.filter { to -> from.canMoveTo(to) }.map { to -> from to to }
            }

        assertEquals(
            listOf(
                TestRequestStatus.NEW to TestRequestStatus.ACCEPTED,
                TestRequestStatus.NEW to TestRequestStatus.DECLINED,
            ),
            allowed,
        )
    }
}
