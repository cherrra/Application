//package com.example.application;
//
//import static androidx.test.espresso.Espresso.onView;
//import static androidx.test.espresso.action.ViewActions.click;
//import static androidx.test.espresso.action.ViewActions.typeText;
//import static androidx.test.espresso.assertion.ViewAssertions.matches;
//import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
//import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
//import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
//import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
//import static androidx.test.espresso.matcher.ViewMatchers.isNotChecked;
//import static androidx.test.espresso.matcher.ViewMatchers.withId;
//import static androidx.test.espresso.matcher.ViewMatchers.withText;
//
//import static org.hamcrest.CoreMatchers.is;
//import static org.hamcrest.Matchers.not;
//
//import android.content.Context;
//
//import androidx.test.core.app.ActivityScenario;
//import androidx.test.core.app.ApplicationProvider;
//import androidx.test.ext.junit.runners.AndroidJUnit4;
//import androidx.test.filters.LargeTest;
//
//import com.example.application.network.ApiClient;
//import com.example.application.ui.view.RegisterActivity;
//import com.example.application.utils.EncryptedSharedPrefs;
//
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//
//import java.io.IOException;
//import java.security.GeneralSecurityException;
//
//@LargeTest
//@RunWith(AndroidJUnit4.class)
//public class RegisterActivityTest {
//
//    private static boolean isApiClientInitialized = false;
//
//    @Before
//    public void setUp() {
//        if (!isApiClientInitialized) {
//            // Инициализация ApiClient только один раз
//            Context context = ApplicationProvider.getApplicationContext();
//            try {
//                // Инициализация ApiClient перед запуском активности
//                ApiClient.init(context);
//                isApiClientInitialized = true;  // Устанавливаем флаг, что инициализация произошла
//
//                // Инициализация EncryptedSharedPrefs и сохранение токенов
//                EncryptedSharedPrefs sharedPrefs = new EncryptedSharedPrefs(context);
//                sharedPrefs.saveTokens("fakeToken", "fakeRefreshToken");
//
//            } catch (GeneralSecurityException | IOException e) {
//                throw new RuntimeException("Ошибка при инициализации", e);
//            }
//        }
//    }
//
//    @Before
//    public void launchActivity() {
//        // Запуск активности перед выполнением теста
//        ActivityScenario.launch(RegisterActivity.class);
//    }
//
//    @Test
//    public void testRegisterButtonInitiallyDisabled() {
//        // Проверка, что кнопка регистрации изначально заблокирована
//        onView(withId(R.id.registerButton)).check(matches(not(isEnabled())));
//    }
//
//    @Test
//    public void testRegisterButtonDisabledWhenCheckboxUnchecked() {
//        // Убедитесь, что чекбокс не выбран
//        onView(withId(R.id.privacyPolicyCheckBox))
//                .check(matches(isNotChecked()));
//
//        // Проверка, что кнопка "Регистрация" отключена
//        onView(withId(R.id.registerButton))
//                .check(matches(not(isEnabled())));
//
//        // Клик по чекбоксу
//        onView(withId(R.id.privacyPolicyCheckBox))
//                .perform(click());
//
//        // Проверка, что чекбокс теперь выбран
//        onView(withId(R.id.privacyPolicyCheckBox))
//                .check(matches(isChecked()));
//
//        // Проверка, что кнопка "Регистрация" теперь активна
//        onView(withId(R.id.registerButton))
//                .check(matches(isEnabled()));
//    }
//
//    @Test
//    public void testRegisterButtonDisabledWhenInvalidEmail() {
//        // Вводим некорректный email и оставляем чекбокс не отмеченным
//        onView(withId(R.id.usernameEditText)).perform(typeText("testuser"));
//        onView(withId(R.id.emailEditText)).perform(typeText("invalid-email"));
//        onView(withId(R.id.passwordEditText)).perform(typeText("password123"));
//        onView(withId(R.id.privacyPolicyCheckBox)).perform(click());
//
//        // Проверка, что кнопка регистрации заблокирована из-за неправильного email
//        onView(withId(R.id.registerButton)).check(matches(not(isEnabled())));
//    }
//
//    @Test
//    public void testRegisterButtonDisabledWhenShortPassword() {
//        // Вводим короткий пароль и оставляем чекбокс не отмеченным
//        onView(withId(R.id.usernameEditText)).perform(typeText("testuser"));
//        onView(withId(R.id.emailEditText)).perform(typeText("test@domain.com"));
//        onView(withId(R.id.passwordEditText)).perform(typeText("short"));
//        onView(withId(R.id.privacyPolicyCheckBox)).perform(click());
//
//        // Проверка, что кнопка регистрации заблокирована из-за короткого пароля
//        onView(withId(R.id.registerButton)).check(matches(not(isEnabled())));
//    }
//
//    @Test
//    public void testRegisterButtonEnabledWithValidInput() {
//        // Вводим правильные данные
//        onView(withId(R.id.usernameEditText)).perform(typeText("testuser"));
//        onView(withId(R.id.emailEditText)).perform(typeText("test@domain.com"));
//        onView(withId(R.id.passwordEditText)).perform(typeText("password123"));
//
//        // Устанавливаем чекбокс
//        onView(withId(R.id.privacyPolicyCheckBox)).perform(click());
//
//        // Проверка, что кнопка регистрации теперь активна
//        onView(withId(R.id.registerButton)).check(matches(isEnabled()));
//    }
//
//    @Test
//    public void testRegisterButtonClick() {
//        // Используем ActivityScenario для запуска активности
//        ActivityScenario<RegisterActivity> activityScenario = ActivityScenario.launch(RegisterActivity.class);
//
//        // Вводим правильные данные
//        onView(withId(R.id.usernameEditText)).perform(typeText("testuser"));
//        onView(withId(R.id.emailEditText)).perform(typeText("test@domain.com"));
//        onView(withId(R.id.passwordEditText)).perform(typeText("password123"));
//        onView(withId(R.id.privacyPolicyCheckBox)).perform(click());
//
//        // Нажимаем на кнопку регистрации
//        onView(withId(R.id.registerButton)).perform(click());
//
//        // Ожидаем появления Toast с успешной регистрацией
//        activityScenario.onActivity(activity -> {
//            onView(withText("Регистрация успешна"))
//                    .inRoot(withDecorView(not(is(activity.getWindow().getDecorView()))))
//                    .check(matches(isDisplayed()));
//        });
//    }
//}
