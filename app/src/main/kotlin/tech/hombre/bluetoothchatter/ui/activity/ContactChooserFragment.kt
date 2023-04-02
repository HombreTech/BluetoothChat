package tech.hombre.bluetoothchatter.ui.activity

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import tech.hombre.bluetoothchatter.R
import tech.hombre.bluetoothchatter.databinding.FragmentContactChooserBinding
import tech.hombre.bluetoothchatter.ui.adapter.ContactsAdapter
import tech.hombre.bluetoothchatter.ui.presenter.ContactChooserPresenter
import tech.hombre.bluetoothchatter.ui.view.ContactChooserView
import tech.hombre.bluetoothchatter.ui.viewmodel.ContactViewModel

class ContactChooserFragment :
    BaseFragment<FragmentContactChooserBinding>(R.layout.fragment_contact_chooser),
    ContactChooserView {

    private val args: ContactChooserFragmentArgs by navArgs()

    private val message: String? by lazy { args.message }
    private val filePath: String? by lazy { args.filePath }

    private val presenter: ContactChooserPresenter by inject { parametersOf(this) }

    private val contactsAdapter = ContactsAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycle.addObserver(presenter)

        binding.appBar.tbToolbar.title = getString(R.string.contact_chooser__title)
        binding.rvContacts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvContacts.adapter = contactsAdapter

        contactsAdapter.clickListener = {
            findNavController().navigate(
                ContactChooserFragmentDirections.actionContactChooserFragmentToChatFragment(
                    address = it.address,
                    message = message,
                    filepath = filePath,
                    nickname = it.name,
                )
            )
        }
    }

    override fun showContacts(contacts: List<ContactViewModel>) {
        contactsAdapter.conversations = ArrayList(contacts)
        contactsAdapter.notifyDataSetChanged()
    }

    override fun showNoContacts() {
        binding.tvNoContacts.visibility = View.VISIBLE
        binding.rvContacts.visibility = View.GONE
    }
}
