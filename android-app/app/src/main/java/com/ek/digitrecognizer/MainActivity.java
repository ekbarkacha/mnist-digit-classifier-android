package com.ek.digitrecognizer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private DrawView drawView;
    private ImageView imageView;
    private Button btnClear, btnProcess,btnSave;
    private TextView textViewResult,textViewConfidence,textViewTime;
    private ProgressBar progressBar;
    private DigitClassifier digitClassifier;

    private Bitmap processedBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        drawView = findViewById(R.id.drawView);
        btnProcess = findViewById(R.id.btnProcess);
        btnClear = findViewById(R.id.btnClear);
        btnSave = findViewById(R.id.btnSave);
        imageView = findViewById(R.id.imageView);
        textViewResult = findViewById(R.id.textViewResult);
        textViewConfidence = findViewById(R.id.textViewConfidence);
        textViewTime = findViewById(R.id.textViewTime);
        progressBar = findViewById(R.id.progressBar);

        // Initialize TensorFlow Lite Model
        try {
            digitClassifier = new DigitClassifier(this);
        } catch (IOException e) {
            Toast.makeText(this, "Error loading model", Toast.LENGTH_SHORT).show();
            return;
        }


        // Process Image Button Click
        btnProcess.setOnClickListener(v -> classifyDigit());

        // Clear Canvas Button Click
        btnClear.setOnClickListener(v -> {
            drawView.clearCanvas();
            textViewResult.setText("Predicted Number: -");
            textViewConfidence.setText("Confidence: -%");
            textViewTime.setText("Time Cost: - ms");

            imageView.setImageBitmap(null);
            progressBar.setVisibility(View.GONE);
            //btnSave.setVisibility(View.GONE);
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (processedBitmap != null) {
                    String fileName = "IMG_" + System.currentTimeMillis();
                    ImageUtils.saveBitmap(MainActivity.this, processedBitmap, fileName);
                }
            }
        });
    }

    private void classifyDigit() {
        progressBar.setVisibility(View.VISIBLE);

        Bitmap bitmap = drawView.getBitmap();
        //processedBitmap = Bitmap.createScaledBitmap(bitmap, 28, 28, true);

        processedBitmap = centerAndResizeBitmap(bitmap);

        imageView.setImageBitmap(processedBitmap);

        new Thread(() -> {
            DigitClassifier.Prediction prediction = digitClassifier.classify(processedBitmap);

            runOnUiThread(() -> {
                textViewResult.setText("Predicted Number: "+ prediction.digit);
                textViewConfidence.setText("Confidence: "+String.format("%.2f", prediction.confidence) + "%");
                textViewTime.setText("Time Cost: "+ prediction.timeTaken + " ms");
                progressBar.setVisibility(View.GONE);
            });
        }).start();

        //btnSave.setVisibility(View.VISIBLE);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (digitClassifier != null) {
            digitClassifier.close();
        }
    }




    public Bitmap preprocessBitmap(Bitmap originalBitmap) {
        // Ensure the bitmap has a white background and black digit
        Bitmap scaledBitmap = Bitmap.createBitmap(28, 28, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(scaledBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setFilterBitmap(true); // Enable filtering to improve quality
        paint.setDither(true);

        // Resize the image while keeping quality
        canvas.drawBitmap(Bitmap.createScaledBitmap(originalBitmap, 28, 28, true), 0, 0, paint);

        return scaledBitmap;
    }

    public Bitmap centerAndResizeBitmap(Bitmap originalBitmap) {
        int originalWidth = originalBitmap.getWidth();
        int originalHeight = originalBitmap.getHeight();

        // Find the larger dimension
        int maxSize = Math.max(originalWidth, originalHeight);

        // Create a square bitmap with the max size
        Bitmap squaredBitmap = Bitmap.createBitmap(maxSize, maxSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(squaredBitmap);
        canvas.drawColor(Color.WHITE); // Set background to white

        // Center the digit
        float left = (maxSize - originalWidth) / 2.0f;
        float top = (maxSize - originalHeight) / 2.0f;
        canvas.drawBitmap(originalBitmap, left, top, null);

        // Now resize while keeping the digit centered
        return preprocessBitmap(squaredBitmap);
    }


}