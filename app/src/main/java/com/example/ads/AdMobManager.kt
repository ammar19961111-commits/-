package com.example.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.example.R
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Pure policy governing ad-free windows and non-intrusive interstitial frequency caps.
 */
object AdFrequencyPolicy {
    const val AD_FREE_REWARD_DURATION_MS = 24L * 60L * 60L * 1000L
    const val MAX_AD_FREE_BANK_MS = 7L * 24L * 60L * 60L * 1000L
    const val MIN_INTERSTITIAL_INTERVAL_MS = 180_000L // 3 minutes cooldown
    const val MIN_TRANSITIONS_BEFORE_INTERSTITIAL = 2

    fun isAdFreeActive(adFreeUntilMillis: Long, nowMillis: Long): Boolean {
        return adFreeUntilMillis > nowMillis
    }

    fun computeExtendedAdFreeUntilMillis(
        currentAdFreeUntilMillis: Long,
        nowMillis: Long,
        durationMillis: Long = AD_FREE_REWARD_DURATION_MS
    ): Long {
        val base = maxOf(nowMillis, currentAdFreeUntilMillis)
        val rawTarget = base + durationMillis.coerceAtLeast(0L)
        val maxCap = nowMillis + MAX_AD_FREE_BANK_MS
        return rawTarget.coerceAtMost(maxCap)
    }

    fun shouldShowInterstitial(
        isAdFreeActive: Boolean,
        naturalTransitionsCount: Int,
        lastInterstitialShownAtMillis: Long,
        nowMillis: Long
    ): Boolean {
        if (isAdFreeActive) return false
        if (naturalTransitionsCount < MIN_TRANSITIONS_BEFORE_INTERSTITIAL) return false
        if (lastInterstitialShownAtMillis > 0L &&
            (nowMillis - lastInterstitialShownAtMillis) < MIN_INTERSTITIAL_INTERVAL_MS
        ) {
            return false
        }
        return true
    }

    fun remainingDurationMinutes(adFreeUntilMillis: Long, nowMillis: Long): Int {
        val diff = (adFreeUntilMillis - nowMillis).coerceAtLeast(0L)
        return ((diff + 59_999L) / 60_000L).toInt()
    }

    fun remainingHoursAndMinutes(adFreeUntilMillis: Long, nowMillis: Long): Pair<Int, Int> {
        val totalMinutes = remainingDurationMinutes(adFreeUntilMillis, nowMillis)
        return (totalMinutes / 60) to (totalMinutes % 60)
    }
}

data class AdControllerState(
    val isInitialized: Boolean = false,
    val isInterstitialReady: Boolean = false,
    val isRewardedReady: Boolean = false,
    val isRewardedLoading: Boolean = false
)

interface AdMobController {
    val state: StateFlow<AdControllerState>
    fun initialize()
    fun preloadIfNeeded(isAdFreeActive: Boolean) = Unit
    fun maybeShowInterstitial(activity: Activity?, onFinished: () -> Unit = {})
    fun showRewardedAd(
        activity: Activity?,
        onRewardEarned: () -> Unit,
        onUnavailable: () -> Unit
    )
}

object NoOpAdMobController : AdMobController {
    private val _state = MutableStateFlow(AdControllerState())
    override val state: StateFlow<AdControllerState> = _state.asStateFlow()
    override fun initialize() = Unit
    override fun maybeShowInterstitial(activity: Activity?, onFinished: () -> Unit) {
        onFinished()
    }

    override fun showRewardedAd(
        activity: Activity?,
        onRewardEarned: () -> Unit,
        onUnavailable: () -> Unit
    ) {
        onUnavailable()
    }
}

class AndroidAdMobController(context: Context) : AdMobController {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(AdControllerState())
    override val state: StateFlow<AdControllerState> = _state.asStateFlow()

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var isLoadingInterstitial = false
    private var isLoadingRewarded = false
    private var hasStartedInit = false
    private var lastInterstitialAttemptMs = 0L
    private var lastRewardedAttemptMs = 0L
    private val pendingRewardedCallbacks = mutableListOf<(RewardedAd?) -> Unit>()

    override fun initialize() {
        if (hasStartedInit) return
        hasStartedInit = true
        scope.launch(Dispatchers.IO) {
            val initialized = runCatching {
                MobileAds.initialize(appContext) {}
                true
            }.getOrDefault(false)
            scope.launch(Dispatchers.Main.immediate) {
                _state.update { it.copy(isInitialized = initialized) }
                if (initialized) {
                    loadInterstitial()
                    loadRewarded()
                }
            }
        }
    }

