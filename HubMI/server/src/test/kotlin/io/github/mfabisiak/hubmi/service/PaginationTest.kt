package io.github.mfabisiak.hubmi.service

import arrow.core.Either
import io.github.mfabisiak.hubmi.api.ErrorCode
import io.github.mfabisiak.hubmi.api.PageRequest
import kotlin.test.*

class PaginationTest {
    @Test
    fun defaultValuesWhenNullProvided() {
        val result = validatePageRequest(null, null)
        assertTrue(result is Either.Right)
        assertEquals(PageRequest.DEFAULT_PAGE, result.value.page)
        assertEquals(PageRequest.DEFAULT_SIZE, result.value.size)
        assertEquals(0, result.value.skip)
        assertEquals(20, result.value.limit)
    }

    @Test
    fun validCustomValues() {
        val result = validatePageRequest(3, 15)
        assertTrue(result is Either.Right)
        assertEquals(3, result.value.page)
        assertEquals(15, result.value.size)
        assertEquals(45, result.value.skip)
        assertEquals(15, result.value.limit)
    }

    @Test
    fun negativePageFailsValidation() {
        val result = validatePageRequest(-1, 10)
        assertTrue(result is Either.Left)
        assertEquals(ErrorCode.VALIDATION_FAILED, result.value.code)
    }

    @Test
    fun zeroOrNegativeSizeFailsValidation() {
        val resultZero = validatePageRequest(0, 0)
        assertTrue(resultZero is Either.Left)
        assertEquals(ErrorCode.VALIDATION_FAILED, resultZero.value.code)

        val resultNegative = validatePageRequest(0, -5)
        assertTrue(resultNegative is Either.Left)
    }

    @Test
    fun sizeExceedingMaxFailsValidation() {
        val result = validatePageRequest(0, 101)
        assertTrue(result is Either.Left)
        assertEquals(ErrorCode.VALIDATION_FAILED, result.value.code)
    }
}
