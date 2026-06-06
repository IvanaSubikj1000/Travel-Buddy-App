package com.travelbuddy.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.travelbuddy.AuthActivity;
import com.travelbuddy.R;

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView emailText = view.findViewById(R.id.emailText);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.isAnonymous() || user.getEmail() == null) {
            emailText.setText(R.string.guest);
        } else {
            emailText.setText(user.getEmail());
        }

        TextView languageValue = view.findViewById(R.id.languageValue);
        languageValue.setText(currentLanguageLabel());

        view.findViewById(R.id.languageRow).setOnClickListener(v -> showLanguagePicker());

        view.findViewById(R.id.signOutButton).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(requireContext(), AuthActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
    }

    private void showLanguagePicker() {
        String[] options = {
            getString(R.string.language_system_default),
            getString(R.string.language_english),
            getString(R.string.language_macedonian)
        };

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.language_picker_title)
                .setSingleChoiceItems(options, currentLanguageIndex(), (dialog, which) -> {
                    dialog.dismiss();
                    switch (which) {
                        case 1:
                            AppCompatDelegate.setApplicationLocales(
                                    LocaleListCompat.forLanguageTags("en"));
                            break;
                        case 2:
                            AppCompatDelegate.setApplicationLocales(
                                    LocaleListCompat.forLanguageTags("mk"));
                            break;
                        default:
                            AppCompatDelegate.setApplicationLocales(
                                    LocaleListCompat.getEmptyLocaleList());
                            break;
                    }
                })
                .show();
    }

    private String currentLanguageLabel() {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        if (locales.isEmpty()) return getString(R.string.language_system_default);
        String lang = locales.get(0).getLanguage();
        if ("en".equals(lang)) return getString(R.string.language_english);
        if ("mk".equals(lang)) return getString(R.string.language_macedonian);
        return getString(R.string.language_system_default);
    }

    private int currentLanguageIndex() {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        if (locales.isEmpty()) return 0;
        String lang = locales.get(0).getLanguage();
        if ("en".equals(lang)) return 1;
        if ("mk".equals(lang)) return 2;
        return 0;
    }

    @Override
    public void onResume() {
        super.onResume();
        Bundle params = new Bundle();
        params.putString(FirebaseAnalytics.Param.SCREEN_NAME, "profile");
        params.putString(FirebaseAnalytics.Param.SCREEN_CLASS, "ProfileFragment");
        FirebaseAnalytics.getInstance(requireContext())
                .logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, params);
    }
}