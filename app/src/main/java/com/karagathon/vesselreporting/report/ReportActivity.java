package com.karagathon.vesselreporting.report;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.error.VolleyError;
import com.android.volley.request.JsonObjectRequest;
import com.android.volley.request.SimpleMultiPartRequest;
import com.android.volley.toolbox.Volley;
import com.karagathon.vesselreporting.R;

import org.json.JSONObject;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class ReportActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String UPLOAD_URL = "http://192.168.0.109:1331/upload";
    private static final String DETAILS_URL = "http://192.168.0.109:1331/reportDetails";
    private Button photoCaptureButton, videoCaptureButton, submitButton;
    private int flag;
    private Uri fileProvider;
    private Bitmap bitMap;
    private ImageView image;
    private EditText dateText, descText;
    private DatePickerDialog picker;
    private String currentPhotoPath;
    private String currentVideoPath;
    private boolean isImage;
    private LocalDateTime dateTime = LocalDateTime.now();
    private DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        setContentView(R.layout.activity_report);

        photoCaptureButton = findViewById(R.id.photo);
        videoCaptureButton = findViewById(R.id.video);
        submitButton = findViewById(R.id.submit);
        image = findViewById(R.id.imageView);
        dateText = findViewById(R.id.date);
        descText = findViewById(R.id.reportDescription);

        photoCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ReportActivity.this, "Button for photo is clicked", Toast.LENGTH_LONG).show();
                flag = 1;
                isImage = true;
                askPermission();
            }

        });

        videoCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(ReportActivity.this, "Button for video is clicked", Toast.LENGTH_LONG).show();
                askPermission();
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                HashMap<String, Object> data = new HashMap<>();
                if (isImage && (notNull(currentPhotoPath) && !currentPhotoPath.isEmpty())) {
                    uploadFile(currentPhotoPath);
//                       data.put("file", currentPhotoPath);
                    isImage = false;
                } else {
                    if (notNull(currentVideoPath) && !currentVideoPath.isEmpty()) {
                        uploadFile(currentVideoPath);
//                           data.put("file", currentVideoPath);
                    }

                }


                data.put("date", Optional.of(dateText.getText().toString().trim()).orElse(""));
                data.put("description", descText.getText().toString().trim());

                sendDetails(data);
            }


        });

        dateText.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                final Calendar cldr = Calendar.getInstance();
                int day = cldr.get(Calendar.DAY_OF_MONTH);
                int month = cldr.get(Calendar.MONTH);
                int year = cldr.get(Calendar.YEAR);

                picker = new DatePickerDialog(ReportActivity.this, R.style.Theme_MaterialComponents_Light_Dialog_FixedSize, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {
                        dateText.setText(dayOfMonth + "-" + (month + 1) + "-" + year);
                    }
                }, year, month, day);
                picker.show();
            }
        });


        //end
    }


    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                openCamera();
        }
    }

    private void askPermission() {
        List<String> askPermissions = Arrays.asList(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE);
        List<String> askPermissionTemp = new ArrayList<>();
        for (String permission : askPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                askPermissionTemp.add(permission);
            }
        }

        if (askPermissionTemp.size() > 0) {
            ActivityCompat.requestPermissions(this, askPermissionTemp.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }

        if (!askPermissionTemp.contains(Manifest.permission.CAMERA)) {
            openCamera();
        }
    }

    //    private void askPermission() {
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST);
//        } else {
//            openCamera();
//        }
//    }
    private void openCamera() {
        Intent intent = null;
        if (flag == 1) {
            intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Log.i("IMAGE CAMERA", "before photoFile");
            File photoFile = photoFile = createImageFile();

            if (photoFile != null) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
            }

        } else {
            intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            File videoFile = createVideoFile();

            if (videoFile != null) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(videoFile));
            }
        }
        startActivityForResult(intent, PERMISSION_REQUEST_CODE);
        flag = 0;
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

//        Toast.makeText(ReportActivity.this, "Outside Activity Result", Toast.LENGTH_LONG).show();
//        Toast.makeText(ReportActivity.this, "Result Code: " + resultCode, Toast.LENGTH_LONG).show();

        if (resultCode == RESULT_OK) {
            Toast.makeText(ReportActivity.this, "On Activity Result", Toast.LENGTH_LONG).show();
//            Bundle extras = data.getExtras();
//            Bitmap imageBitmap = (Bitmap) extras.get("data");
//            Uri photoUri = (Uri) extras.get("output");


//            filePath = getRealPathFromURI(photoUri);
            ;//            ImageDecoder.Source decoder = ImageDecoder.createSource(getContentResolver(), filePath);
//                bitMap = ImageDecoder.decodeBitmap(decoder);
//                image.setImageBitmap(imageBitmap);
        }
    }

    private void uploadFile(String filePath) {
        SimpleMultiPartRequest request = new SimpleMultiPartRequest(Request.Method.POST, UPLOAD_URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Toast.makeText(ReportActivity.this, "Response: " + response, Toast.LENGTH_LONG).show();
                try {
                    JSONObject jObj = new JSONObject(response);
                    String message = jObj.getString("message");
//                    Toast.makeText(getApplicationContext(), "ZZTM Message" + message, Toast.LENGTH_LONG).show();
                    Log.i("ZZTM Message", message);
                } catch (Exception e) {

                }
            }


        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        request.addFile("file", filePath);
        RequestQueue rQueue = Volley.newRequestQueue(getApplicationContext());
        rQueue.add(request);
    }

    private void sendDetails(HashMap data) {
        JSONObject jsonRequest = null;
        Request request = null;
        request = new JsonObjectRequest(Request.Method.POST, DETAILS_URL, new JSONObject(data), new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        RequestQueue rQueue = Volley.newRequestQueue(getApplicationContext());
        rQueue.add(request);
    }

    private File createImageFile() {
        File image = null;
        try {

            String timeStamp = dateTime.format(format);
            String imageFileName = timeStamp + "_";
            Log.i("CREATE", "Create Image File 2");
            File storageDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES);
            image = File.createTempFile(
                    imageFileName,
                    ".jpg",
                    storageDir
            );
            Log.i("CREATE", "Create Image File 3");
            Log.i("Absolute Path", "file:" + image.getAbsolutePath());
            currentPhotoPath = image.getAbsolutePath();
        } catch (Exception e) {
            Log.e("Error", e.getLocalizedMessage());
        }
        return image;
    }

    private File createVideoFile() {
        File video = null;
        try {
            Log.i("CREATE", "Create Video File");
            // Create an image file name
            String timeStamp = dateTime.format(format);
            String imageFileName = timeStamp + "_";
            Log.i("CREATE", "Create Video File 2");
            File storageDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS);
            video = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".mp4",         /* suffix */
                    storageDir      /* directory */
            );
            Log.i("CREATE", "Create Video File 3");
            // Save a file: path for use with ACTION_VIEW intents
            Log.i("Absolute Path", "file:" + video.getAbsolutePath());
            currentVideoPath = video.getAbsolutePath();
        } catch (Exception e) {
            Log.e("Error", e.getLocalizedMessage());
        }
        return video;
    }

    private boolean notNull(Object o) {
        return (o != null);
    }
}