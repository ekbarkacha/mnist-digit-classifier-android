package com.ek.digitrecognizer;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class ImageUtils {
    public static void saveBitmap(Context context, Bitmap bitmap, String fileName) {
        OutputStream fos;
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // Android 10+ (Scoped Storage)
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName + ".png");
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

                // Insert the image into the MediaStore
                fos = context.getContentResolver().openOutputStream(
                        context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values));
            } else {
                // Older versions (before Android 10)
                File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Digit Recognizer Images");
                if (!directory.exists()) {
                    directory.mkdirs();
                }
                File imageFile = new File(directory, fileName + ".png");
                fos = new FileOutputStream(imageFile);
            }

            if (fos != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();
                Toast.makeText(context, "Image saved successfully", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Error saving image: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}

