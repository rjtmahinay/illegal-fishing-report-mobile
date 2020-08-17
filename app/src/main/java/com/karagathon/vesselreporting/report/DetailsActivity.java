package com.karagathon.vesselreporting.report;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.storage.s3.AWSS3StoragePlugin;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.karagathon.vesselreporting.BuildConfig;
import com.karagathon.vesselreporting.R;
import com.karagathon.vesselreporting.adapter.PlacesAutoCompletedAdapter;
import com.karagathon.vesselreporting.helper.DatabaseHelper;
import com.karagathon.vesselreporting.model.Report;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.CloseableHttpResponse;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.message.BasicNameValuePair;

public class DetailsActivity extends AppCompatActivity {

    private static final String UPLOAD_URL = "http://192.168.0.109:1331/upload";
    public static final int AUTO_PLACE_REQ_CODE = 200;
    public static final String TEXT_MESSAGE_API_URL = "https://api.semaphore.co/api/v4/messages";
    private EditText dateText, locationText, reportDescription;
    private Button submitButton;
    private DatePickerDialog picker;
    private String currentFileName;
    private Intent reportIntent;
    private HashMap<String, Object> dataMap;
    private File singleMediaFile;
    private boolean isGallery;
    private GoogleSignInClient googleSignInClient;
    private TextView nameView;
    private FirebaseAuth auth;
    private PlacesAutoCompletedAdapter adapter;
    private AutoCompleteTextView autoCompleteTextView;
    private LocalDate localDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        if (Objects.isNull(savedInstanceState)) {
            try {
                Amplify.addPlugin(new AWSCognitoAuthPlugin());
                Amplify.addPlugin(new AWSS3StoragePlugin());
                Amplify.configure(getApplicationContext());

                Log.i("MyAmplifyApp", "Initialized Amplify");
            } catch (AmplifyException e) {
                e.printStackTrace();
            }
        }

        dataMap = new HashMap<>();
        dateText = findViewById(R.id.date);
        submitButton = findViewById(R.id.submit);
        locationText = findViewById(R.id.location);
        reportDescription = findViewById(R.id.reportDescription);

        nameView = findViewById(R.id.name);

        auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        nameView.setText(currentUser.getDisplayName());

        dateText.setOnClickListener(view -> {
            final Calendar cldr = Calendar.getInstance();
            int day = cldr.get(Calendar.DAY_OF_MONTH);
            int month = cldr.get(Calendar.MONTH);
            int year = cldr.get(Calendar.YEAR);

            picker = new DatePickerDialog(DetailsActivity.this, R.style.Theme_MaterialComponents_Light_Dialog_FixedSize,
                    (datePicker, year1, month1, dayOfMonth)
                            -> {
                        dateText.setText(String.format("%d-%d-%d", year1, month1 + 1, dayOfMonth));
                        localDate = LocalDate.of(year1, month1 + 1, dayOfMonth);
                    }, year, month, day);
            picker.show();
        });


        Places.initialize(getApplicationContext(), BuildConfig.GOOGLE_API_KEY);

        autoCompleteTextView = findViewById(R.id.location);
        autoCompleteTextView.setThreshold(3);

        RectangularBounds bounds = RectangularBounds.newInstance(
                new LatLng(4.2158064, 114.0952145),
                new LatLng(21.3217806, 126.8072562));

        adapter = new PlacesAutoCompletedAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, bounds);

        autoCompleteTextView.setAdapter(adapter);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean isHideNameEnabled = sharedPreferences.getBoolean("hide_name", false);

        if (isHideNameEnabled) {
            nameView.setVisibility(View.GONE);
        }

