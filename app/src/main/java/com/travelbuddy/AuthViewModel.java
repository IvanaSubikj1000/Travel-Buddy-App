package com.travelbuddy;

import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.GoogleAuthProvider;

public class AuthViewModel extends AndroidViewModel {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseAnalytics analytics;
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        analytics = FirebaseAnalytics.getInstance(application);
    }

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }

    public void signIn(String email, String password) {
        loading.setValue(true);
        error.setValue(null);
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    loading.setValue(false);
                    if (task.isSuccessful()) {
                        logSignIn("password");
                    } else {
                        error.setValue(messageFrom(task.getException()));
                    }
                });
    }

    public void createAccount(String email, String password) {
        loading.setValue(true);
        error.setValue(null);
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    loading.setValue(false);
                    if (task.isSuccessful()) {
                        logSignIn("password");
                    } else {
                        error.setValue(messageFrom(task.getException()));
                    }
                });
    }

    public void signInAnonymously() {
        loading.setValue(true);
        error.setValue(null);
        auth.signInAnonymously()
                .addOnCompleteListener(task -> {
                    loading.setValue(false);
                    if (task.isSuccessful()) {
                        logSignIn("anonymous");
                    } else {
                        error.setValue(messageFrom(task.getException()));
                    }
                });
    }

    public void signInWithGoogle(String idToken) {
        loading.setValue(true);
        error.setValue(null);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    loading.setValue(false);
                    if (task.isSuccessful()) {
                        logSignIn("google");
                    } else {
                        error.setValue(messageFrom(task.getException()));
                    }
                });
    }

    public void signInWithFacebook(String accessToken) {
        loading.setValue(true);
        error.setValue(null);
        AuthCredential credential = FacebookAuthProvider.getCredential(accessToken);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    loading.setValue(false);
                    if (task.isSuccessful()) {
                        logSignIn("facebook");
                    } else {
                        error.setValue(messageFrom(task.getException()));
                    }
                });
    }

    private void logSignIn(String method) {
        Bundle params = new Bundle();
        params.putString(FirebaseAnalytics.Param.METHOD, method);
        analytics.logEvent("sign_in", params);
    }

    private String messageFrom(Exception e) {
        if (e instanceof FirebaseAuthUserCollisionException) {
            return getApplication().getString(R.string.error_account_collision);
        }
        return e != null ? e.getMessage() : "Unknown error";
    }
}