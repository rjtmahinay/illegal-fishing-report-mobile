package com.karagathon.vesselreporting.report;

import android.Manifest;
import android.content.ClipData;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.karagathon.vesselreporting.R;
import com.karagathon.vesselreporting.constant.FileType;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ReportActivity extends AppCompatActivity {
    private static final int MEDIA_REQUEST_CODE = 1;
    private static final int GALLERY_REQUEST_CODE = 2;
    private static final String DETAILS_URL = "http://192.168.0.109:1331/reportDetails";
    private int flag;
    private String absoluteFilePath;
    private Button photoCaptureButton, videoCaptureButton, submitButton, galleryButton;
    private LocalDateTime dateTime = LocalDateTime.now();
    private DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private List<String> galleryDataPaths;
    private String gallerySingleDataPath;

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static String getPathFromUri(final Context context, final Uri uri) {


        if (isExternalStorageDocument(uri)) {
            Log.i("Inside External Storage", "Inside External Storage");
            final String docId = DocumentsContract.getDocumentId(uri);
            final String[] split = docId.split(":");
            final String type = split[0];

            if ("primary".equalsIgnoreCase(type)) {
                return Environment.getExternalStorageDirectory() + "/" + split[1];
            }
        } else if (isDownloadsDocument(uri)) {
            Log.i("Inside Download Storage", "Inside Download Storage");
            final String id = DocumentsContract.getDocumentId(uri);
            final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));

            return getDataColumn(context, contentUri, null, null);
        } else if (isMediaDocument(uri)) {
            Log.i("Inside Media ", "Inside Media ");
            final String docId = DocumentsContract.getDocumentId(uri);
            final String[] split = docId.split(":");
            final String type = split[0];

            Uri contentUri = null;
            if ("image".equals(type)) {
                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            } else if ("video".equals(type)) {
                contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            } else if ("audio".equals(type)) {
                contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            }

            final String selection = "_id=?";
            final String[] selectionArgs = new String[]{
                    split[1]
            };
            return getDataColumn(context, contentUri, selection, selectionArgs);
        }

        // Media Store
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            Log.i("Inside Media Store", "Inside Media Store");
            return getDataColumn(context, uri, null, null);
        }
        //File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            Log.i("Inside File", "Inside File");
            return uri.getPath();
        }
        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    private void askCameraPermission() {
        String[] askPermissions = {Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

        if (ContextCompat.checkSelfPermission(this, askPermissions[0]) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, askPermissions[1]) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, askPermissions[2]) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, askPermissions, MEDIA_REQUEST_CODE);
        } else {
            openCamera();
        }
    }

    private void askGalleryPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, GALLERY_REQUEST_CODE);
        } else {
            pickImageInGallery();
        }
    }

    private void openCamera() {
        switch (flag) {
            case 1:
                Intent photoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                File photoFile = photoFile = createFile(FileType.PICTURE);

                if (notNull(photoFile)) {
                    photoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                }

                startActivityForResult(photoIntent, MEDIA_REQUEST_CODE);
                break;
            case 2:
                Intent videoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                File videoFile = createFile(FileType.VIDEO);

                if (notNull(videoFile)) {
                    videoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(videoFile));
                }
                startActivityForResult(videoIntent, MEDIA_REQUEST_CODE);
                break;
        }
        flag = 0;
    }

    private void pickImageInGallery() {
        Intent pickInGallery = new Intent(Intent.ACTION_GET_CONTENT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickInGallery.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        pickInGallery.setType("image/* video/*");
        startActivityForResult(pickInGallery, GALLERY_REQUEST_CODE);
    }

    private File createFile(FileType type) {

        File storageDir = storageDir = getExternalFilesDir(null);

        String timeStamp = dateTime.format(format);
        String fileName = timeStamp + "_";

        File file = createTempFile(fileName, storageDir, type);

        Log.i("STORAGE DIR", storageDir.getAbsolutePath());

        absoluteFilePath = file.getAbsolutePath();
        Log.i("Current Photo/Video", absoluteFilePath);
        return file;
    }

    private File createTempFile(String fileName, File storageDir, FileType type) {
        File file = null;
        String extension = type.getExtension();
        try {
            file = File.createTempFile(
                    fileName,
                    extension,
                    storageDir);
        } catch (Exception e) {
            Log.e("Error", e.getLocalizedMessage());
        }
        return file;
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        setContentView(R.layout.activity_report);

        photoCaptureButton = findViewById(R.id.photo);
        videoCaptureButton = findViewById(R.id.video);
        galleryButton = findViewById(R.id.gallery);
//        image = findViewById(R.id.imageView);

        photoCaptureButton.setOnClickListener(view -> {
            Toast.makeText(ReportActivity.this, "Button for photo is clicked", Toast.LENGTH_LONG).show();
            flag = 1;
            askCameraPermission();
        });

        videoCaptureButton.setOnClickListener(view -> {
            Toast.makeText(ReportActivity.this, "Button for video is clicked", Toast.LENGTH_LONG).show();
            flag = 2;
            askCameraPermission();
        });

        galleryButton.setOnClickListener(view -> {
            askGalleryPermission();
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MEDIA_REQUEST_CODE
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED
                && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
            Log.i("On Request Permissions", "Inside On Request Permissions");
            openCamera();
        }
        if (requestCode == GALLERY_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                pickImageInGallery();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Intent intent = new Intent(ReportActivity.this, DetailsActivity.class);

        if (requestCode == MEDIA_REQUEST_CODE && resultCode == RESULT_OK) {
            Toast.makeText(ReportActivity.this, "On Activity Result", Toast.LENGTH_LONG).show();
            intent.putExtra("isGallery", false);
            intent.putExtra("absoluteFilePath", absoluteFilePath);

            startActivityForResult(intent, MEDIA_REQUEST_CODE);
        }

        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK) {
            galleryDataPaths = new ArrayList<>();
            Log.i("Gallery", "On Activity Result for Gallery");
//            Log.i("1 file only", getPathFromUri(getApplicationContext(), data.getData()));

            intent.putExtra("isGallery", true);
            ClipData clipData = data.getClipData();
            Log.i("ClipData", String.valueOf(clipData));
            if (notNull(clipData)) {
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    Log.i("Path from URI" + i, getPathFromUri(getApplicationContext(), clipData.getItemAt(i).getUri()));
                    galleryDataPaths.add(getPathFromUri(getApplicationContext(), clipData.getItemAt(i).getUri()));
                }

                intent.putExtra("galleryDataPaths",
                        galleryDataPaths.toArray(new String[galleryDataPaths.size()]));
            } else {
                gallerySingleDataPath = getPathFromUri(getApplicationContext(), data.getData());
                intent.putExtra("gallerySingleDataPath", gallerySingleDataPath);
            }
            startActivityForResult(intent, GALLERY_REQUEST_CODE);


//            Log.i("Data", data.getData().getPath());
//            Bundle extras = data.getExtras();
//            Bitmap imageBitmap = (Bitmap) extras.get("data");
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    private boolean notNull(Object o) {
        return Objects.nonNull(o);
    }

}