package com.example.application;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import com.example.application.network.ApiClient;
import com.example.application.ui.view.admin.HomeAdminActivity;
import com.example.application.utils.EncryptedSharedPrefs;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.security.GeneralSecurityException;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class HomeAdminActivityTest {

    @Rule
    public ActivityTestRule<HomeAdminActivity> activityRule =
            new ActivityTestRule<>(HomeAdminActivity.class, true, false);

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        try {
            ApiClient.init(context);
            EncryptedSharedPrefs sharedPrefs = new EncryptedSharedPrefs(context);
            sharedPrefs.saveTokens("fakeAdminToken", "fakeAdminRefreshToken");
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

        // Ждем появления Toast
        onView(withText("Необходима авторизация"))
                .inRoot(withDecorView(not(is(activityRule.getActivity().getWindow().getDecorView()))))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testNavigationButtonsAreClickable() {
        activityRule.launchActivity(new Intent());

        // Ждем загрузки активности
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Проверяем только элементы, которые точно есть в иерархии
        onView(withId(R.id.usersButton)).perform(click());
        onView(withId(R.id.orderButton)).perform(click());
        onView(withId(R.id.homeButton)).perform(click());
        onView(withId(R.id.logoutButton)).perform(click());
    }

    @Test
    public void testAddCategoryButtonShowsDialog() {
        activityRule.launchActivity(new Intent());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(withId(R.id.addCategoryButton)).perform(click());

        onView(withText("Добавить новую категорию"))
                .inRoot(isDialog())
                .check(matches(isDisplayed()));
    }
}