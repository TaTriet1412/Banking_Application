package com.example.bankingapplication;

import com.example.bankingapplication.Utils.NumberFormat;
import com.example.bankingapplication.Utils.NumberToWordsConverter;
import com.example.bankingapplication.Utils.VnPayUtils;
import com.example.bankingapplication.Object.Account;
import com.example.bankingapplication.Object.TransactionData;
import com.example.bankingapplication.Object.User;
import com.example.bankingapplication.Utils.GlobalVariables;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class UtilsTest {

    @Mock
    Account mockAccount;
    
    @Mock 
    User mockUser;

    @Before
    public void setUp() {
        // Any setup code that needs to run before each test
    }

    @Test
    public void testNumberFormatCurrencyWithUnit() {
        // Test formatting an amount with currency unit (VNĐ)
        int amount = 1000000;
        String formattedAmount = NumberFormat.convertToCurrencyFormatHasUnit(amount);
        assertEquals("1.000.000 VNĐ", formattedAmount);
    }

    @Test
    public void testNumberFormatCurrencyOnlyNumber() {
        // Test formatting an amount without currency unit
        int amount = 1500000;
        String formattedAmount = NumberFormat.convertToCurrencyFormatOnlyNumber(amount);
        assertEquals("1.500.000", formattedAmount);
    }

    @Test
    public void testNumberToWordsConverter() {
        // Test converting number to Vietnamese words
        int number = 1234567;
        String words = NumberToWordsConverter.convertNumberToVietnameseWords(number);
        assertEquals("Một triệu hai trăm ba mươi tư nghìn năm trăm sáu mươi bảy", words);
    }
    
    @Test
    public void testNumberToWordsConverterWithZero() {
        // Test converting zero to Vietnamese words
        int number = 0;
        String words = NumberToWordsConverter.convertNumberToVietnameseWords(number);
        assertEquals("Không", words);
    }

    @Test
    public void testGlobalVariablesSingleton() {
        // Test the singleton pattern of GlobalVariables
        GlobalVariables instance1 = GlobalVariables.getInstance();
        GlobalVariables instance2 = GlobalVariables.getInstance();
        
        // Both instances should point to the same object
        assertNotNull(instance1);
        assertNotNull(instance2);
        assertEquals(instance1, instance2);
    }
    
    @Test
    public void testGlobalVariablesSetCurrentUser() {
        // Test setting and getting current user in GlobalVariables
        GlobalVariables.getInstance().setCurrentUser(mockUser);
        User retrievedUser = GlobalVariables.getInstance().getCurrentUser();
        assertEquals(mockUser, retrievedUser);
    }
    
    @Test
    public void testGlobalVariablesSetCurrentAccount() {
        // Test setting and getting current account in GlobalVariables
        GlobalVariables.getInstance().setCurrentAccount(mockAccount);
        Account retrievedAccount = GlobalVariables.getInstance().getCurrentAccount();
        assertEquals(mockAccount, retrievedAccount);
    }
    
    @Test
    public void testVnPayHmacSHA512() {
        // Test HMAC SHA-512 implementation
        String key = "SECRETKEY";
        String data = "testdata";
        String result = VnPayUtils.hmacSHA512(key, data);
        
        // Verify the result is not empty and has proper hash length (128 hex chars)
        assertNotNull(result);
        assertTrue(result.length() > 0);
        assertEquals(128, result.length());
    }

    @Test
    public void testAccountObjectGetterSetter() {
        // Test proper functioning of Account object getters and setters
        Account account = new Account();
        
        String uid = "ACC123456";
        String accountNumber = "0123456789";
        
        account.setUID(uid);
        account.setAccountNumber(accountNumber);
        
        assertEquals(uid, account.getUID());
        assertEquals(accountNumber, account.getAccountNumber());
    }

    @Test
    public void testTransactionDataObjectCreation() {
        // Test transaction data object creation and initialization
        String uid = "TRX123";
        int amount = 500000;
        String type = "transfer";
        String status = "pending";
        String description = "Test transaction";
        
        TransactionData transaction = new TransactionData();
        transaction.setUID(uid);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setStatus(status);
        transaction.setDescription(description);

        assertEquals(uid, transaction.getUID());
        assertEquals(amount, transaction.getAmount().intValue());
        assertEquals(type, transaction.getType());
        assertEquals(status, transaction.getStatus());
        assertEquals(description, transaction.getDescription());
    }
}
