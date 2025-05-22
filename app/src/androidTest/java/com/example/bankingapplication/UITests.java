package com.example.bankingapplication;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.containsString;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class UITests {

    // Thông tin đăng nhập
    private static final String OFFICER_EMAIL = "tatriet16@gmail.com";
    private static final String USER_EMAIL = "triettrinhthinh@gmail.com";
    private static final String PASSWORD = "123456";

    @Rule
    public ActivityScenarioRule<SignInActivity> activityScenarioRule = 
            new ActivityScenarioRule<>(SignInActivity.class);
    
    @Before
    public void setUp() {
        // Chuẩn bị trước khi chạy mỗi test case
    }
    
    // 1. Test đăng nhập với tài khoản người dùng
    @Test
    public void testUserLogin() {
        // Nhập email và mật khẩu - Sửa ID thành txtEmail, txtPass cho đúng với layout
        onView(withId(R.id.txtEmail))
            .perform(typeText(USER_EMAIL), closeSoftKeyboard());
        
        onView(withId(R.id.txtPass))
            .perform(typeText(PASSWORD), closeSoftKeyboard());
        
        // Nhấn nút đăng nhập
        onView(withId(R.id.btnSignIn)).perform(click());
        
        // Kiểm tra đã vào màn hình chính (chờ màn hình load)
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Kiểm tra phần tử trong dashboard hiển thị - Thay bằng một phần tử chắc chắn có trong màn hình chính
        onView(withId(R.id.tv_user_name)).check(matches(isDisplayed()));
    }
    


    // 8. Test chức năng xem lịch sử giao dịch
    @Test
    public void testViewTransactionHistory() {
        // Đăng nhập
        loginAsUser();
        
        // Chọn xem lịch sử giao dịch
        onView(withText("Lịch sử giao dịch")).perform(click());
        
        // Kiểm tra màn hình lịch sử giao dịch hiển thị
        onView(withId(R.id.toolbar_transaction_history)).check(matches(isDisplayed()));
    }

    // 10. Test chức năng xem danh sách khách hàng (nhân viên)
    @Test
    public void testViewCustomerList() {
        // Đăng nhập với tài khoản nhân viên
        loginAsOfficer();
        

        // Kiểm tra màn hình danh sách khách hàng hiển thị
        onView(withId(R.id.search_view_customers)).check(matches(isDisplayed()));
    }
    
    // 11. Test chức năng tìm kiếm khách hàng (nhân viên)
    @Test
    public void testSearchCustomer() {
        // Đăng nhập với tài khoản nhân viên
        loginAsOfficer();
        
        // Nhập từ khóa tìm kiếm
        onView(withId(R.id.search_view_customers)).perform(click());
        onView(withId(androidx.appcompat.R.id.search_src_text))
            .perform(typeText("Triet"), closeSoftKeyboard());
        
        // Kiểm tra kết quả tìm kiếm hiển thị
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        onView(withId(R.id.recyclerViewUsers)).check(matches(isDisplayed()));
    }
    

    
    // 13. Test chức năng xem lịch sử tiết kiệm
    @Test
    public void testViewSavingHistory() {
        // Đăng nhập
        loginAsUser();

        onView(withText("Tài khoản")).perform(click());
        
        // Chọn mục tài khoản tiết kiệm
        onView(withText("Tiết kiệm")).perform(click());

        // Nhấn nút xem lịch sử tiết kiệm
        onView(withId(R.id.iv_view_interest_history)).perform(click());
        
        // Kiểm tra dialog lịch sử tiết kiệm hiển thị
        onView(withText("Lịch sử tiết kiệm")).check(matches(isDisplayed()));
    }
    


    // Helper methods
    
    private void loginAsUser() {
        onView(withId(R.id.txtEmail))
            .perform(typeText(USER_EMAIL), closeSoftKeyboard());
        
        onView(withId(R.id.txtPass))
            .perform(typeText(PASSWORD), closeSoftKeyboard());
        
        onView(withId(R.id.btnSignIn)).perform(click());
        
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    private void loginAsOfficer() {
        onView(withId(R.id.txtEmail))
            .perform(typeText(OFFICER_EMAIL), closeSoftKeyboard());
        
        onView(withId(R.id.txtPass))
            .perform(typeText(PASSWORD), closeSoftKeyboard());
        
        onView(withId(R.id.btnSignIn)).perform(click());
        
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    

}
