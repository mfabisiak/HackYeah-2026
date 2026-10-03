package io.github.mfabisiak.hubmi.samples

import arrow.core.Either
import com.mongodb.client.model.IndexOptions
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.model.Indexes
import io.github.mfabisiak.hubmi.MongoTestEnvironment
import io.github.mfabisiak.hubmi.api.PageRequest
import io.github.mfabisiak.hubmi.common.Description
import io.github.mfabisiak.hubmi.common.RepositoryError
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.BeforeClass
import kotlin.test.*

class SampleRepositoryTest {
    companion object {
        private lateinit var client: MongoClient
        private lateinit var repository: SampleRepository

        @BeforeClass
        @JvmStatic
        fun setUpContainer() {
            client = MongoClient.create(MongoTestEnvironment.connectionString)
            val database = client.getDatabase("test-hubmi-repo")
            repository = SampleRepository(database)

            runBlocking {
                database.drop()
                database.samples.createIndex(Indexes.ascending(SampleItem::slug), IndexOptions().unique(true))
            }
        }

        @AfterClass
        @JvmStatic
        fun tearDownContainer() {
            client.close()
        }
    }

    @Test
    fun createAndFindById() =
        runBlocking {
            val item =
                SampleItem(
                    slug = "test-item-1",
                    name = "Test Item 1",
                    description = "Description 1",
                )

            val created = repository.create(item)
            assertTrue(created is Either.Right)
            assertEquals("test-item-1", created.value.slug)

            val found = repository.findById(item.id)
            assertTrue(found is Either.Right)
            assertNotNull(found.value)
            assertEquals("Test Item 1", found.value?.name)
        }

    @Test
    fun uniqueConstraintViolationReturnsConflict() =
        runBlocking {
            val item1 =
                SampleItem(
                    slug = "duplicate-slug",
                    name = "Original Item",
                    description = "Original",
                )
            val item2 =
                SampleItem(
                    slug = "duplicate-slug",
                    name = "Duplicate Item",
                    description = "Duplicate",
                )

            val first = repository.create(item1)
            assertTrue(first is Either.Right)

            val second = repository.create(item2)
            assertTrue(second is Either.Left)
            assertTrue(second.value is RepositoryError.Conflict)
        }

    @Test
    fun paginationReturnsItemsAndTotal() =
        runBlocking {
            for (i in 1..5) {
                repository.create(
                    SampleItem(
                        slug = "paged-slug-$i",
                        name = "Paged Item $i",
                        description = "Desc $i",
                    ),
                )
            }

            val pageResult = repository.findAll(PageRequest(page = 0, size = 3))
            assertTrue(pageResult is Either.Right)
            assertEquals(3, pageResult.value.items.size)
            assertTrue(pageResult.value.total >= 5)
        }

    @Test
    fun deleteRemovesItem() =
        runBlocking {
            val item =
                SampleItem(
                    slug = "delete-me",
                    name = "Delete Me",
                    description = "To be deleted",
                )
            repository.create(item)

            val deleted = repository.deleteById(item.id)
            assertTrue(deleted is Either.Right)
            assertTrue(deleted.value)

            val foundAfterDelete = repository.findById(item.id)
            assertTrue(foundAfterDelete is Either.Right)
            assertNull(foundAfterDelete.value)
        }
}
