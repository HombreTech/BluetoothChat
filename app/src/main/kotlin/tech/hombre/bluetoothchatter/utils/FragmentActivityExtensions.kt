package tech.hombre.bluetoothchatter.utils

import androidx.annotation.IdRes
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment

/**
 * Returns the fragment for [fragmentId] as a [NavHostFragment]. Throws an error if the fragment
 * fails to cast.
 */
fun FragmentActivity.getAsNavHostFragmentFor(@IdRes fragmentId: Int): NavHostFragment =
    supportFragmentManager.findFragmentById(fragmentId) as NavHostFragment

/**
 * Returns the [NavController] associated with the [fragmentId].
 */
fun FragmentActivity.findNavControllerFor(@IdRes fragmentId: Int): NavController =
    getAsNavHostFragmentFor(fragmentId).navController
