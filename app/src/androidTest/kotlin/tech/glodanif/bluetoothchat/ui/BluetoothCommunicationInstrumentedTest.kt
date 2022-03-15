package tech.hombre.bluetoothchatter.ui

import android.Manifest
import android.app.Activity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.runner.AndroidJUnit4
import androidx.test.runner.permission.PermissionRequester
import tech.hombre.bluetoothchatter.R
import tech.hombre.bluetoothchatter.data.internal.AutoresponderProxy
import tech.hombre.bluetoothchatter.ui.UITestUtils.Companion.atPosition
import tech.hombre.bluetoothchatter.ui.UITestUtils.Companion.withToolbarSubTitle
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class BluetoothCommunicationInstrumentedTest {

    private val autoResponderAddress = "AC:22:0B:A1:89:A8"

    private val permissionRequester = PermissionRequester().apply {
        addPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    private val textMessageDelay = 2500.toLong()
    private val fileMessageDelay = 12500.toLong()
    private lateinit var context: Activity

    @Before
    fun setup() {
        permissionRequester.requestPermissions()
    }

    @Test
    fun communication() {
        checkNotConnected()
        checkNotSendingTextIfDisconnected()

        onView(withText(R.string.chat__connect)).perform(click())
        checkOutcommingConnection()

        checkTextMessageReceiving()
        checkTextMessageReceiving()
        checkFileMessageReceiving()
        checkFileMessageReceivingAndCancelByPartner()
        checkFileMessageReceiving()
        checkFileMessageReceivingAndCancelByPartner()
        checkTextMessageReceiving()

        checkDisconnectionByPartner()
        onView(withId(android.R.id.button2)).perform(click())

        onView(withText(R.string.chat__connect)).perform(click())
        checkOutcommingConnection()

        checkDisconnectionByPartner()
        onView(withId(android.R.id.button1)).perform(click())
        checkOutcommingConnection()

        checkDisconnectionByPartner()
        onView(withId(android.R.id.button2)).perform(click())

        checkNotConnected()
    }

    private fun checkOutcommingConnection() {
        onView(withText(R.string.chat__waiting_for_device))
        onView(withId(R.id.tb_toolbar)).check(matches(
                withToolbarSubTitle(context.getString(R.string.chat__pending))))
        Thread.sleep(textMessageDelay)
        onView(withId(R.id.av_actions)).check(matches(not(isDisplayed())))
        onView(withId(R.id.tb_toolbar)).check(matches(
                withToolbarSubTitle(context.getString(R.string.chat__connected))))
    }

       private fun checkFileMessageReceiving() {
        onView(withId(R.id.et_message)).perform(typeText(AutoresponderProxy.COMMAND_SEND_FILE))
        onView(withId(R.id.ib_send)).perform(click())
        Thread.sleep(textMessageDelay)
        onView(withText(R.string.chat__receiving_images)).check(matches(isDisplayed()))
        Thread.sleep(fileMessageDelay)
        onView(withId(R.id.rv_chat))
                .check(matches(atPosition(0, hasDescendant(withId(R.id.iv_image)))))
        onView(withText(R.string.chat__receiving_images)).check(matches(not(isDisplayed())))
    }

    private fun checkFileMessageReceivingAndCancelByPartner() {
        onView(withId(R.id.et_message)).perform(typeText(AutoresponderProxy.COMMAND_SEND_FILE_AND_CANCEL))
        onView(withId(R.id.ib_send)).perform(click())
        Thread.sleep(textMessageDelay * 2)
        onView(withText(R.string.chat__partner_canceled_image_transfer))
                .inRoot(withDecorView(not(`is`(context.window.decorView)))).check(matches(isDisplayed()))
        onView(withText(R.string.chat__receiving_images)).check(matches(not(isDisplayed())))
        onView(withId(R.id.rv_chat))
                .check(matches(atPosition(0, not(hasDescendant(withText(AutoresponderProxy.RESPONSE_RECEIVED))))))
    }

    private fun checkNotSendingTextIfDisconnected() {
        onView(withId(R.id.et_message)).perform(typeText(AutoresponderProxy.COMMAND_SEND_TEXT))
        onView(withId(R.id.ib_send)).perform(click())
        onView(withText(R.string.chat__not_connected_to_send))
                .inRoot(withDecorView(not(`is`(context.window.decorView)))).check(matches(isDisplayed()))
        onView(withId(R.id.et_message)).perform(clearText())
    }

    private fun checkTextMessageReceiving() {
        onView(withId(R.id.et_message)).perform(typeText(AutoresponderProxy.COMMAND_SEND_TEXT))
        onView(withId(R.id.ib_send)).perform(click())
        Thread.sleep(textMessageDelay)
        onView(withId(R.id.rv_chat))
                .check(matches(atPosition(0, hasDescendant(withText(AutoresponderProxy.RESPONSE_RECEIVED)))))
    }

    private fun checkDisconnectionByPartner() {
        onView(withId(R.id.et_message)).perform(typeText(AutoresponderProxy.COMMAND_DISCONNECT))
        onView(withId(R.id.ib_send)).perform(click())
        Thread.sleep(textMessageDelay)
        onView(withText(R.string.chat__partner_disconnected)).check(matches(isDisplayed()))
    }

    private fun checkNotConnected() {
        onView(withText(R.string.chat__not_connected_to_this_device))
                .check(matches(isDisplayed()))
        onView(withId(R.id.tb_toolbar)).check(matches(
                withToolbarSubTitle(context.getString(R.string.chat__not_connected))))
    }
}
