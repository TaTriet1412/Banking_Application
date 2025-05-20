package com.example.bankingapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.bankingapplication.Utils.VnPayUtils;

import java.net.URISyntaxException;

public class WebViewPaymentActivity extends AppCompatActivity {

    private static final String TAG = "WebViewPaymentActivity";
    private WebView webView;
    private ProgressBar progressBar;
    private TextView txtLoading;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view_payment);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Thanh toán VNPay");
        }

        // Initialize views
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        txtLoading = findViewById(R.id.txtLoading);
        
        // Get payment URL from intent
        String paymentUrl = getIntent().getStringExtra("paymentUrl");
        if (paymentUrl == null || paymentUrl.isEmpty()) {
            Log.e(TAG, "No payment URL provided");
            finish();
            return;
        }
        
        // Configure WebView
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setSupportZoom(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        
        // Setup WebViewClient to handle page loading and URL redirects
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
                txtLoading.setVisibility(View.VISIBLE);
                Log.d(TAG, "Page loading started: " + url);
                
                // Check if the URL contains the return URL scheme
                if (url.startsWith(VnPayUtils.vnp_ReturnUrl)) {
                    try {
                        // Convert the URL to an Intent to launch PaymentReturnActivity
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        startActivity(intent);
                        finish(); // Close WebView after redirecting
                        return;
                    } catch (URISyntaxException e) {
                        Log.e(TAG, "Failed to parse return URL", e);
                    }
                }
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                txtLoading.setVisibility(View.GONE);
                Log.d(TAG, "Page loading finished: " + url);
                super.onPageFinished(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                Log.d(TAG, "shouldOverrideUrlLoading: " + url);
                
                // Handle VNPay return URL
                if (url.startsWith(VnPayUtils.vnp_ReturnUrl)) {
                    try {
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        startActivity(intent);
                        finish();
                        return true;
                    } catch (URISyntaxException e) {
                        Log.e(TAG, "Failed to parse return URL", e);
                    }
                }
                
                return super.shouldOverrideUrlLoading(view, request);
            }
        });
        
        // Load the payment URL
        Log.d(TAG, "Loading payment URL: " + paymentUrl);
        webView.loadUrl(paymentUrl);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
