/**
 * Copyright Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.firebase.codelab.friendlychat;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.acl.Group;

public class SignInActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "SignInActivity";
    private static final int SELECT_PICTURE = 500;

    private Button mSignInButton;
    private EditText mEmailEditText;
    private EditText mPasswordEditText;
    private EditText mSecondPasswordEditText;
    private LinearLayout mSignInLayout;
    private EditText mUserName;
    private LinearLayout mUserNameLayout;
    private FrameLayout mLoadingLayout;
    private Button mGoButton;




    private FirebaseAuth mFirebaseAuth;

    private StorageReference storageRef;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        // Assign fields
        mEmailEditText = (EditText) findViewById(R.id.sign_in_email_edit_text);
        mPasswordEditText = (EditText) findViewById(R.id.sign_in_password_1_edit_text);
        mSecondPasswordEditText = (EditText) findViewById(R.id.sign_in_password_2_edit_text);
        mSignInButton = (Button) findViewById(R.id.sign_in_login_button);
        mSignInLayout = (LinearLayout) findViewById(R.id.sign_in_layout);
        mUserName = (EditText) findViewById(R.id.sign_in_user_name_edit_text);
        mUserNameLayout = (LinearLayout) findViewById(R.id.sign_in_user_name_layout);
        mLoadingLayout = (FrameLayout) findViewById(R.id.sign_in_loading_layout);
        mGoButton = (Button) findViewById(R.id.sign_in_go_button);


        // Set click listeners
        mSignInButton.setOnClickListener(this);
        mGoButton.setOnClickListener(this);



        // Initialize FirebaseAuth
        mFirebaseAuth = FirebaseAuth.getInstance();
        if (mFirebaseAuth.getCurrentUser() == null){
            mSignInLayout.setVisibility(View.VISIBLE);
            mLoadingLayout.setVisibility(View.GONE);
            mUserNameLayout.setVisibility(View.GONE);
        }else {
            mUserNameLayout.setVisibility(View.VISIBLE);
            mSignInLayout.setVisibility(View.GONE);
            mLoadingLayout.setVisibility(View.GONE);
        }
        FirebaseStorage storage = FirebaseStorage.getInstance();

        storageRef = storage.getReferenceFromUrl("gs://friendlychat-c6627.appspot.com/");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_login_button:
                signIn();
                break;
            case R.id.sign_in_go_button:
                updateUserProfile();
                break;

            default:
                return;
        }
    }


    private void signIn() {
        String Email = mEmailEditText.getText().toString().trim();
        String password = mPasswordEditText.getText().toString().trim();
        String second = mSecondPasswordEditText.getText().toString().trim();

        if (isEmptyOrNull(Email)){
            Snackbar.make(mSignInLayout, "PLEASE ENTER E-Mail", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            return;
        }
        if (isEmptyOrNull(password)){
            Snackbar.make(mSignInLayout, "PLEASE ENTER PASSWORD", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            return;
        }
        if (isEmptyOrNull(second)){
            Snackbar.make(mSignInLayout, "PLEASE RE ENTER PASSWORD", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            return;
        }
        if (! password.equals(second)){
            Snackbar.make(mSignInLayout, "PASSWORDS DOES NOt MATCH", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            return;
        }

        firebaseAuthWithEmail(Email, password);

    }

    private void firebaseAuthWithEmail(String Email, String password) {
        Log.d(TAG, "firebaseAuthWithEmail:" + Email);

        mFirebaseAuth.signInWithEmailAndPassword(Email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                mLoadingLayout.setVisibility(View.GONE);
                if (task.isComplete()){
                    startPickingUsername();
                }else {
                    Snackbar.make(mSignInLayout, "Error, please try again", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
            }
        });
        mLoadingLayout.setVisibility(View.VISIBLE);
        mUserNameLayout.setVisibility(View.GONE);
        mSignInLayout.setVisibility(View.GONE);
    }


    private void updateUserProfile() {
        String userName = mUserName.getText().toString().trim();
        if (isEmptyOrNull(userName)){
            Snackbar.make(mUserNameLayout, "PLEASE ENTER USER NAME", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            return;
        }

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(userName)
                .build();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null)
            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                startActivity(new Intent(SignInActivity.this, MainActivity.class));
                                finish();
                            }else {
                                Snackbar.make(mLoadingLayout, "Error, please try again", Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                            }
                        }
                    });
        mLoadingLayout.setVisibility(View.VISIBLE);
        mUserNameLayout.setVisibility(View.GONE);
        mSignInLayout.setVisibility(View.GONE);

    }


    private void startPickingUsername() {
        mLoadingLayout.setVisibility(View.GONE);
        mUserNameLayout.setVisibility(View.VISIBLE);
        mSignInLayout.setVisibility(View.GONE);
    }


    public boolean isEmptyOrNull(String string) {
        return string == null || string.equals("") || string.trim().equals("");
    }


}
