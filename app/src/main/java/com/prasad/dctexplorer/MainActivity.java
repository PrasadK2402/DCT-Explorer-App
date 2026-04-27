package com.prasad.dctexplorer;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private static final int STORAGE_PERMISSION_CODE = 101;

    private ImageView ivOriginal, ivCompressed;
    private TextView tvQualityValue, tvSizeOriginal, tvSizeCompressed;
    private SeekBar seekBarQuality;
    private MaterialCardView cardUpload;
    private MaterialButton btnCompress, btnDownload;

    private Bitmap originalBitmap;
    private Bitmap compressedBitmap;
    private int quality = 80;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    loadOriginalImage(imageUri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupListeners();
    }

    private void initViews() {
        ivOriginal = findViewById(R.id.ivOriginal);
        ivCompressed = findViewById(R.id.ivCompressed);
        tvQualityValue = findViewById(R.id.tvQualityValue);
        tvSizeOriginal = findViewById(R.id.tvSizeOriginal);
        tvSizeCompressed = findViewById(R.id.tvSizeCompressed);
        seekBarQuality = findViewById(R.id.seekBarQuality);
        cardUpload = findViewById(R.id.cardUpload);
        btnCompress = findViewById(R.id.btnCompress);
        btnDownload = findViewById(R.id.btnDownload);

        // Set initial quality value
        tvQualityValue.setText(quality + "%");
        seekBarQuality.setProgress(quality);
    }

    private void setupListeners() {
        cardUpload.setOnClickListener(v -> checkPermissionAndOpenGallery());

        seekBarQuality.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                quality = progress;
                tvQualityValue.setText(quality + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnCompress.setOnClickListener(v -> {
            if (originalBitmap != null) {
                compressImage();
            } else {
                Toast.makeText(this, "Please upload an image first", Toast.LENGTH_SHORT).show();
            }
        });

        btnDownload.setOnClickListener(v -> {
            if (compressedBitmap != null) {
                saveImageToGallery(compressedBitmap);
            } else {
                Toast.makeText(this, "Please compress an image first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkPermissionAndOpenGallery() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{permission}, STORAGE_PERMISSION_CODE);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Permission denied. Cannot access gallery.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadOriginalImage(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                // Get original size
                long fileSize = inputStream.available();
                tvSizeOriginal.setText("Original size: " + (fileSize / 1024) + " KB");
                inputStream.close();
            }
            
            // Decode with scaling to prevent OOM
            originalBitmap = decodeSampledBitmapFromStream(uri, 800, 800);
            ivOriginal.setImageBitmap(originalBitmap);
            
            // Clear compressed view
            ivCompressed.setImageBitmap(null);
            compressedBitmap = null;
            tvSizeCompressed.setText("Compressed size: 0 KB");
            
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap decodeSampledBitmapFromStream(Uri uri, int reqWidth, int reqHeight) throws IOException {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        
        InputStream input = getContentResolver().openInputStream(uri);
        BitmapFactory.decodeStream(input, null, options);
        if (input != null) input.close();

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        options.inJustDecodeBounds = false;
        input = getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);
        if (input != null) input.close();
        
        return bitmap;
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private void compressImage() {
        if (originalBitmap == null) return;

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        originalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
        byte[] compressedBytes = outputStream.toByteArray();
        
        compressedBitmap = BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.length);
        
        ivCompressed.setImageBitmap(compressedBitmap);
        tvSizeCompressed.setText("Compressed size: " + (compressedBytes.length / 1024) + " KB");
        
        try {
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveImageToGallery(Bitmap bitmap) {
        OutputStream fos;
        String filename = "DCT_Compressed_" + System.currentTimeMillis() + ".jpg";

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/DCTExplorer");

                Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                fos = getContentResolver().openOutputStream(imageUri);
            } else {
                String imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
                java.io.File image = new java.io.File(imagesDir, filename);
                fos = new java.io.FileOutputStream(image);
            }

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            if (fos != null) {
                fos.close();
            }
            Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }
}