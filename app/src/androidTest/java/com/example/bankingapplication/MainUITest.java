package com.example.bankingapplication;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class MainUITest {

    // Note: These tests should be run with a logged-in emulator to succeed
    // The tests assume certain UI elements are present and may fail if the UI changes

    @Rule
    public ActivityScenarioRule<SignInActivity> activityScenarioRule = 
            new ActivityScenarioRule<>(SignInActivity.class);
            
    @Test
    public void testSignInScreenLoadsCorrectly() {
        // Kiểm tra các thành phần chính của màn hình đăng nhập hiển thị đúng
        onView(withId(R.id.txtEmail)).check(matches(isDisplayed()));
        onView(withId(R.id.txtPass)).check(matches(isDisplayed()));
        onView(withId(R.id.btnSignIn)).check(matches(isDisplayed()));
        onView(withId(R.id.btnSignUp)).check(matches(isDisplayed()));
    }
    
    @Test
    public void testSignInButtonDisplaysCorrectText() {
        // Kiểm tra nút đăng nhập có hiển thị đúng text không
        onView(withId(R.id.btnSignIn)).check(matches(withText("Đăng nhập")));
    }
    
    @Test
    public void testSignUpButtonDisplaysCorrectText() {
        // Kiểm tra nút đăng ký có hiển thị đúng text không
        onView(withId(R.id.btnSignUp)).check(matches(withText("Đăng ký")));
    }
    
    @Test
    public void testLabelErrorIsInitiallyInvisible() {
        // Kiểm tra thông báo lỗi ban đầu không hiển thị
        onView(withId(R.id.lblError)).check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.INVISIBLE)));
    }
    
    @Test
    public void testBankingAppLogoIsDisplayed() {
        // Kiểm tra logo của ứng dụng hiển thị đúng
        onView(withId(R.id.logoText)).check(matches(isDisplayed()));
        onView(withId(R.id.logoText)).check(matches(withText("3T Banking")));
    }
}
