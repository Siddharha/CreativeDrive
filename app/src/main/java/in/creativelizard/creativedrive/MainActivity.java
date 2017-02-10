package in.creativelizard.creativedrive;

import android.content.Intent;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final int RESOLVE_CONNECTION_REQUEST_CODE = 1;
    private static final String TAG = "response";
    GoogleApiClient mGoogleApiClient;
    TextView txtStatus;
    String fileId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialize();
    }

    private void initialize() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        txtStatus = (TextView) findViewById(R.id.txtStatus);

        fileId = "";
    }

    public void clkConnect(View view) {
        mGoogleApiClient.connect();
    }

    public void clkOpen(View view) {
        Query query = new Query.Builder()
                .addFilter(Filters.and(Filters.eq(
                        SearchableField.TITLE, "Creative_Drive"),
                        Filters.eq(SearchableField.TRASHED, false)))
                .build();
        new MyAsyncTask().execute(query);
    }


    ResultCallback<DriveFolder.DriveFolderResult> folderCreatedCallback = new
            ResultCallback<DriveFolder.DriveFolderResult>() {
                @Override
                public void onResult(DriveFolder.DriveFolderResult result) {
                    if (!result.getStatus().isSuccess()) {
                       // showMessage("Error while trying to create the folder");
                        Toast.makeText(MainActivity.this, "Error while trying to create the folder", Toast.LENGTH_SHORT).show();
                        return;
                    }
                   // showMessage("Created a folder: " + result.getDriveFolder().getDriveId());
                    String s = "Created a folder: " + result.getDriveFolder().getDriveId();
                    Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
                }
            };



    @Override
    public void onConnected(@Nullable Bundle bundle) {
        txtStatus.setText("Connected!");


}

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, RESOLVE_CONNECTION_REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {
                // Unable to resolve, message user appropriately
            }
        } else {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), this, 0).show();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RESOLVE_CONNECTION_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    mGoogleApiClient.connect();
                }
                break;
        }

    }

    class MyAsyncTask extends AsyncTask<Query, Void, DriveApi.MetadataBufferResult> {
        @Override
        protected DriveApi.MetadataBufferResult doInBackground(Query... params) {
            return Drive.DriveApi.query(mGoogleApiClient, params[0]).await();
        }

        @Override
        protected void onPostExecute(DriveApi.MetadataBufferResult metadataBufferResult) {
            if (metadataBufferResult.getMetadataBuffer().getCount() == 0) {
                // Toast(this,"",Toast.LENGTH_SHORT);
                Log.e(TAG,"Folder does not exist");
               // Log.e("FolderId",metadataBufferResult.getMetadataBuffer().get(0).getDriveId().toString());
                MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                        .setTitle("Creative_Drive").build();
                Drive.DriveApi.getRootFolder(mGoogleApiClient).createFolder(
                        mGoogleApiClient, changeSet).setResultCallback(folderCreatedCallback);
            } else {
                // showMessage("File exists");
                Log.e(TAG,"Folder exist");
                Log.e("FolderId",metadataBufferResult.getMetadataBuffer().get(0).getDriveId().toString());

                fileId = metadataBufferResult.getMetadataBuffer().get(0).getDriveId().toString();
            }
        }
    }

    public void clkDisconnect(View view){
        mGoogleApiClient.disconnect();
        txtStatus.setText("Disconnected!");
    }

    public void clkDelete(View view){

        if(!fileId.equals("")){
            DriveFile driveFile = Drive.DriveApi.getFile(mGoogleApiClient,
                    DriveId.decodeFromString(fileId));
// Call to delete file.
            driveFile.delete(mGoogleApiClient).setResultCallback(deleteCallback);
        }else {
            Toast.makeText(this, "Can't Find Folder!!", Toast.LENGTH_SHORT).show();
        }


    }

    ResultCallback<? super Status> deleteCallback = new ResultCallback<Status>() {
        @Override
        public void onResult(@NonNull Status status) {
            Toast.makeText(MainActivity.this, "File Deleted!", Toast.LENGTH_SHORT).show();
                    }
    };
}
