package com.travelbuddy;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class AuthViewModel extends ViewModel {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }

    public void signIn(String email, String password) {
        loading.setValue(true);
        error.setValue(null);
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    loading.setValue(false);
                    if (!task.isSuccessful()) error.setValue(messageFrom(task.getException()));
                });
    }

    public void createAccount(String email, String password) {
        loading.setValue(true);
        error.setValue(null);
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    loading.setValue(false);
                    if (!task.isSuccessful()) error.setValue(messageFrom(task.getException()));
                });
    }

    public void signInAnonymously() {
        loading.setValue(true);
        error.setValue(null);
        auth.signInAnonymously()
                .addOnCompleteListener(task -> {
                    loading.setValue(false);
                    if (!task.isSuccessful()) error.setValue(messageFrom(task.getException()));
                });
    }

    public void signInWithGoogle(String idToken) {
        loading.setValue(true);
        error.setValue(null);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    loading.setValue(false);
                    if (!task.isSuccessful()) error.setValue(messageFrom(task.getException()));
                });
    }

    private String messageFrom(Exception e) {
        return e != null ? e.getMessage() : "Unknown error";
    }
}
