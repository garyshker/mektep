package app.tisimai.mektep.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tisimai.mektep.data.local.ParentalConfigDao
import app.tisimai.mektep.data.local.ParentalPrefsStore
import app.tisimai.mektep.data.models.ParentalConfig
import app.tisimai.mektep.util.PinHasher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val parentalConfigDao: ParentalConfigDao,
    private val parentalPrefsStore: ParentalPrefsStore
) : ViewModel() {

    private val _pinError = MutableStateFlow<String?>(null)
    val pinError: StateFlow<String?> = _pinError.asStateFlow()

    private val _setupComplete = MutableStateFlow(false)
    val setupComplete: StateFlow<Boolean> = _setupComplete.asStateFlow()

    fun setMode(mode: String) {
        viewModelScope.launch {
            parentalPrefsStore.setDeviceMode(mode)
            if (mode == "NONE") {
                parentalConfigDao.upsertConfig(ParentalConfig(mode = "NONE"))
                parentalPrefsStore.setSetupCompleted(true)
            }
        }
    }

    fun savePin(pin: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val salt = PinHasher.generateSalt()
            val hash = PinHasher.hash(pin, salt)

            parentalPrefsStore.savePin(hash, salt)

            val existing = parentalConfigDao.getConfigOnce()
            val mode = existing?.mode ?: "SAME_DEVICE"
            parentalConfigDao.upsertConfig(
                ParentalConfig(
                    mode = mode,
                    pinHash = hash,
                    pinSalt = salt
                )
            )

            onSuccess()
        }
    }

    fun verifyPin(pin: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val config = parentalConfigDao.getConfigOnce()
            if (config != null && PinHasher.verify(pin, config.pinSalt, config.pinHash)) {
                _pinError.value = null
                onSuccess()
            } else {
                _pinError.value = "Wrong PIN"
            }
        }
    }

    fun completeSetup() {
        viewModelScope.launch {
            parentalPrefsStore.setSetupCompleted(true)
            _setupComplete.value = true
        }
    }
}
