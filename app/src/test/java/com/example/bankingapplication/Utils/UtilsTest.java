package com.example.bankingapplication.Utils;

import org.junit.Test;
import static org.junit.Assert.*;

public class UtilsTest {

    @Test
    public void testNumberFormatCurrency() {
        // Test currency formatting
        int amount = 1000000;
        String expected = "1.000.000 VNĐ";
        String actual = NumberFormat.convertToCurrencyFormatHasUnit(amount);
        assertEquals(expected, actual);
    }
    
    @Test
    public void testNumberToWords() {
        // Test converting number to Vietnamese words
        int number = 1000000;
        String result = NumberToWordsConverter.convertNumberToVietnameseWords(number);
        assertEquals("Một triệu", result);
    }
    
    @Test
    public void testNumberToWordsZero() {
        // Test edge case - zero
        int number = 0;
        String result = NumberToWordsConverter.convertNumberToVietnameseWords(number);
        assertEquals("Không", result);
    }
    
    @Test
    public void testGlobalVariablesSingleton() {
        // Test that GlobalVariables is indeed a singleton
        GlobalVariables instance1 = GlobalVariables.getInstance();
        GlobalVariables instance2 = GlobalVariables.getInstance();
        assertSame("GlobalVariables should be a singleton", instance1, instance2);
    }
    
    @Test
    public void testCheckingAccountBalance() {
        // Test CheckingAccount class
        CheckingAccount account = new CheckingAccount(5000000);
        assertEquals(Integer.valueOf(5000000), account.getBalance());
        
        // Test setting new balance
        account.setBalance(6000000);
        assertEquals(Integer.valueOf(6000000), account.getBalance());
    }
}
