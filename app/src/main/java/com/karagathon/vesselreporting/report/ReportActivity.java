package com.karagathon.vesselreporting.report;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.karagathon.vesselreporting.R;
import com.karagathon.vesselreporting.constant.FileType;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String UPLOAD_URL = "http://192.168.0.109:1331/upload";
    private static final String DETAILS_URL = "http://192.168.0.109:1331/reportDetails";
    private Button photoCaptureButton, videoCaptureButton, submitButton;
    //    private ImageButton photoCaptureButton;
    private int flag;
    private Uri fileProvider;
    private Bitmap bitMap;
    private ImageView image;
    private EditText dateText, descText;
    private String currentPhotoPath;
    private String currentVideoPath;
    private boolean isImage;
    private LocalDateTime dateTime = LocalDateTime.now();
    private DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private String imageString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        setContentView(R.layout.activity_report);

        photoCaptureButton = findViewById(R.id.photo);
        videoCaptureButton = findViewById(R.id.video);
        submitButton = findViewById(R.id.submit);
//        image = findViewById(R.id.imageView);

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
                HashMap<String, List<String>> map = new HashMap<>();
//                Intent intent = new Intent(ReportActivity.this, DetailsActivity.class);
//                startActivity(intent);
//                finish();
                if (isImage && (notNull(currentPhotoPath) && !currentPhotoPath.isEmpty())) {
                    Log.i("IMAGE STRING", imageString);
                    map.put("image", Arrays.asList(imageString, "testImageString"));
                    uploadFile(map);
//                     uploadFileTest(currentPhotoPath);
                    isImage = false;
                } else {
                    if (notNull(currentVideoPath) && !currentVideoPath.isEmpty()) {
//                        uploadFile(currentVideoPath);
                    }

                }
//                sendDetails(data);
            }


        });



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

    private void openCamera() {
        Intent intent = null;
        if (flag == 1) {
            intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File photoFile = photoFile = createFile(FileType.PICTURE);

            if (notNull(photoFile)) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
            }

        } else {
            intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            File videoFile = createFile(FileType.VIDEO);

            if (notNull(videoFile)) {
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

        if (resultCode == RESULT_OK) {
            Toast.makeText(ReportActivity.this, "On Activity Result", Toast.LENGTH_LONG).show();
//            Log.i("Data", data.getData().getPath());
//            Bundle extras = data.getExtras();
//            Bitmap imageBitmap = (Bitmap) extras.get("data");
//
            imageString = getBase64Image();
//            Uri photoUri = (Uri) extras.get("output");


//            filePath = getRealPathFromURI(photoUri);
            //            ImageDecoder.Source decoder = ImageDecoder.createSource(getContentResolver(), filePath);
//                bitMap = ImageDecoder.decodeBitmap(decoder);
//                image.setImageBitmap(imageBitmap);


        }
    }

//    private void uploadFileTest(String filePath) {
//        SimpleMultiPartRequest request = new SimpleMultiPartRequest(Request.Method.POST, UPLOAD_URL, new Response.Listener<String>() {
//            @Override
//            public void onResponse(String response) {
//                Toast.makeText(ReportActivity.this, "Response: " + response, Toast.LENGTH_LONG).show();
//                try {
//                    JSONObject jObj = new JSONObject(response);
//                    String message = jObj.getString("message");
//                } catch (Exception e) {
//                    //empty
//                }
//            }
//
//
//        }, new Response.ErrorListener() {
//
//            @Override
//            public void onErrorResponse(VolleyError error) {
//                //empty
//            }
//        });
//
//        request.addFile("file", filePath);
//        RequestQueue rQueue = Volley.newRequestQueue(getApplicationContext());
//        rQueue.add(request);
//
//    }

    private void uploadFile(Map data) {
//        SimpleMultiPartRequest request = new SimpleMultiPartRequest(Request.Method.POST, UPLOAD_URL, new Response.Listener<String>() {
//            @Override
//            public void onResponse(String response) {
//                Toast.makeText(ReportActivity.this, "Response: " + response, Toast.LENGTH_LONG).show();
//                try {
//                    JSONObject jObj = new JSONObject(response);
//                    String message = jObj.getString("message");
//                } catch (Exception e) {
//                    //empty
//                }
//            }
//
//
//        }, new Response.ErrorListener() {
//
//            @Override
//            public void onErrorResponse(VolleyError error) {
//                //empty
//            }
//        });
//
//        request.addFile("file", filePath);
//        RequestQueue rQueue = Volley.newRequestQueue(getApplicationContext());
//        rQueue.add(request);

//        StringRequest request = new StringRequest(Request.Method.POST, UPLOAD_URL, new Response.Listener<String>() {
//            @Override
//            public void onResponse(String response) {
//
//            }
//        }, new Response.ErrorListener() {
//            @Override
//            public void onErrorResponse(VolleyError error) {
//
//            }
//        }){
//            @Override
//            protected Map<String, String> getParams(){
//
//                Map<String, String> params = new HashMap<>();
//
//                params.put("files", filePath);
//                params.put("files", "test");
//                return params;
//            }
//        };

        JSONObject jsonRequest = null;
        Request request = new JsonObjectRequest(Request.Method.POST, UPLOAD_URL, new JSONObject(data), new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                //empty
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                //empty
            }
        });

        RequestQueue rQueue = Volley.newRequestQueue(getApplicationContext());
        rQueue.add(request);
    }

