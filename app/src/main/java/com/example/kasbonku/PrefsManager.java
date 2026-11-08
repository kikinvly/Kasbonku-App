package com.example.kasbonku;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefsManager {

    private static final String PREF_NAME = "KasbonkuPrefs";
    private static final String KEY_HIDE_NOMINAL = "hide_nominal";
    private static final String KEY_NAMA_PEMILIK = "nama_pemilik";
    private static final String KEY_DARK_MODE = "dark_mode";

    private final SharedPreferences sharedPreferences;

    public PrefsManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // 1. SharedPreferences: Sensor Nominal
    public void setHideNominal(boolean isHidden) {
        sharedPreferences.edit().putBoolean(KEY_HIDE_NOMINAL, isHidden).apply();
    }

    public boolean isNominalHidden() {
        return sharedPreferences.getBoolean(KEY_HIDE_NOMINAL, false);
    }

    // 2. SharedPreferences: Nama Sapaan Pengguna
    public void setNamaPemilik(String nama) {
        sharedPreferences.edit().putString(KEY_NAMA_PEMILIK, nama).apply();
    }

    public String getNamaPemilik() {
        return sharedPreferences.getString(KEY_NAMA_PEMILIK, "Kiki");
    }

    // 3. SharedPreferences: Dark Mode
    public void setDarkMode(boolean isDark) {
        sharedPreferences.edit().putBoolean(KEY_DARK_MODE, isDark).apply();
    }

    public boolean isDarkMode() {
        return sharedPreferences.getBoolean(KEY_DARK_MODE, false);
    }
}