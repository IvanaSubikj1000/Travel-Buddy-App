package com.travelbuddy;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.lifecycle.ViewModelProvider;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Arrays;
import java.util.concurrent.Executors;

public class AuthActivity extends AppCompatActivity {

    private static final String TAG = "AuthActivity";

    private AuthViewModel viewModel;
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private View progressBar;
    private View signInButton;
    private View createAccountButton;
    private View googleButton;
    private View facebookButton;
    private View guestButton;
    private FirebaseAuth.AuthStateListener authStateListener;
    private CredentialManager credentialManager;
    private CallbackManager callbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            navigateToMain();
            return;
        }

        setContentView(R.layout.activity_auth);
        credentialManager = CredentialManager.create(this);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Temporary: log the actual key hash being used — compare with Meta Developer Portal
        try {
            android.content.pm.PackageInfo info = getPackageManager().getPackageInfo(
                    getPackageName(), android.content.pm.PackageManager.GET_SIGNATURES);
            for (android.content.pm.Signature sig : info.signatures) {
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA");
                md.update(sig.toByteArray());
                Log.d("FB_KEYHASH", "Key hash: " + android.util.Base64.encodeToString(
                        md.digest(), android.util.Base64.DEFAULT).trim());
            }
        } catch (Exception e) {
            Log.e("FB_KEYHASH", "Could not get key hash", e);
        }

        // Facebook SDK — callbackManager must be created before any login call
        callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        Log.d(TAG, "Facebook onSuccess");
                        viewModel.signInWithFacebook(loginResult.getAccessToken().getToken());
                    }

                    @Override
                    public void onCancel() {
                        Log.d(TAG, "Facebook onCancel");
                    }

                    @Override
                    public void onError(FacebookException error) {
                        Log.e(TAG, "Facebook onError: " + error.getMessage());
                        viewModel.handleAuthError(error);
                    }
                });

        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        progressBar = findViewById(R.id.progressBar);
        signInButton = findViewById(R.id.signInButton);
        createAccountButton = findViewById(R.id.createAccountButton);
        googleButton = findViewById(R.id.googleButton);
        facebookButton = findViewById(R.id.facebookButton);
        guestButton = findViewById(R.id.guestButton);

        viewModel.getLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            signInButton.setEnabled(!isLoading);
            createAccountButton.setEnabled(!isLoading);
            googleButton.setEnabled(!isLoading);
            facebookButton.setEnabled(!isLoading);
            guestButton.setEnabled(!isLoading);
        });

        viewModel.getError().observe(this, msg -> {
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        });

        signInButton.setOnClickListener(v -> {
            if (validate()) viewModel.signIn(email(), password());
        });
        createAccountButton.setOnClickListener(v -> {
            if (validate()) viewModel.createAccount(email(), password());
        });
        guestButton.setOnClickListener(v -> viewModel.signInAnonymously());
        googleButton.setOnClickListener(v -> launchGoogleSignIn());
        facebookButton.setOnClickListener(v -> launchFacebookSignIn());
    }

    // Required by Facebook SDK to deliver the login result back to CallbackManager
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onStart() {
        super.onStart();
        authStateListener = auth -> {
            if (auth.getCurrentUser() != null && !isFinishing()) {
                navigateToMain();
            }
        };
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener);
    }

    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private String email() {
        CharSequence t = emailInput.getText();
        return t != null ? t.toString().trim() : "";
    }

    private String password() {
        CharSequence t = passwordInput.getText();
        return t != null ? t.toString() : "";
    }

    private boolean validate() {
        emailLayout.setError(null);
        passwordLayout.setError(null);
        if (email().isEmpty()) {
            emailLayout.setError(getString(R.string.error_email_required));
            return false;
        }
        if (password().isEmpty()) {
            passwordLayout.setError(getString(R.string.error_password_required));
            return false;
        }
        if (password().length() < 6) {
            passwordLayout.setError(getString(R.string.error_password_too_short));
            return false;
        }
        return true;
    }

    private void launchGoogleSignIn() {
        GetSignInWithGoogleOption option = new GetSignInWithGoogleOption.Builder(
                getString(R.string.default_web_client_id))
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                null,
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        runOnUiThread(() -> handleGoogleResult(result));
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        Log.e(TAG, "Google error: " + e.getClass().getSimpleName() + " — " + e.getMessage());
                        if (!(e instanceof GetCredentialCancellationException)) {
                            String msg = e.getMessage();
                            runOnUiThread(() -> Toast.makeText(
                                    AuthActivity.this,
                                    msg != null ? msg : getString(R.string.error_google_sign_in),
                                    Toast.LENGTH_LONG).show());
                        }
                    }
                });
    }

    private void handleGoogleResult(GetCredentialResponse result) {
        if (result.getCredential() instanceof CustomCredential) {
            CustomCredential custom = (CustomCredential) result.getCredential();
            if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(custom.getType())) {
                GoogleIdTokenCredential googleCred =
                        GoogleIdTokenCredential.createFrom(custom.getData());
                viewModel.signInWithGoogle(googleCred.getIdToken());
            } else {
                Log.e(TAG, "Unexpected credential type: " + custom.getType());
                Toast.makeText(this, R.string.error_google_sign_in, Toast.LENGTH_LONG).show();
            }
        } else {
            Log.e(TAG, "Unexpected credential class: " + result.getCredential().getClass());
            Toast.makeText(this, R.string.error_google_sign_in, Toast.LENGTH_LONG).show();
        }
    }

    private void launchFacebookSignIn() {
        Log.d(TAG, "Launching Facebook sign-in");
        LoginManager.getInstance().logInWithReadPermissions(
                this,
                callbackManager,
                Arrays.asList("email", "public_profile"));
    }
}
