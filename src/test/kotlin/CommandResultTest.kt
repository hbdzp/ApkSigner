import io.github.jixiaoyong.beans.CommandResult
import io.github.jixiaoyong.data.SettingPreferencesRepository
import io.github.jixiaoyong.pages.signapp.SignAppViewModel
import io.github.jd1378.otphelper.data.local.PreferenceDataStoreHelper
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.flowOf
import kotlin.test.*

class CommandResultTest {
    private val viewModel = SignAppViewModel(SettingPreferencesRepository(PreferenceDataStoreHelper(
        object : DataStore<Preferences> {
            override val data = flowOf(emptyPreferences())
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
                error("Batch result aggregation must not access preferences")
        }
    )))

    @Test fun `mixed results report failure with each item once`() {
        val result = viewModel.mergeCommandResult(listOf(CommandResult.Success("signed-one"), CommandResult.Error("failed-two")), listOf("one.apk", "two.apk"))
        val text = assertIs<CommandResult.Error<*>>(result).message.toString()
        assertEquals(1, Regex("signed-one").findAll(text).count())
        assertEquals(1, Regex("failed-two").findAll(text).count())
    }

    @Test fun `successful batch is not duplicated`() {
        val result = viewModel.mergeCommandResult(listOf(CommandResult.Success("signed-one"), CommandResult.Success("signed-two")), listOf("one.apk", "two.apk"))
        val text = assertIs<CommandResult.Success<*>>(result).result.toString()
        assertEquals(1, Regex("signed-one").findAll(text).count())
        assertEquals(1, Regex("signed-two").findAll(text).count())
    }

    @Test fun `unfinished state returns an error without a cast crash`() {
        val result = viewModel.mergeCommandResult(listOf(CommandResult.EXECUTING), listOf("one.apk"))
        assertIs<CommandResult.Error<*>>(result)
    }
}
