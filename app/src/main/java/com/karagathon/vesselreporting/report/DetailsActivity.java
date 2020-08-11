package com.karagathon.vesselreporting.report;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.DatePicker;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.storage.s3.AWSS3StoragePlugin;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.karagathon.vesselreporting.R;

import org.json.JSONObject;

import java.io.File;
import java.util.Calendar;
import java.util.Map;

public class DetailsActivity extends AppCompatActivity {

    private static final String UPLOAD_URL = "http://192.168.0.109:1331/upload";
    private EditText dateText;
    private DatePickerDialog picker;
    private String absoluteFilePath;
    private String currentFileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        try {
            Amplify.addPlugin(new AWSCognitoAuthPlugin());
            Amplify.addPlugin(new AWSS3StoragePlugin());
            Amplify.configure(getApplicationContext());

            Log.i("MyAmplifyApp", "Initialized Amplify");
        } catch (AmplifyException e) {
            e.printStackTrace();
        }

        dateText = findViewById(R.id.date);

        setTitle("Title");

        dateText.setOnClickListener(view -> {
            final Calendar cldr = Calendar.getInstance();
            int day = cldr.get(Calendar.DAY_OF_MONTH);
            int month = cldr.get(Calendar.MONTH);
            int year = cldr.get(Calendar.YEAR);

            picker = new DatePickerDialog(DetailsActivity.this, R.style.Theme_MaterialComponents_Light_Dialog_FixedSize, new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {
                    dateText.setText(dayOfMonth + "-" + (month + 1) + "-" + year);
                }
            }, year, month, day);
            picker.show();
        });

        Intent intent = getIntent();
        currentFileName = intent.getStringExtra("currentFileName");
        absoluteFilePath = intent.getStringExtra("absoluteFilePath");

        Log.i("Details Current Filename", currentFileName);
        Log.i("Details Absolute File Path", absoluteFilePath);

        File f = new File(absoluteFilePath);

//        Amplify.Storage.uploadFile(currentFileName, f, result -> Log.i("MyAmplifyApp", "Successfully uploaded: " + result.getKey()), storageFailure -> Log.e("MyAmplifyApp", "Upload failed", storageFailure));

    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("On Destroy", "On Destroy");
        File f = new File(absoluteFilePath);
        f.delete();
    }

    private void uploadFile(Map data) {

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
}