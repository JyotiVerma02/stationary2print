package com.example.stationary2print;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.database.Cursor;
import android.provider.OpenableColumns;
import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private EditText etColorPages, etBWPages;
    private CheckBox cbBinding;
    private TextView tvSubtotal, tvSelectedFile;
    private Button btnNextStep;
    private Uri selectedFileUri;
    private double totalAmount = 0.0;
    private String selectedFileName = "";
    private FirebaseFirestore db;

    @SuppressLint("SetTextI18n")
    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedFileUri = result.getData().getData();
                    if (selectedFileUri != null) {
                        selectedFileName = getFileName(selectedFileUri);
                        tvSelectedFile.setText("Selected File: " + selectedFileName);
                        Toast.makeText(this, "File Selected Successfully!", Toast.LENGTH_SHORT).show();
                        checkNextButtonState();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        etColorPages = findViewById(R.id.etColorPages);
        etBWPages = findViewById(R.id.etBWPages);
        cbBinding = findViewById(R.id.cbBinding);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvSelectedFile = findViewById(R.id.tvSelectedFile);
        Button btnSelectFile = findViewById(R.id.btnSelectFile);
        btnNextStep = findViewById(R.id.btnNextStep);

        btnNextStep.setEnabled(false);

        // TextWatchers to update subtotal automatically
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateTotal();
                checkNextButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etColorPages.addTextChangedListener(textWatcher);
        etBWPages.addTextChangedListener(textWatcher);
        cbBinding.setOnCheckedChangeListener((buttonView, isChecked) -> {
            calculateTotal();
            checkNextButtonState();
        });

        btnSelectFile.setOnClickListener(v -> openFilePicker());

        btnNextStep.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Paynow.class);
            intent.putExtra("colorPages", getIntFromEditText(etColorPages));
            intent.putExtra("bwPages", getIntFromEditText(etBWPages));
            intent.putExtra("isBinding", cbBinding.isChecked());
            intent.putExtra("totalAmount", totalAmount);

            if (selectedFileUri != null) {
                intent.putExtra("selectedFileUri", selectedFileUri.toString());
                intent.putExtra("selectedFileName", selectedFileName);
            }

            startActivity(intent);
            // Save data to Firestore when the "Next Step" button is clicked
            saveDataToFirestore();
        });
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimeTypes = {"image/png", "image/jpeg", "application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        filePickerLauncher.launch(intent);
    }

    private String getFileName(Uri uri) {
        String fileName = "Unknown File";

        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            }
        } else if (uri.getPath() != null) {
            int cut = uri.getPath().lastIndexOf('/');
            if (cut != -1) {
                fileName = uri.getPath().substring(cut + 1);
            }
        }

        return fileName;
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    private void calculateTotal() {
        int colorPages = getIntFromEditText(etColorPages);
        int bwPages = getIntFromEditText(etBWPages);
        boolean isBinding = cbBinding.isChecked();

        double BW_PRICE = 0.50;
        double COLOR_PRICE = 1.0;
        totalAmount = (colorPages * COLOR_PRICE) + (bwPages * BW_PRICE);
        if (isBinding) {
            double BINDING_PRICE = 50.0;
            totalAmount += BINDING_PRICE;
        }

        tvSubtotal.setText("Subtotal: ₹" + String.format("%.2f", totalAmount));
    }

    private int getIntFromEditText(EditText editText) {
        String value = editText.getText().toString();
        return value.isEmpty() ? 0 : Integer.parseInt(value);
    }

    private void checkNextButtonState() {
        int colorPages = getIntFromEditText(etColorPages);
        int bwPages = getIntFromEditText(etBWPages);

        btnNextStep.setEnabled((colorPages > 0 || bwPages > 0) && selectedFileUri != null);
    }
    private void saveDataToFirestore() {
    DocumentReference counterRef = db.collection("counters").document("orders_counter");
    counterRef.get().addOnSuccessListener(documentSnapshot -> {
        final long[] nextId = {1}; // Using an array to keep it effectively final
        Long countValue = documentSnapshot.getLong("count");
        nextId[0] = (countValue != null) ? countValue + 1 : 1; // Prevent NullPointerException

        // Prepare data to be saved
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("image_url", selectedFileUri != null ? selectedFileUri.toString() : ""); // Store the file URI as a string
        orderData.put("color_pages", getIntFromEditText(etColorPages));
        orderData.put("bw_pages", getIntFromEditText(etBWPages));
        orderData.put("isBinding", cbBinding.isChecked());
        orderData.put("sub_total", totalAmount);

        // Add a new document with the incremented ID
        db.collection("stationery_files").document(String.valueOf(nextId[0]))
                .set(orderData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Document added with ID: " + nextId[0]);
                    Toast.makeText(MainActivity.this, "Order data saved successfully!", Toast.LENGTH_SHORT).show();

                    // Update the counter
                    Map<String, Object> counterUpdate = new HashMap<>();
                    counterUpdate.put("count", nextId[0]);
                    counterRef.set(counterUpdate, SetOptions.merge())
                            .addOnFailureListener(e -> Log.w("Firestore", "Failed to update counter", e));
                })
                .addOnFailureListener(e -> {
                    Log.w("Firestore", "Error adding document", e);
                    Toast.makeText(MainActivity.this, "Failed to save order data.", Toast.LENGTH_SHORT).show();
                });
    }).addOnFailureListener(e -> {
        Log.w("Firestore", "Failed to get counter document", e);
        Toast.makeText(MainActivity.this, "Failed to get counter document.", Toast.LENGTH_SHORT).show();
    });
    }
}