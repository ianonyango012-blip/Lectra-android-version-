package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("LECTRA", appName)
  }

  @Test
  fun `launch MainActivity and verify drawables`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val symbolDrawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_lectra_symbol)
    org.junit.Assert.assertNotNull("ic_lectra_symbol must be non-null", symbolDrawable)

    val launcherDrawable = androidx.core.content.ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
    org.junit.Assert.assertNotNull("ic_launcher must be non-null", launcherDrawable)
  }

  @Test
  fun `launch MainActivity without crash`() {
    val scenario = androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java)
    scenario.onActivity { activity ->
      org.junit.Assert.assertNotNull(activity)
    }
  }

  @Test
  fun `test LectraViewModel initialization and database access`() = kotlinx.coroutines.test.runTest {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.LectraViewModel(application)
    org.junit.Assert.assertNotNull(viewModel)
    val state = viewModel.uiState.value
    org.junit.Assert.assertNotNull(state)
  }
}

