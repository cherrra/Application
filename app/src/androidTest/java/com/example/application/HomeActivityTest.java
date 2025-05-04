package com.example.application;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.not;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import com.example.application.ui.view.HomeActivity;
import com.example.application.utils.EncryptedSharedPrefs;
import com.example.application.network.ApiClient;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.security.GeneralSecurityException;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class HomeActivityTest {

    @Rule
    public ActivityTestRule<HomeActivity> activityRule =
            new ActivityTestRule<>(HomeActivity.class, true, false);


    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        try {
            // Инициализируем ApiClient перед его использованием
            ApiClient.init(context);

            // Инициализация EncryptedSharedPrefs и сохранение токенов
            EncryptedSharedPrefs sharedPrefs = new EncryptedSharedPrefs(context);
            sharedPrefs.saveTokens("fakeToken", "fakeRefreshToken");

        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Ошибка при инициализации", e);
        }
    }


    @Test
    public void testToastWhenNoToken() {
        Context context = ApplicationProvider.getApplicationContext();
        try {
            EncryptedSharedPrefs sharedPrefs = new EncryptedSharedPrefs(context);
            sharedPrefs.clearTokens();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Ошибка при очистке токенов", e);
        }

        activityRule.launchActivity(new Intent());

        onView(withText("Необходима авторизация"))
                .inRoot(withDecorView(not(activityRule.getActivity().getWindow().getDecorView())))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testMenuButtonsAreClickable() {
        activityRule.launchActivity(new Intent());
        onView(withId(R.id.accountButton)).perform(click());
        onView(withId(R.id.garageButton)).perform(click());
        onView(withId(R.id.orderButton)).perform(click());
    }

}
