/*
 * Copyright 2019 Babylon Partners Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.babylon.certificatetransparency.datasource

import com.babylon.certificatetransparency.internal.loglist.InMemoryDataSource
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.internal.verification.Times

class DataSourceComposeTest {

    @Test
    fun testComposeQueriesBothEmptyCaches() = runBlocking {
        // given we have two composed empty caches
        val cache1 = spy(InMemoryDataSource<Int>())
        val cache2 = spy(InMemoryDataSource<Int>())
        val composed = cache1 + cache2

        // when we ask for a value
        val value = composed.get()

        // then nothing is returned and both caches are queried
        assertNull(value)

        verify(cache1, Times(1)).get()
        verify(cache2, Times(1)).get()

        verify(cache1, Times(0)).set(any())
        verify(cache2, Times(0)).set(any())
    }

    @Test
    fun testComposeQueriesOnlyFirstPopulatedCache() = runBlocking {
        // given we have two composed empty caches
        val cache1 = spy(InMemoryDataSource<Int>().apply { set(1) })
        val cache2 = spy(InMemoryDataSource<Int>().apply { set(2) })
        val composed = cache1 + cache2

        // when we ask for a value
        val value = composed.get()

        // then value from first cache is returned and second cache is not called
        assertEquals(1, value)

        verify(cache1, Times(1)).get()
        verify(cache2, Times(0)).get()

        verify(cache1, Times(0)).set(any())
        verify(cache2, Times(0)).set(any())
    }

    @Test
    fun testComposeQueriesSecondCacheAndSetsValueInFirst() = runBlocking {
        // given we have two composed empty caches
        val cache1 = spy(InMemoryDataSource<Int>())
        val cache2 = spy(InMemoryDataSource<Int>().apply { set(2) })
        val composed = cache1 + cache2

        // when we ask for a value
        val value = composed.get()

        // then value from second cache is returned and value set in first cache
        assertEquals(2, value)
        assertEquals(2, cache1.get())

        verify(cache1, Times(2)).get()
        verify(cache2, Times(1)).get()

        verify(cache1, Times(1)).set(any())
        verify(cache2, Times(0)).set(any())
    }

    @Test
    fun testComposeQueriesSecondCacheAndSetsValueInFirstWhenIsValidReturnsFalse() = runBlocking {
        // given we have two composed empty caches
        val cache1 = spy(InMemoryDataSource<Int>().apply { set(2) })
        whenever(cache1.isValid(anyInt())).thenReturn(false)

        val cache2 = spy(InMemoryDataSource<Int>().apply { set(3) })
        val composed = cache1 + cache2

        // when we ask for a value
        val value = composed.get()

        // then value from second cache is returned and value set in first cache
        assertEquals(3, value)
        assertEquals(3, cache1.get())

        verify(cache1, Times(2)).get()
        verify(cache2, Times(1)).get()

        verify(cache1, Times(1)).set(any())
        verify(cache2, Times(0)).set(any())
    }

    @Test
    fun testComposeThrowsExceptionWhenFirstErrors() = runBlocking {
        // given we have two composed empty caches
        val cache1 = spy(InMemoryDataSource<Int>())
        val cache2 = spy(InMemoryDataSource<Int>())
        val composed = cache1 + cache2

        whenever(cache1.get()).then { throw IllegalStateException() }

        // when we ask for a value
        // expect an exception
        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                composed.get()
            }
        }

        Unit
    }

    @Test
    fun testComposeThrowsExceptionWhenSecondErrors() = runBlocking {
        // given we have two composed empty caches
        val cache1 = spy(InMemoryDataSource<Int>())
        val cache2 = spy(InMemoryDataSource<Int>())
        val composed = cache1 + cache2

        whenever(cache2.get()).then { throw IllegalStateException() }

        // when we ask for a value
        // expect an exception
        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                composed.get()
            }
        }

        Unit
    }

    @Test
    fun testComposeThrowsExceptionWhenSecondCacheIsNull() {
        // when second cache is null
        // expect exception
        val throwable = assertThrows(IllegalArgumentException::class.java) {
            InMemoryDataSource<Int>() + uninitialized()
        }
        assertTrue(throwable.message!!.startsWith("Parameter specified as non-null is null"))
    }

    @Test
    @Ignore("Re-visit cache implementation in latest coroutine library")
    fun `throw exception when job cancelled on get and first cache is executing get`() = runBlocking {
        val firstCache = spy<InMemoryDataSource<String>>()
        val secondCache = spy<InMemoryDataSource<String>>()
        val composedCache = firstCache.compose(secondCache)

        // given the first cache cancels
        whenever(firstCache.get()).then { coroutineContext.cancel() }
        whenever(secondCache.get()).then { throw IllegalStateException() }

        // when we get the value
        // expect exception
        val throwable = assertThrows(CancellationException::class.java) {
            runBlocking {
                composedCache.get()
            }
        }
        assertTrue(throwable.message!!.startsWith("Job was cancelled"))
    }

    @Test
    @Ignore("Re-visit cache implementation in latest coroutine library")
    fun `throw exception when job cancelled on get and second cache is executing get`() = runBlocking {
        val firstCache = spy<InMemoryDataSource<String>>()
        val secondCache = spy<InMemoryDataSource<String>>()
        val composedCache = firstCache.compose(secondCache)

        // given the second cache cancels
        whenever(secondCache.get()).then { coroutineContext.cancel() }

        // when we get the value
        // expect exception
        val throwable = assertThrows(CancellationException::class.java) {
            runBlocking {
                composedCache.get()
            }
        }
        assertTrue(throwable.message!!.startsWith("Job was cancelled"))
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun <T> uninitialized(): T = null as T
    }
}
