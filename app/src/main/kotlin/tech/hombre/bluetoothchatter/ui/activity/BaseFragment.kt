package tech.hombre.bluetoothchatter.ui.activity

import android.content.ActivityNotFoundException
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import tech.hombre.bluetoothchatter.R
import tech.hombre.bluetoothchatter.ui.util.ThemeHolder

open class BaseFragment<ViewBinding : ViewDataBinding>(@LayoutRes val layoutResId: Int) : Fragment(layoutResId) {

    protected lateinit var binding: ViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        val nightMode = (requireActivity().application as ThemeHolder).getNightMode()
        AppCompatDelegate.setDefaultNightMode(nightMode)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, layoutResId, container, false)
        return binding.root
    }

    protected inline fun doIfStarted(action: () -> Unit) {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            action.invoke()
        }
    }

    protected fun openLink(link: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(link)
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), R.string.general__no_browser, Toast.LENGTH_LONG).show()
        }
    }

    protected fun hideKeyboard() {
        val inputManager = requireContext().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        requireActivity().currentFocus?.let { view ->
            inputManager.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        }
    }
}
