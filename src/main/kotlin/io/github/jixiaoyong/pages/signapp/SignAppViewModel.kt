package io.github.jixiaoyong.pages.signapp

import ApkSigner
import Logger
import io.github.jixiaoyong.base.BaseViewModel
import io.github.jixiaoyong.beans.CommandResult
import io.github.jixiaoyong.beans.SignInfoBean
import io.github.jixiaoyong.data.SettingPreferencesRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath

/**
 * @author : jixiaoyong
 * @description ：sign app view model
 *
 * @email : jixiaoyong1995@gmail.com
 * @date : 25/3/2024
 */
class SignAppViewModel(private val settings: SettingPreferencesRepository) : BaseViewModel() {
    private val TITLE_CONTENT_DIVIDER = "-------------------------------------------------------"

    private val uiStateFlow = MutableStateFlow(SignAppState())
    val uiState = uiStateFlow.asStateFlow()

    override fun onInit() {
        combine(
            settings.signTypeList,
            settings.selectedSignInfoBean,
            settings.signInfoBeans,
            settings.signedDirectory,
            settings.isZipAlign,
            settings.isAutoMatchSignature
        ) { params ->
            uiStateFlow.value.copy(
                apkSignType = params[0] as Set<Int>,
                globalSelectedSignInfo = params[1] as SignInfoBean?,
                allSignInfoBeans = params[2] as List<SignInfoBean>?,
                signedOutputDirectory = params[3] as String?,
                isZipAlign = params[4] as Boolean,
                isAutoMatchSignature = params[5] as Boolean
            )
        }.onEach {
            uiStateFlow.value = it
        }.launchIn(viewModelScope)

        settings.apkSignatureMap.onEach {
            uiStateFlow.value = uiStateFlow.value.copy(apkSignatureMap = it)
        }.launchIn(viewModelScope)

        // 监听并更新当前apk对应的签名信息
        uiStateFlow.distinctUntilChanged { old, new ->
            return@distinctUntilChanged (old.allSignInfoBeans == new.allSignInfoBeans
                    && old.globalSelectedSignInfo == new.globalSelectedSignInfo
                    && old.apkPackageName == new.apkPackageName
                    && old.isAutoMatchSignature == new.isAutoMatchSignature
                    && old.apkSignatureMap == new.apkSignatureMap)
        }.onEach { uiState ->
            val currentSignInfo = if (null == uiState.apkPackageName || !uiState.isAutoMatchSignature) {
                uiState.globalSelectedSignInfo
            } else {
                uiState.apkSignatureMap.get(uiState.apkPackageName)?.let { id ->
                    uiState.allSignInfoBeans?.find { it.isValid() && it.id == id }
                } ?: uiState.globalSelectedSignInfo
            }
            uiStateFlow.emit(uiState.copy(currentSignInfo = currentSignInfo))
            Logger.log("apkPackageName:${uiState.apkPackageName} currentSignInfo:$currentSignInfo ")
        }.launchIn(viewModelScope)

    }

    suspend fun changeApkFilePath(apkFilePath: List<String>) {
        // 每次重新选择apk的时候重置此标志
        val apkPackageName = if (1 != apkFilePath.size) {
            null
        } else {
            ApkSigner.getApkPackageName(apkFilePath.firstOrNull())
        }

        val outputDir = uiStateFlow.value.signedOutputDirectory ?: apkFilePath.firstOrNull()?.run {
            this.toPath().parent.toString()
        }
        saveSignedOutputDirectory(outputDir)

        uiStateFlow.emit(
            uiStateFlow.value.copy(
                apkFilePaths = apkFilePath,
                apkPackageName = apkPackageName,
                apkSignedResult = CommandResult.NOT_EXECUT,
                signedOutputDirectory = outputDir,
                signedLogs = emptyList()
            )
        )
    }

    fun removeApkSignature(packageName: String?) {
        if (null == packageName) {
            return
        }
        viewModelScope.launch {
            val newMap = uiState.value.apkSignatureMap.toMutableMap()
            newMap.remove(packageName)
            settings.saveApkSignatureMap(newMap)
        }
    }

    fun updateApkSignatureMap(packageName: String?, localSelectedSignInfo: SignInfoBean) {
        viewModelScope.launch {
            if (null == packageName || !localSelectedSignInfo.isValid()) {
                return@launch
            }

            val newMap = uiState.value.apkSignatureMap.toMutableMap()
            newMap[packageName] = localSelectedSignInfo.id
            settings.saveApkSignatureMap(newMap)
        }
    }


    fun changeSignInfo(signInfoResult: CommandResult) {
        viewModelScope.launch { uiStateFlow.emit(uiStateFlow.value.copy(signInfoResult = signInfoResult)) }
    }

    fun saveSignedOutputDirectory(signedDirectory: String?) {
        viewModelScope.launch { settings.saveSignedDirectory(signedDirectory) }
    }

    fun changeZipAlign(isZipAlign: Boolean) {
        viewModelScope.launch { settings.saveIsZipAlign(isZipAlign) }
    }

    fun changeApkSignType(newTypes: MutableSet<Int>) {
        viewModelScope.launch { settings.saveSignTypeList(newTypes) }
    }

    fun changeSignApkResult(signApkResult: CommandResult) {
        viewModelScope.launch { uiStateFlow.emit(uiStateFlow.value.copy(apkSignedResult = signApkResult)) }
    }

    fun updateSignedLogs(logs: List<String>) {
        viewModelScope.launch { uiStateFlow.emit(uiStateFlow.value.copy(signedLogs = logs)) }
    }


    /**
     * 根据给定的 [pathList] 将给定的 [CommandResult] 列表合并为单个 [CommandResult]。
     *
     * 只有所有任务成功才返回 Success；失败时仍保留每个文件的结果。
     *
     * @param resultList 要合并的 [CommandResult] 列表
     * @param pathList [resultList]中每个[CommandResult]对应的路径列表
     * @return 单个 [CommandResult]，它是合并给定 [resultList] 的结果
     */
    fun mergeCommandResult(resultList: List<CommandResult>, pathList: List<String>): CommandResult {
        if (resultList.isEmpty()) return CommandResult.Error("没有可处理的文件")
        val message = resultList.mapIndexed { index, result ->
            val detail = when (result) {
                is CommandResult.Success<*> -> result.result.toString()
                is CommandResult.Error<*> -> result.message.toString()
                CommandResult.EXECUTING -> "任务尚未完成"
                CommandResult.NOT_EXECUT -> "任务尚未执行"
            }
            "$TITLE_CONTENT_DIVIDER\n[${index + 1}] ${pathList.getOrNull(index)}\n$TITLE_CONTENT_DIVIDER\n$detail"
        }.joinToString("\n\n")
        return if (resultList.all { it is CommandResult.Success<*> }) CommandResult.Success(message)
        else CommandResult.Error(message)
    }
}

data class SignAppState(
    val apkFilePaths: List<String> = emptyList(),
    val apkPackageName: String? = null,
    val signedLogs: List<String> = emptyList(),
    val apkSignType: Set<Int> = emptySet(),
    val globalSelectedSignInfo: SignInfoBean? = null,
    val allSignInfoBeans: List<SignInfoBean>? = null,
    val apkSignatureMap: Map<String, Long> = emptyMap(),
    val signedOutputDirectory: String? = null,
    val apkSignedResult: CommandResult = CommandResult.NOT_EXECUT,
    val isZipAlign: Boolean = false,
    val isAutoMatchSignature: Boolean = false,
    val currentSignInfo: SignInfoBean? = null,
    val signInfoResult: CommandResult = CommandResult.NOT_EXECUT,
)
