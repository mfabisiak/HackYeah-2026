package io.github.mfabisiak.hubmi.tester

import io.github.mfabisiak.hubmi.api.TestRequestStatus

/** A request is decided once: `NEW` becomes `ACCEPTED` or `DECLINED`, and a decision is final. */
fun TestRequestStatus.canMoveTo(next: TestRequestStatus): Boolean =
    when (this) {
        TestRequestStatus.NEW -> next == TestRequestStatus.ACCEPTED || next == TestRequestStatus.DECLINED
        TestRequestStatus.ACCEPTED, TestRequestStatus.DECLINED -> false
    }
