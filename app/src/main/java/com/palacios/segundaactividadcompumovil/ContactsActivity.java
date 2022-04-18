package com.palacios.segundaactividadcompumovil;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.ListView;
import android.widget.Toast;

import com.palacios.segundaactividadcompumovil.Utilities.ContactsAdapter;

public class ContactsActivity extends AppCompatActivity {

    ListView contacts;
    String[] mProjection;
    Cursor mCursor;
    ContactsAdapter mContactsAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        //Inflate
        contacts = (ListView) findViewById(R.id.contacts);
        mProjection = new String[]{
                ContactsContract.Profile._ID,
                ContactsContract.Profile.DISPLAY_NAME_PRIMARY,
        };
        mContactsAdapter = new ContactsAdapter(this, null, 0);
        contacts.setAdapter(mContactsAdapter);

        //Ask Permisions
        getSinglePermission.launch(Manifest.permission.READ_CONTACTS);
        initView();
    }

    private void initView() {
        //If permission is granted
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            mCursor = getContentResolver().query(
                    ContactsContract.Contacts.CONTENT_URI,
                    mProjection, null, null, null);

            mContactsAdapter.changeCursor(mCursor);
        }
        else
        {
            Toast.makeText(this,"Contacts permission denied",Toast.LENGTH_LONG).show();
        }
    }

    //solicitar permisos
    ActivityResultLauncher<String> getSinglePermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean result) {
                    if (result == true) { //granted
                        initView();
                    }
                    else {//denied
                    }
                }
            });

}