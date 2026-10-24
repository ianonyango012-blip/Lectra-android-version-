package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.ui.AuthViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthViewModelTest {

    @Test
    fun `initial state has no error and is not loading`() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = AuthViewModel(application)

        val state = viewModel.authState.value
        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
        assertNull(state.errorMessage)
        assertNull(state.userDisplayName)
        assertNull(state.userEmail)
    }

    @Test
    fun `getSafeFirebaseAuth does not throw when Firebase is uninitialized`() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = AuthViewModel(application)

        // Must return either null or valid auth instance without throwing IllegalStateException
        val auth = viewModel.getSafeFirebaseAuth()
        // In headless Robolectric without google-services.json, this safely returns null
        // Crucially, it must NOT throw an uncaught exception.
    }

    @Test
    fun `getSafeCurrentUser does not throw when unauthenticated`() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = AuthViewModel(application)

        val user = viewModel.getSafeCurrentUser()
        assertNull(user)
    }

    @Test
    fun `signInWithGoogle with null activity sets readable error and does not crash`() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = AuthViewModel(application)

        var successCalled = false
        viewModel.signInWithGoogle(null) {
            successCalled = true
        }

        assertFalse(successCalled)
        val state = viewModel.authState.value
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("unavailable"))
    }

    @Test
    fun `signInWithGoogle with destroyed activity sets readable error and does not crash`() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = AuthViewModel(application)
        val activityController = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = activityController.get()
        activityController.destroy()

        var successCalled = false
        viewModel.signInWithGoogle(activity) {
            successCalled = true
        }

        assertFalse(successCalled)
        val state = viewModel.authState.value
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
    }

    @Test
    fun `signInWithGoogle when default_web_client_id is missing reports configuration error without crashing`() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = AuthViewModel(application)
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()

        var successCalled = false
        viewModel.signInWithGoogle(activity) {
            successCalled = true
        }

        assertFalse(successCalled)
        val state = viewModel.authState.value
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
        assertTrue(
            state.errorMessage!!.contains("default_web_client_id") ||
            state.errorMessage!!.contains("configuration")
        )
    }

    @Test
    fun `clearError resets the error message in auth state`() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = AuthViewModel(application)

        viewModel.signInWithGoogle(null) {}
        assertNotNull(viewModel.authState.value.errorMessage)

        viewModel.clearError()
        assertNull(viewModel.authState.value.errorMessage)
    }
}
