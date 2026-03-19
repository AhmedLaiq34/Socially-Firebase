package com.example.i230657

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.i230657.R
import org.hamcrest.Matcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(login_page::class.java)

    fun waitFor(millis: Long): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> = org.hamcrest.Matchers.any(View::class.java)
            override fun getDescription(): String = "Wait for $millis milliseconds."
            override fun perform(uiController: androidx.test.espresso.UiController, view: View?) {
                uiController.loopMainThreadForAtLeast(millis)
            }
        }
    }

    @Test
    fun full_functionality_test() {

        onView(withId(R.id.usernameField))
            .perform(typeText("ahmed"), ViewActions.closeSoftKeyboard())
        onView(isRoot()).perform(waitFor(1000)) // wait 1 second

        onView(withId(R.id.passwordField))
            .perform(typeText("123456"), ViewActions.closeSoftKeyboard())
        onView(isRoot()).perform(waitFor(1000)) // wait 1 second


        onView(withId(R.id.loginButton)).perform(click())
        onView(isRoot()).perform(waitFor(2000))

        onView(withId(R.id.loginButton)).perform(click())
        onView(isRoot()).perform(waitFor(2000))


        onView(withId(R.id.storyImage2)).perform(click())
        onView(isRoot()).perform(waitFor(2000))


        onView(withId(R.id.closeButton)).perform(click())
        onView(isRoot()).perform(waitFor(2000))


        onView(withId(R.id.iv_message)).perform(click())
        onView(isRoot()).perform(waitFor(2000))


        onView(withId(R.id.chat_item_1)).perform(click())
        onView(isRoot()).perform(waitFor(2000))


        onView(withId(R.id.btn_video_call)).perform(click())
        onView(isRoot()).perform(waitFor(2000))


        onView(withId(R.id.btnEndCall)).perform(click())
        onView(isRoot()).perform(waitFor(2000))


        onView(withId(R.id.btn_back)).perform(click())
        onView(isRoot()).perform(waitFor(2000))


        onView(withId(R.id.back_button)).perform(click())
        onView(isRoot()).perform(waitFor(2000))


        onView(withId(R.id.iv_camera)).perform(click())
        onView(isRoot()).perform(waitFor(2000))


        onView(withId(R.id.captureButton)).perform(click())
        onView(isRoot()).perform(waitFor(2000))


        onView(withId(R.id.your_stories)).perform(click())
        onView(isRoot()).perform(waitFor(2000))


        onView(withId(R.id.close)).perform(click())
        onView(isRoot()).perform(waitFor(2000))


        onView(withId(R.id.post1ProfileImage)).perform(click())
        onView(isRoot()).perform(waitFor(2000))


        onView(withId(R.id.follow)).perform(click())
        onView(isRoot()).perform(waitFor(2000))


        onView(withId(R.id.follow)).perform(click())
        onView(isRoot()).perform(waitFor(2000))


        onView(withId(R.id.back)).perform(click())
        onView(isRoot()).perform(waitFor(2000))


        onView(withId(R.id.bottomNavSearch)).perform(click())
        onView(isRoot()).perform(waitFor(2000))


        onView(withId(R.id.searchBarLayout)).perform(click())
        onView(isRoot()).perform(waitFor(2000))


        pressBack()
        onView(isRoot()).perform(waitFor(2000))


        onView(withId(R.id.bottomNavHome)).perform(click())
        onView(isRoot()).perform(waitFor(2000))


        onView(withId(R.id.bottomNavCreate)).perform(click())
        onView(isRoot()).perform(waitFor(2000))


        onView(withId(R.id.btn_cancel)).perform(click())
        onView(isRoot()).perform(waitFor(2000))


        onView(withId(R.id.bottomNavLikes)).perform(click())
        onView(isRoot()).perform(waitFor(2000))


        onView(withId(R.id.tab_you_text)).perform(click())
        onView(isRoot()).perform(waitFor(2000))


        onView(withId(R.id.tab_following_text)).perform(click())
        onView(isRoot()).perform(waitFor(2000))


        onView(withId(R.id.bottom_nav_home)).perform(click())
        onView(isRoot()).perform(waitFor(2000))


        onView(withId(R.id.bottomNavProfile)).perform(click())
        onView(isRoot()).perform(waitFor(2000))


        onView(withId(R.id.btn_edit_profile)).perform(click())
        onView(isRoot()).perform(waitFor(2000))


        onView(withId(R.id.cancel_text)).perform(click())
        onView(isRoot()).perform(waitFor(2000))


        onView(withId(R.id.highlight1)).perform(click())
        onView(isRoot()).perform(waitFor(2000))


        onView(withId(R.id.close)).perform(click())
        onView(isRoot()).perform(waitFor(2000))



    }



    @Test
    fun fillAllSignupFields() {
        onView(withId(R.id.signUpText)).perform(click())
        waitFor(2000)

        onView(withId(R.id.Username_input)).perform(typeText("ahmed123"), closeSoftKeyboard())
        waitFor(2000)

        onView(withId(R.id.FirstName_input)).perform(typeText("Ahmed"), closeSoftKeyboard())
        waitFor(2000)

        onView(withId(R.id.LastName_input)).perform(typeText("Khan"), closeSoftKeyboard())
        waitFor(2000)

        onView(withId(R.id.Dob_input)).perform(typeText("01/01/1990"), closeSoftKeyboard())
        waitFor(2000)

        onView(withId(R.id.Email_input)).perform(typeText("ahmed@example.com"), closeSoftKeyboard())
        waitFor(2000)

        onView(withId(R.id.Password_input)).perform(typeText("Password123"), closeSoftKeyboard())
        waitFor(2000)

        onView(withId(R.id.createAccountButton)).perform(click())
        waitFor(5000)
    }

}