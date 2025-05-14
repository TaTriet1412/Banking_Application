package com.example.bankingapplication.DTO.Response;

import java.util.Map;

public class CardFrontRes {
    private Map<String, String> result;

    public CardFrontRes(Map<String, String> result) {
        this.result = result;
    }

    public Map<String, String> getResult() {
        return result;
    }

    public void setResult(Map<String, String> result) {
        this.result = result;
    }

    public CardFrontRes() {
    }
}
