package com.karagathon.vesselreporting.report;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

public class ReportActivity extends BaseNavigationActivity {
    private static final int MEDIA_REQUEST_CODE = 1;
    private static final int GALLERY_REQUEST_CODE = 2;
    private int flag;
    private String absoluteFilePath;
    private Button photoCaptureButton, videoCaptureButton, galleryButton;
    private LocalDateTime dateTime = LocalDateTime.now();
    private DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private List<String> galleryDataPaths;
    private String gallerySingleDataPath;
    private boolean isGPSEnabled;

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static String getPathFromUri(final Context context, final Uri uri) {


        if (isExternalStorageDocument(uri)) {
            final String docId = DocumentsContract.getDocumentId(uri);
            final String[] split = docId.split(":");
            final String type = split[0];

            if ("primary".equalsIgnoreCase(type)) {
                return Environment.getExternalStorageDirectory() + "/" + split[1];
            }
        } else if (isDownloadsDocument(uri)) {
            final String id = DocumentsContract.getDocumentId(uri);
            final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));

            return getDataColumn(context, contentUri, null, null);

        } else if (isMediaDocument(uri)) {
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
            return getDataColumn(context, uri, null, null);
        }
        //File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
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

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
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

                if (Objects.nonNull(photoFile)) {
                    photoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                }

                startActivityForResult(photoIntent, MEDIA_REQUEST_CODE);
                break;
            case 2:
                Intent videoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                File videoFile = createFile(FileType.VIDEO);

                if (Objects.nonNull(videoFile)) {
                    videoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(videoFile));
                }
                startActivityForResult(videoIntent, MEDIA_REQUEST_CODE);
                break;
        }
        flag = 0;
    }

    private void pickImageInGallery() {
        Intent pickInGallery = new Intent(Intent.ACTION_GET_CONTENT);
        pickInGallery.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        pickInGallery.setType("*/*");
//        pickInGallery.setType("image/*, video/*");
//        pickInGallery.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {"image/*, video/*"});
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        View rootView = getLayoutInflater().inflate(R.layout.activity_report, frameLayout);

        photoCaptureButton = findViewById(R.id.photo);
        videoCaptureButton = findViewById(R.id.video);
        galleryButton = findViewById(R.id.gallery);
//        image = findViewById(R.id.imageView);

        photoCaptureButton.setOnClickListener(view -> {
            Toast.makeText(ReportActivity.this, "Button for photo is clicked", Toast.LENGTH_LONG).show();
            flag = 1;
            checkGPSIsEnabled();
            if (isGPSEnabled) {
                askCameraPermission();
                isGPSEnabled = false;
            }
        });

        videoCaptureButton.setOnClickListener(view -> {
            Toast.makeText(ReportActivity.this, "Button for video is clicked", Toast.LENGTH_LONG).show();
            checkGPSIsEnabled();
            flag = 2;
            if (isGPSEnabled) {
                askCameraPermission();
                isGPSEnabled = false;
            }

        });

        galleryButton.setOnClickListener(view -> {
            checkGPSIsEnabled();
            if (isGPSEnabled) {
                askGalleryPermission();
                isGPSEnabled = false;
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MEDIA_REQUEST_CODE
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED
                && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
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
            if (Objects.nonNull(clipData)) {
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
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        moveTaskToBack(true);
    }

    private void checkGPSIsEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showGPSDialog();
        } else {
            isGPSEnabled = true;
            Log.i("GPS is Enabled", "GPS is Enabled");
        }
    }

    private void showGPSDialog() {
        AlertDialog alertDialog = new AlertDialog.Builder(ReportActivity.this).create();
        alertDialog.setTitle("Location is Off");
        alertDialog.setMessage("Turn on Location to report");
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Turn On Location", (dialogInterface, i) -> {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));

        });

        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", (dialogInterface, i) -> {
            //
        });

        alertDialog.show();
    }
}