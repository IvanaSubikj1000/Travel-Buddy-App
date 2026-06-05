package com.travelbuddy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

import java.util.concurrent.Executors;

public class AuthActivity extends AppCompatActivity {

    private static final String WEB_CLIENT_ID =
            "1095340571939-ujl3mq9c1el147armulalsh49ed7mcns.apps.googleusercontent.com";

    private AuthViewModel viewModel;
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private View progressBar;
    private View signInButton;
    private View createAccountButton;
    private View googleButton;
    private View guestButton;
    private FirebaseAuth.AuthStateListener authStateListener;
    private CredentialManager credentialManager;

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

        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        progressBar = findViewById(R.id.progressBar);
        signInButton = findViewById(R.id.signInButton);
        createAccountButton = findViewById(R.id.createAccountButton);
        googleButton = findViewById(R.id.googleButton);
        guestButton = findViewById(R.id.guestButton);

        viewModel.getLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            signInButton.setEnabled(!isLoading);
            createAccountButton.setEnabled(!isLoading);
            googleButton.setEnabled(!isLoading);
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
        GetGoogleIdOption option = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(WEB_CLIENT_ID)
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
                        if (!(e instanceof GetCredentialCancellationException)) {
                            runOnUiThread(() -> Toast.makeText(
                                    AuthActivity.this, e.getMessage(), Toast.LENGTH_LONG).show());
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
            }
        }
    }
}
