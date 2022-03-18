package tech.hombre.bluetoothchatter.utils

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import java.util.*

const val KEY_RESULT = "nav_extender_result"
const val ARG_REQUEST_CODE = "nav_extender_request_code"
const val RESULT_OK = 1
const val RESULT_CANCELED = 2

/**
 *
 * Use this when you need to navigate for some direction and get result from it.
 * Your destination fragment should implement **[Fragment.setFragmentResult]** with code **[RESULT_OK]**
 * in case if action should be called.
 * Check list for implementing navigate with result:
 * 1. Create request parameter at target destination
 * 2. Implement **[Fragment.setFragmentResult]** with code **[RESULT_OK]** or use **[setResultOk]**
 * 3. Call this method and pass generated `request_key` by `directionBuilder`
 *
 */
fun Fragment.navigateWithResult(
    direction: NavDirections,
    onCanceled: () -> Unit = {},
    action: (args: Bundle) -> Unit = {}
) {
    kotlin.runCatching {
        val key = UUID.randomUUID().toString()
        val newArgs = Bundle(direction.arguments).apply { putString(ARG_REQUEST_CODE, key) }
        findNavController().navigate(direction.actionId, newArgs)
        setFragmentResultListener(key) { _, bundle ->
            when (bundle[KEY_RESULT]) {
                RESULT_OK -> action.invoke(bundle)
                RESULT_CANCELED -> onCanceled.invoke()
            }
        }
    }.onFailure {
        it.printStackTrace()
    }
}

fun Fragment.setResultOk(
    vararg bundleParams: Pair<String, Any>,
    requestCodeRequired: Boolean = false
) {
    val requestKey = requireArguments().getString(ARG_REQUEST_CODE)
    if (requestCodeRequired && requestKey == null) {
        throw IllegalStateException("$ARG_REQUEST_CODE parameter not found in fragment arguments")
    }
    requestKey?.let {
        setFragmentResult(
            requestKey,
            bundleOf(KEY_RESULT to RESULT_OK, *bundleParams)
        )
    }
}

fun Fragment.setResultOkSafe(vararg bundleParams: Pair<String, Any>) {
    val requestKey = arguments?.getString(ARG_REQUEST_CODE) ?: return
    setFragmentResult(requestKey, bundleOf(KEY_RESULT to RESULT_OK, *bundleParams))
}

fun Fragment.setResultOk(bundle: Bundle = Bundle.EMPTY) {
    val requestKey = requireArguments().getString(ARG_REQUEST_CODE)
        ?: throw IllegalStateException("$ARG_REQUEST_CODE parameter not found in fragment arguments")
    setFragmentResult(requestKey, bundle.apply {
        putInt(KEY_RESULT, RESULT_OK)
    })
}

fun Fragment.setResultCanceled(vararg bundleParams: Pair<String, Any>) {
    val requestKey = requireArguments().getString(ARG_REQUEST_CODE)
        ?: throw IllegalStateException("$ARG_REQUEST_CODE parameter not found in fragment arguments")
    setFragmentResult(requestKey, bundleOf(KEY_RESULT to RESULT_CANCELED, *bundleParams))
}

fun Fragment.navigate(directions: NavDirections) {
    kotlin.runCatching { findNavController().navigate(directions) }.onFailure {
        it.printStackTrace()
    }
}