//        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
//                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
//
//        // Specify the types of place data to return.
//        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME));
//
//        // Set up a PlaceSelectionListener to handle the response.
//        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
//            @Override
//            public void onPlaceSelected(Place place) {
//                // TODO: Get info about the selected place.
//                Log.i("Location", "Place: " + place.getName() + ", " + place.getId());
//            }
//
//
//            @Override
//            public void onError(Status status) {
//                // TODO: Handle the error.
//                Log.i("Location", "An error occurred: " + status);
//            }
//        });
//        locationText.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//            }
//
//            @Override
//            public void afterTextChanged(Editable editable) {
//                if (locationText.length() < 1) {
//                    locationText.setError("Location is required");
//                }
//            }
//        });


        submitButton.setOnClickListener(view -> {
            reportIntent = getIntent();
            isGallery = reportIntent.getBooleanExtra("isGallery", false);

            dataMap.put("name", currentUser.getDisplayName());
            dataMap.put("location", autoCompleteTextView.getEditableText().toString());
            dataMap.put("date", dateText.getText().toString());
            dataMap.put("description", reportDescription.getText().toString());

            if (isGallery) {
                String[] galleryDataPaths = reportIntent.getStringArrayExtra("galleryDataPaths");

                processGalleryData(galleryDataPaths);
                uploadData(dataMap);

            } else {
//            currentFileName = reportIntent.getStringExtra("currentFileName");
                String absoluteFilePath = reportIntent.getStringExtra("absoluteFilePath");

//            Log.i("Details Current Filename", currentFileName);
                Log.i("Details Absolute File Path", absoluteFilePath);

                singleMediaFile = new File(absoluteFilePath);
                dataMap.put("files", Arrays.asList(singleMediaFile.getName()));

                uploadData(dataMap);
//            storeFilesInObjectStorage(file.getName(), file);
            }
            //send sms
            Log.i("Sending a Message Now!", "Sending a Message Now!");
//           new SendTextMessage().execute();

            Report report
                    = new Report(nameView.getText().toString(), locationText.getText().toString(),
                    reportDescription.getText().toString(), localDate);

            DatabaseHelper databaseHelper = new DatabaseHelper(getApplicationContext());
            databaseHelper.add(report);
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        Log.i("Places Result Code", String.valueOf(resultCode));
//        if (requestCode == AUTO_PLACE_REQ_CODE && resultCode == RESULT_OK) {
//
//            Place place = Autocomplete.getPlaceFromIntent(data);
//
//            locationText.setText(place.getAddress());
//        } else if (requestCode == AUTO_PLACE_REQ_CODE && resultCode == RESULT_CANCELED) {
//            Log.i("User", "Cancelled");
//
//        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("On Destroy", "On Destroy");
//        File f = new File(absoluteFilePath);
//        f.delete();

        if (!isGallery) {
            if (Objects.nonNull(singleMediaFile)) {
                singleMediaFile.delete();
            }
        }
        reportIntent = null;
        dataMap = null;

        try {
            Amplify.removePlugin(new AWSCognitoAuthPlugin());
            Amplify.removePlugin(new AWSS3StoragePlugin());
        } catch (AmplifyException e) {
            e.printStackTrace();
        }

    }

    private void uploadData(Map data) {

        Request request = new JsonObjectRequest(Request.Method.POST, UPLOAD_URL, new JSONObject(data), response -> {
            //empty
        }, error -> {
            //empty
        });

        RequestQueue rQueue = Volley.newRequestQueue(getApplicationContext());
        rQueue.add(request);
    }

    private void processGalleryData(String[] galleryDataPaths) {
        File file;

        if (Objects.isNull(galleryDataPaths)) {
            String gallerySingleDataPath
                    = reportIntent.getStringExtra("gallerySingleDataPath");
            file = new File(gallerySingleDataPath);

            dataMap.put("files", Arrays.asList(file.getName()));
//            storeFilesInObjectStorage(file.getName(), file);
            return;
        }

        List<String> fileNames = new ArrayList<>(galleryDataPaths.length);
        for (String galleryPaths : galleryDataPaths) {
            Log.i("Gallery Details Data", galleryPaths);
            file = new File(galleryPaths);
            fileNames.add(file.getName());
//            storeFilesInObjectStorage(file.getName(), file);
        }

        dataMap.put("files", fileNames);
    }

    private void storeFilesInObjectStorage(String fileName, File file) {
        Amplify.Storage.uploadFile(fileName, file, result ->
                        Log.i("MyAmplifyApp", "Successfully uploaded: " + result.getKey()),
                storageFailure -> Log.e("MyAmplifyApp", "Upload failed", storageFailure));
    }


    private class SendTextMessage extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] objects) {
            sendSMS();
            return null;
        }

        private void sendSMS() {
            HttpPost post = new HttpPost(TEXT_MESSAGE_API_URL);

            String message = "Hi from Android";
            // add request parameter, form parameters
            List<NameValuePair> urlParameters = new ArrayList<>();
            urlParameters.add(new BasicNameValuePair("apikey", BuildConfig.SEMAPHORE_API_KEY));
            urlParameters.add(new BasicNameValuePair("number", "09272697150"));
            urlParameters.add(new BasicNameValuePair("message", message));

            try {
                post.setEntity(new UrlEncodedFormEntity(urlParameters));

                CloseableHttpClient httpClient = HttpClients.createDefault();
                CloseableHttpResponse response = httpClient.execute(post);
                Log.i("Send SMS", String.valueOf(response));

            } catch (UnsupportedEncodingException ue) {
                Log.e("Unsupported Exception", ue.getLocalizedMessage());
            } catch (IOException ie) {
                Log.e("IOException", ie.getLocalizedMessage());
            }
        }
    }

}