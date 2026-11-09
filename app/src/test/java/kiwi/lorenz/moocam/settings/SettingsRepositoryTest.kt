package kiwi.lorenz.moocam.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) {
            folder.newFile("settings.preferences_pb").apply { delete() }
        }
        repository = SettingsRepository(dataStore)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun lastPhotoUri_isNullUntilSet() = runBlocking {
        assertNull(repository.lastPhotoUri.first())
    }

    @Test
    fun lastPhotoUri_returnsTheLatestPhoto() = runBlocking {
        repository.setLastPhotoUri("content://media/external/images/media/1")
        repository.setLastPhotoUri("content://media/external/images/media/2")
        assertEquals("content://media/external/images/media/2", repository.lastPhotoUri.first())
    }

    @Test
    fun clearLastPhotoUri_removesThatPhoto() = runBlocking {
        repository.setLastPhotoUri("content://media/external/images/media/1")
        repository.clearLastPhotoUri("content://media/external/images/media/1")
        assertNull(repository.lastPhotoUri.first())
    }

    @Test
    fun clearLastPhotoUri_keepsAPhotoTakenMeanwhile() = runBlocking {
        repository.setLastPhotoUri("content://media/external/images/media/2")
        repository.clearLastPhotoUri("content://media/external/images/media/1")
        assertEquals("content://media/external/images/media/2", repository.lastPhotoUri.first())
    }
}
