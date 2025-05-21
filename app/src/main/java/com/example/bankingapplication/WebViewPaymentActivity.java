package com.example.bankingapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
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
import android.widget.Toast;

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

            private WebViewClient webViewClient = new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    Log.d(TAG, "Loading URL: " + url);

                    // Check if the URL is the return URL from VNPAY
                    if (url.startsWith("myapp://vnpayresponse")) {
                        Log.d(TAG, "VNPay return URL detected: " + url);
                        
                        try {
                            // Parse the URL to extract the query parameters
                            Uri uri = Uri.parse(url);
                            
                            // Create an intent to pass the data to PaymentReturnActivity
                            Intent intent = new Intent(WebViewPaymentActivity.this, PaymentReturnActivity.class);
                            intent.setData(uri);
                            
                            // Start the activity
                            startActivity(intent);
                            
                            // Close this WebView activity
                            finish();
                            
                            // Return true to indicate we've handled this URL
                            return true;
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing VNPay return URL", e);
                            // If there's an error, let WebView handle it
                            return false;
                        }
                    }
                    
                    // Let WebView handle all other URLs
                    return false;
                }
                
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    Log.d(TAG, "Page finished loading: " + url);
                    
                    // Hide loading indicator when page is loaded
                    progressBar.setVisibility(View.GONE);
                }
                
                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    super.onReceivedError(view, errorCode, description, failingUrl);
                    Log.e(TAG, "Error loading page: " + description);
                    
                    // Show error message to user
                    Toast.makeText(WebViewPaymentActivity.this, 
                            "Lỗi kết nối: " + description, Toast.LENGTH_LONG).show();
                    
                    // Hide loading indicator
                    progressBar.setVisibility(View.GONE);
                }
            };
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
