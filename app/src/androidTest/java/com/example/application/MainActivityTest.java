package com.example.application;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import com.example.application.ui.view.LoginActivity;
import com.example.application.ui.view.MainActivity;
import com.example.application.ui.view.RegisterActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Rule
    public ActivityTestRule<MainActivity> activityRule =
            new ActivityTestRule<>(MainActivity.class);

    @Test
    public void testButtonsDisplayed() {
        onView(withId(R.id.button_login)).check(matches(isDisplayed()));
        onView(withId(R.id.button_register)).check(matches(isDisplayed()));
    }

    @Test
    public void testLoginButtonOpensLoginActivity() {
        Intents.init();
        onView(withId(R.id.button_login)).perform(click());
        intended(hasComponent(LoginActivity.class.getName()));
        Intents.release();
    }

    @Test
    public void testRegisterButtonOpensRegisterActivity() {
        Intents.init();
        onView(withId(R.id.button_register)).perform(click());
        intended(hasComponent(RegisterActivity.class.getName()));
        Intents.release();
    }

}