    override fun preloadIfNeeded(isAdFreeActive: Boolean) {
        if (!_state.value.isInitialized) return
        val now = System.currentTimeMillis()
        if (!isAdFreeActive && interstitialAd == null && !isLoadingInterstitial &&
            (now - lastInterstitialAttemptMs) >= MIN_RELOAD_RETRY_MS
        ) {
            loadInterstitial()
        }
        if (rewardedAd == null && !isLoadingRewarded &&
            (now - lastRewardedAttemptMs) >= MIN_RELOAD_RETRY_MS
        ) {
            loadRewarded()
        }
    }

    private fun loadInterstitial() {
        if (isLoadingInterstitial || interstitialAd != null) return
        isLoadingInterstitial = true
        lastInterstitialAttemptMs = System.currentTimeMillis()
        val adUnitId = runCatching { appContext.getString(R.string.admob_interstitial_unit_id) }.getOrNull()
        if (adUnitId.isNullOrBlank()) {
            isLoadingInterstitial = false
            return
        }
        runCatching {
            InterstitialAd.load(
                appContext,
                adUnitId,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        isLoadingInterstitial = false
                        interstitialAd = ad
                        _state.update { it.copy(isInterstitialReady = true) }
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        isLoadingInterstitial = false
                        interstitialAd = null
                        _state.update { it.copy(isInterstitialReady = false) }
                    }
                }
            )
        }.onFailure {
            isLoadingInterstitial = false
        }
    }

    private fun flushPendingRewardedCallbacks(ad: RewardedAd?) {
        if (pendingRewardedCallbacks.isEmpty()) return
        val snapshot = pendingRewardedCallbacks.toList()
        pendingRewardedCallbacks.clear()
        snapshot.forEach { callback -> callback(ad) }
    }

    private fun loadRewarded(onReadyCallback: ((RewardedAd?) -> Unit)? = null) {
        val existing = rewardedAd
        if (existing != null) {
            onReadyCallback?.invoke(existing)
            return
        }
        if (onReadyCallback != null) {
            pendingRewardedCallbacks.add(onReadyCallback)
        }
        if (isLoadingRewarded) return
        isLoadingRewarded = true
        lastRewardedAttemptMs = System.currentTimeMillis()
        _state.update { it.copy(isRewardedLoading = true) }

        val adUnitId = runCatching { appContext.getString(R.string.admob_rewarded_unit_id) }.getOrNull()
        if (adUnitId.isNullOrBlank()) {
            isLoadingRewarded = false
            _state.update { it.copy(isRewardedLoading = false) }
            flushPendingRewardedCallbacks(null)
            return
        }

        runCatching {
            RewardedAd.load(
                appContext,
                adUnitId,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        isLoadingRewarded = false
                        rewardedAd = ad
                        _state.update {
                            it.copy(isRewardedReady = true, isRewardedLoading = false)
                        }
                        flushPendingRewardedCallbacks(ad)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        isLoadingRewarded = false
                        rewardedAd = null
                        _state.update {
                            it.copy(isRewardedReady = false, isRewardedLoading = false)
                        }
                        flushPendingRewardedCallbacks(null)
                    }
                }
            )
        }.onFailure {
            isLoadingRewarded = false
            _state.update { it.copy(isRewardedLoading = false) }
            flushPendingRewardedCallbacks(null)
        }
    }

    override fun maybeShowInterstitial(activity: Activity?, onFinished: () -> Unit) {
        val currentAd = interstitialAd
        if (activity == null || activity.isFinishing || currentAd == null) {
            loadInterstitial()
            onFinished()
            return
        }

        interstitialAd = null
        _state.update { it.copy(isInterstitialReady = false) }

        currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                loadInterstitial()
                onFinished()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                loadInterstitial()
                onFinished()
            }
        }
        runCatching {
            currentAd.show(activity)
        }.onFailure {
            loadInterstitial()
            onFinished()
        }
    }

    override fun showRewardedAd(
        activity: Activity?,
        onRewardEarned: () -> Unit,
        onUnavailable: () -> Unit
    ) {
        if (activity == null || activity.isFinishing) {
            onUnavailable()
            return
        }

        val showNow: (RewardedAd) -> Unit = { ad ->
            rewardedAd = null
            _state.update { it.copy(isRewardedReady = false) }
            var rewardEarned = false
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    loadRewarded()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    if (!rewardEarned) onUnavailable()
                    loadRewarded()
                }
            }
            runCatching {
                ad.show(activity) {
                    rewardEarned = true
                    onRewardEarned()
                }
            }.onFailure {
                onUnavailable()
            }
        }

        val ready = rewardedAd
        if (ready != null) {
            showNow(ready)
        } else {
            loadRewarded { loaded ->
                if (loaded != null && !activity.isFinishing) {
                    showNow(loaded)
                } else {
                    onUnavailable()
                }
            }
        }
    }

    private companion object {
        const val MIN_RELOAD_RETRY_MS = 30_000L
    }
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
