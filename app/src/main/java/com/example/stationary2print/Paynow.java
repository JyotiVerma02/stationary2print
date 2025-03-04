package com.example.stationary2print;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Paynow extends AppCompatActivity {

    private static final int UPI_PAYMENT_REQUEST = 100;
    private static final String UPI_ID = "8708068091@ybl";
    private static final String PAYEE_NAME = "Payment Service";
    private double totalAmount;

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pay_now);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI components
        TextView tvColorPages = findViewById(R.id.tvColorPages);
        TextView tvBWPages = findViewById(R.id.tvBWPages);
        TextView tvBinding = findViewById(R.id.tvBinding);
        TextView tvTotalAmount = findViewById(R.id.tvTotalAmount);
        Button upiButton = findViewById(R.id.upiPaymentButton);

        // Get data from Intent
        int colorPages = getIntent().getIntExtra("colorPages", 0);
        int bwPages = getIntent().getIntExtra("bwPages", 0);
        boolean isBinding = getIntent().getBooleanExtra("isBinding", false);
        totalAmount = getIntent().getDoubleExtra("totalAmount", 0.0);

        // Display order details
        tvColorPages.setText("Color Pages: " + colorPages);
        tvBWPages.setText("B/W Pages: " + bwPages);
        tvBinding.setText("Binding: " + (isBinding ? "Yes" : "No"));
        tvTotalAmount.setText("Total Amount: ₹" + String.format("%.2f", totalAmount));

        // UPI Payment Button
        upiButton.setOnClickListener(view -> initiateUPIPayment(UPI_ID, PAYEE_NAME, String.valueOf(totalAmount)));
    }

    @SuppressLint("QueryPermissionsNeeded")
    private void initiateUPIPayment(String upiID, String name, String amount) {
        Uri uri = Uri.parse("upi://pay?pa=" + upiID + "&pn=" + name + "&tn=Photostat Payment&am=" + amount + "&cu=INR");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);

        try {
            startActivityForResult(intent, UPI_PAYMENT_REQUEST);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No UPI-supported app found. Please install Google Pay, PhonePe, or Paytm.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == UPI_PAYMENT_REQUEST) {
            if (data != null && data.getStringExtra("response") != null) {
                handleUPIResponse(data.getStringExtra("response"));
            } else {
                Toast.makeText(this, "Payment was canceled or failed.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void handleUPIResponse(String response) {
        if (response.toLowerCase().contains("status=success")) {
            Toast.makeText(this, "✅ Payment Successful!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "❌ Payment Failed. Try again.", Toast.LENGTH_LONG).show();
        }
    }
}