//    private void uploadFile1(String filePath) {
//        SimpleMultiPartRequest request = new SimpleMultiPartRequest(Request.Method.POST, UPLOAD_URL, new Response.Listener<String>() {
//            @Override
//            public void onResponse(String response) {
//                Toast.makeText(ReportActivity.this, "Response: " + response, Toast.LENGTH_LONG).show();
//                try {
//                    JSONObject jObj = new JSONObject(response);
//                    String message = jObj.getString("message");
//                } catch (Exception e) {
//                    //empty
//                }
//            }
//
//
//        }, new Response.ErrorListener() {
//
//            @Override
//            public void onErrorResponse(VolleyError error) {
//                //empty
//            }
//        }){
//            @Override
//            protected Map<String, String> getParams() {
//                Map<String, String> params = new HashMap<>();
//                params.put("date", Optional.of(dateText.getText().toString().trim()).orElse(""));
//                params.put("description", descText.getText().toString().trim());
//
//                return params;
//            }
//
//
//        };
//
//        request.addFile("file", filePath);
//        RequestQueue rQueue = Volley.newRequestQueue(getApplicationContext());
//        rQueue.add(request);
//    }

//    private void sendDetails(HashMap data) {
//        JSONObject jsonRequest = null;
//        Request request = new JsonObjectRequest(Request.Method.POST, DETAILS_URL, new JSONObject(data), new Response.Listener<JSONObject>() {
//
//            @Override
//            public void onResponse(JSONObject response) {
//                //empty
//            }
//        }, new Response.ErrorListener() {
//
//            @Override
//            public void onErrorResponse(VolleyError error) {
//                //empty
//            }
//        });
//        RequestQueue rQueue = Volley.newRequestQueue(getApplicationContext());
//        rQueue.add(request);
//    }

    private File createFile(FileType type) {
//        File storageDir = storageDir = Environment.getExternalStoragePublicDirectory(
//                Environment.DIRECTORY_DCIM + "/Camera") ;

        File storageDir = storageDir = getExternalFilesDir(null);

        String timeStamp = dateTime.format(format);
        String fileName = timeStamp + "_";

        File file = createTempFile(fileName, storageDir, type);

        Log.i("STORAGE DIR", storageDir.getAbsolutePath());

        currentPhotoPath = file.getAbsolutePath();
        Log.i("Current Photo", currentPhotoPath);
        return file;
    }

    private File createTempFile(String fileName, File storageDir, FileType type) {
        File file = null;
        try {
            switch (type) {
                case PICTURE:
                    file = File.createTempFile(
                            fileName,
                            ".jpg",
                            storageDir);
                    break;
                case VIDEO:
                    file = File.createTempFile(
                            fileName,
                            ".mp4",
                            storageDir);
            }
        } catch (Exception e) {
            Log.e("Error", e.getLocalizedMessage());
        }
        return file;
    }

    public String getStringImage(Bitmap bmp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.NO_WRAP);
    }

    public String getBase64Image(Bitmap bmp) {
        ByteBuffer buffer = ByteBuffer.allocate(bmp.getRowBytes() *
                bmp.getHeight());
        bmp.copyPixelsToBuffer(buffer);
        byte[] data = buffer.array();
        return Base64.encodeToString(data, Base64.DEFAULT);
    }

    public String getBase64Image() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Log.i("Photo Path", currentPhotoPath);
        Bitmap bmp = BitmapFactory.decodeFile(currentPhotoPath);
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, bos);
        byte[] imageBytes = bos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }
//    private File createImageFile() {
//        File image = null;
//        try {
//
//            String timeStamp = dateTime.format(format);
//            String imageFileName = timeStamp + "_";
//            File storageDir = Environment.getExternalStoragePublicDirectory(
//                    Environment.DIRECTORY_PICTURES);
//            image = File.createTempFile(
//                    imageFileName,
//                    ".jpg",
//                    storageDir
//            );
//            currentPhotoPath = image.getAbsolutePath();
//        } catch (Exception e) {
//            Log.e("Error", e.getLocalizedMessage());
//        }
//        return image;
//    }
//
//    private File createVideoFile() {
//        File video = null;
//        try {
//            // Create an image file name
//            String timeStamp = dateTime.format(format);
//            String imageFileName = timeStamp + "_";
//            File storageDir = Environment.getExternalStoragePublicDirectory(
//                    Environment.DIRECTORY_DOWNLOADS);
//            video = File.createTempFile(
//                    imageFileName,
//                    ".mp4",
//                    storageDir
//            );
//            currentVideoPath = video.getAbsolutePath();
//        } catch (Exception e) {
//            Log.e("Error", e.getLocalizedMessage());
//        }
//        return video;
//    }

    private boolean notNull(Object o) {
        return (o != null);
    }
}