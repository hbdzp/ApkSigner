package io.github.jixiaoyong.pages.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import io.github.jd1378.otphelper.data.local.PreferenceDataStoreHelper
import io.github.jixiaoyong.data.SettingPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertTrue

class SettingInfoViewModelTest {
    @Test fun `missing JAVA_HOME does not prevent settings from being saved`() = runBlocking {
        assumeTrue("Requires an unset JAVA_HOME", System.getenv("JAVA_HOME") == null)
        val state = MutableStateFlow<Preferences>(emptyPreferences())
        val store = object : DataStore<Preferences> {
            override val data = state
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
                transform(state.value).also { state.value = it }
        }
        val repository = SettingPreferencesRepository(PreferenceDataStoreHelper(store))
        val viewModel = SettingInfoViewModel(repository)
        viewModel.onInit()
        kotlinx.coroutines.delay(300)
        viewModel.saveAutoMatchSignature(true)
        assertTrue(withTimeout(2_000) { repository.isAutoMatchSignature.first { it } })
    }
}
