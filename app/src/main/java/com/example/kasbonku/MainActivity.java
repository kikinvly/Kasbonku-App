package com.example.kasbonku;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.textfield.TextInputLayout;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    static class Pelanggan {
        int id;
        String nama;
        int hutang;
        String tanggal;

        Pelanggan(int id, String nama, int hutang, String tanggal) {
            this.id = id;
            this.nama = nama;
            this.hutang = hutang;
            this.tanggal = tanggal;
        }
    }

    // DATABASE HELPER (DATABASE LOKAL PERSISTEN)
    static class DatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "KasbonkuRoom.db";
        private static final int DATABASE_VERSION = 2; // Version bump untuk tambah kolom tanggal
        private static final String TABLE_PELANGGAN = "pelanggan";

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_PELANGGAN + " (id INTEGER PRIMARY KEY AUTOINCREMENT, nama TEXT, hutang INTEGER, tanggal TEXT)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_PELANGGAN);
            onCreate(db);
        }

        public void tambahPelanggan(String nama, int hutang, String tanggal) {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("nama", nama);
            cv.put("hutang", hutang);
            cv.put("tanggal", tanggal);
            db.insert(TABLE_PELANGGAN, null, cv);
        }

        public void updateHutang(int id, int hutangBaru) {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("hutang", hutangBaru);
            db.update(TABLE_PELANGGAN, cv, "id=?", new String[]{String.valueOf(id)});
        }

        public void hapusPelanggan(int id) {
            SQLiteDatabase db = this.getWritableDatabase();
            db.delete(TABLE_PELANGGAN, "id=?", new String[]{String.valueOf(id)});
        }

        public List<Pelanggan> getAllPelanggan() {
            List<Pelanggan> list = new ArrayList<>();
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_PELANGGAN, null);
            if (cursor.moveToFirst()) {
                do {
                    String tgl = cursor.getColumnCount() > 3 ? cursor.getString(3) : "Baru saja";
                    list.add(new Pelanggan(cursor.getInt(0), cursor.getString(1), cursor.getInt(2), tgl));
                } while (cursor.moveToNext());
            }
            cursor.close();
            return list;
        }
    }

    private PrefsManager prefsManager;
    private DatabaseHelper dbHelper;
    private ImageButton btnToggleSensor, btnSettings;
    private TextView tvTotalHutang, tvNamaPemilik, lblTambah, tvLabelTotal;
    private EditText etNamaPelanggan, etNominalInput, etCariPelanggan;
    private TextInputLayout tilNama, tilNominal, tilCari;
    private Button btnSimpanCatatan;
    private LinearLayout llDaftarPelanggan, mainLayout;
    private CardView cardHeader, cardFormTambah;

    private List<Pelanggan> listPelanggan = new ArrayList<>();
    private String filterKataKunci = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefsManager = new PrefsManager(this);
        dbHelper = new DatabaseHelper(this);

        mainLayout = findViewById(R.id.mainLayout);
        cardHeader = findViewById(R.id.cardHeader);
        cardFormTambah = findViewById(R.id.cardFormTambah);

        btnToggleSensor = findViewById(R.id.btnToggleSensor);
        btnSettings = findViewById(R.id.btnSettings);
        tvTotalHutang = findViewById(R.id.tvTotalHutang);
        tvNamaPemilik = findViewById(R.id.tvNamaPemilik);
        lblTambah = findViewById(R.id.lblTambah);
        tvLabelTotal = findViewById(R.id.tvLabelTotal);

        tilNama = findViewById(R.id.tilNama);
        tilNominal = findViewById(R.id.tilNominal);
        tilCari = findViewById(R.id.tilCari);

        etNamaPelanggan = findViewById(R.id.etNamaPelanggan);
        etNominalInput = findViewById(R.id.etNominalInput);
        etCariPelanggan = findViewById(R.id.etCariPelanggan);
        btnSimpanCatatan = findViewById(R.id.btnSimpanCatatan);
        llDaftarPelanggan = findViewById(R.id.llDaftarPelanggan);

        btnToggleSensor.setOnClickListener(v -> {
            boolean currentStatus = prefsManager.isNominalHidden();
            prefsManager.setHideNominal(!currentStatus);
            updateUI();
        });

        btnSettings.setOnClickListener(v -> showSettingsDialog());

        // TUTUP KEYBOARD SAAT ENTER DI KOLOM CARI
        etCariPelanggan.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(etCariPelanggan.getWindowToken(), 0);
                }
                etCariPelanggan.clearFocus();
                return true;
            }
            return false;
        });

        etCariPelanggan.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterKataKunci = s.toString().trim().toLowerCase();
                renderDaftarPelanggan();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnSimpanCatatan.setOnClickListener(v -> {
            String nama = etNamaPelanggan.getText().toString().trim();
            String nominalStr = etNominalInput.getText().toString().trim();

            if (TextUtils.isEmpty(nama) || TextUtils.isEmpty(nominalStr)) {
                Toast.makeText(this, "Isi nama dan nominal utang!", Toast.LENGTH_SHORT).show();
                return;
            }

            int nominal = Integer.parseInt(nominalStr);
            String tanggalSekarang = new SimpleDateFormat("dd MMM, HH:mm", new Locale("in", "ID")).format(new Date());

            dbHelper.tambahPelanggan(nama, nominal, tanggalSekarang);

            etNamaPelanggan.setText("");
            etNominalInput.setText("");
            Toast.makeText(this, "Catatan utang " + nama + " tersimpan!", Toast.LENGTH_SHORT).show();

            updateUI();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("⚙️ Pengaturan Aplikasi");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        final EditText inputPemilik = new EditText(this);
        inputPemilik.setHint("Nama Sapaan (misal: Kiki)");
        inputPemilik.setText(prefsManager.getNamaPemilik());
        layout.addView(inputPemilik);

        final CheckBox cbDarkMode = new CheckBox(this);
        cbDarkMode.setText("Aktifkan Mode Gelap (Kontras Tinggi)");
        cbDarkMode.setChecked(prefsManager.isDarkMode());
        cbDarkMode.setPadding(0, 20, 0, 0);
        layout.addView(cbDarkMode);

        builder.setView(layout);

        builder.setPositiveButton("Simpan", (dialog, which) -> {
            String p = inputPemilik.getText().toString().trim();
            if (!p.isEmpty()) prefsManager.setNamaPemilik(p);
            prefsManager.setDarkMode(cbDarkMode.isChecked());

            Toast.makeText(this, "Pengaturan disimpan ke SharedPreferences!", Toast.LENGTH_SHORT).show();
            updateUI();
        });

        builder.setNegativeButton("Batal", null);
        builder.show();
    }

    private void updateUI() {
        boolean isHidden = prefsManager.isNominalHidden();
        boolean isDark = prefsManager.isDarkMode();

        tvNamaPemilik.setText("Halo, " + prefsManager.getNamaPemilik() + "! 👋");

        if (isDark) {
            mainLayout.setBackgroundColor(Color.parseColor("#000000"));
            cardHeader.setCardBackgroundColor(Color.parseColor("#18181B"));
            cardFormTambah.setCardBackgroundColor(Color.parseColor("#18181B"));

            lblTambah.setTextColor(Color.parseColor("#FFFFFF"));
            tvLabelTotal.setTextColor(Color.parseColor("#FDE047"));

            etNamaPelanggan.setTextColor(Color.parseColor("#FFFFFF"));
            etNominalInput.setTextColor(Color.parseColor("#FFFFFF"));
            etCariPelanggan.setTextColor(Color.parseColor("#FFFFFF"));

            tilNama.setHintTextColor(ColorStateList.valueOf(Color.parseColor("#FDE047")));
            tilNominal.setHintTextColor(ColorStateList.valueOf(Color.parseColor("#FDE047")));
            tilCari.setHintTextColor(ColorStateList.valueOf(Color.parseColor("#FDE047")));

            btnSimpanCatatan.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#16A34A")));
        } else {
            mainLayout.setBackgroundColor(Color.parseColor("#F0F9FF"));
            cardHeader.setCardBackgroundColor(Color.parseColor("#0284C7"));
            cardFormTambah.setCardBackgroundColor(Color.WHITE);

            lblTambah.setTextColor(Color.parseColor("#0F172A"));
            tvLabelTotal.setTextColor(Color.parseColor("#E0F2FE"));

            etNamaPelanggan.setTextColor(Color.BLACK);
            etNominalInput.setTextColor(Color.BLACK);
            etCariPelanggan.setTextColor(Color.BLACK);

            tilNama.setHintTextColor(ColorStateList.valueOf(Color.parseColor("#64748B")));
            tilNominal.setHintTextColor(ColorStateList.valueOf(Color.parseColor("#64748B")));
            tilCari.setHintTextColor(ColorStateList.valueOf(Color.parseColor("#64748B")));

            btnSimpanCatatan.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0284C7")));
        }

        listPelanggan = dbHelper.getAllPelanggan();

        int totalHutang = 0;
        for (Pelanggan p : listPelanggan) {
            totalHutang += p.hutang;
        }

        if (isHidden) {
            tvTotalHutang.setText("Rp *******");
            btnToggleSensor.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        } else {
            tvTotalHutang.setText(formatRupiah(totalHutang));
            btnToggleSensor.setImageResource(android.R.drawable.ic_menu_view);
        }

        renderDaftarPelanggan();
    }

    private void renderDaftarPelanggan() {
        llDaftarPelanggan.removeAllViews();
        boolean isHidden = prefsManager.isNominalHidden();
        boolean isDark = prefsManager.isDarkMode();

        for (Pelanggan p : listPelanggan) {

            if (!filterKataKunci.isEmpty() && !p.nama.toLowerCase().contains(filterKataKunci)) {
                continue;
            }

            CardView card = new CardView(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 0, 0, 16);
            card.setLayoutParams(cardParams);
            card.setRadius(20);
            card.setCardElevation(3);
            card.setCardBackgroundColor(isDark ? Color.parseColor("#18181B") : Color.WHITE);

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(32, 32, 32, 32);

            // 1. BARIS HEADER CARD: IKON PROFIL + NAMA + TANGGAL
            LinearLayout headerRow = new LinearLayout(this);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setGravity(Gravity.CENTER_VERTICAL);

            ImageView imgAvatar = new ImageView(this);
            imgAvatar.setImageResource(android.R.drawable.ic_menu_myplaces); // Ikon Avatar Kontak
            imgAvatar.setColorFilter(isDark ? Color.parseColor("#38BDF8") : Color.parseColor("#0284C7"));
            LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(48, 48);
            avatarParams.setMargins(0, 0, 16, 0);
            imgAvatar.setLayoutParams(avatarParams);

            LinearLayout nameBox = new LinearLayout(this);
            nameBox.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            nameBox.setLayoutParams(nameParams);

            TextView tvNama = new TextView(this);
            tvNama.setText(p.nama);
            tvNama.setTextSize(17);
            tvNama.setTextColor(isDark ? Color.parseColor("#FFFFFF") : Color.parseColor("#0F172A"));
            tvNama.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView tvTanggal = new TextView(this);
            tvTanggal.setText("🕒 " + (p.tanggal != null ? p.tanggal : "Hari ini"));
            tvTanggal.setTextSize(12);
            tvTanggal.setTextColor(isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B"));

            nameBox.addView(tvNama);
            nameBox.addView(tvTanggal);

            headerRow.addView(imgAvatar);
            headerRow.addView(nameBox);

            // 2. NOMINAL SISA HUTANG
            TextView tvHutang = new TextView(this);
            tvHutang.setText("Sisa Hutang: " + (isHidden ? "Rp *******" : formatRupiah(p.hutang)));
            tvHutang.setTextColor(isDark ? Color.parseColor("#F87171") : Color.parseColor("#EF4444"));
            tvHutang.setTextSize(15);
            tvHutang.setTypeface(null, android.graphics.Typeface.BOLD);
            tvHutang.setPadding(0, 12, 0, 12);

            // 3. INPUT AKSINYA
            EditText etAksi = new EditText(this);
            etAksi.setHint("Nominal Transaksi (Rp)");
            etAksi.setTextColor(isDark ? Color.WHITE : Color.BLACK);
            etAksi.setHintTextColor(isDark ? Color.parseColor("#A1A1AA") : Color.GRAY);
            etAksi.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

            // 4. TOMBOL UTANG & CICIL
            LinearLayout btnLayout = new LinearLayout(this);
            btnLayout.setOrientation(LinearLayout.HORIZONTAL);
            btnLayout.setPadding(0, 12, 0, 0);

            Button btnTambah = new Button(this);
            btnTambah.setText("+ Utang");
            btnTambah.setBackgroundTintList(ColorStateList.valueOf(isDark ? Color.parseColor("#2563EB") : Color.parseColor("#0284C7")));
            btnTambah.setTextColor(Color.WHITE);
            btnTambah.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams btnParams1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            btnParams1.setMargins(0, 0, 8, 0);
            btnTambah.setLayoutParams(btnParams1);

            Button btnCicil = new Button(this);
            btnCicil.setText("Bayar / Cicil");
            btnCicil.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#16A34A")));
            btnCicil.setTextColor(Color.WHITE);
            btnCicil.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams btnParams2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            btnParams2.setMargins(8, 0, 0, 0);
            btnCicil.setLayoutParams(btnParams2);

            btnTambah.setOnClickListener(v -> {
                String val = etAksi.getText().toString().trim();
                if (!val.isEmpty()) {
                    int hutangBaru = p.hutang + Integer.parseInt(val);
                    dbHelper.updateHutang(p.id, hutangBaru);
                    Toast.makeText(this, "Hutang " + p.nama + " diperbarui!", Toast.LENGTH_SHORT).show();
                    updateUI();
                }
            });

            btnCicil.setOnClickListener(v -> {
                String val = etAksi.getText().toString().trim();
                if (!val.isEmpty()) {
                    int bayar = Integer.parseInt(val);
                    if (bayar >= p.hutang) {
                        dbHelper.hapusPelanggan(p.id);
                        Toast.makeText(this, "Hutang " + p.nama + " LUNAS!", Toast.LENGTH_LONG).show();
                    } else {
                        int hutangSisa = p.hutang - bayar;
                        dbHelper.updateHutang(p.id, hutangSisa);
                        Toast.makeText(this, "Cicilan " + p.nama + " berhasil!", Toast.LENGTH_SHORT).show();
                    }
                    updateUI();
                }
            });

            btnLayout.addView(btnTambah);
            btnLayout.addView(btnCicil);

            layout.addView(headerRow);
            layout.addView(tvHutang);
            layout.addView(etAksi);
            layout.addView(btnLayout);

            card.addView(layout);
            llDaftarPelanggan.addView(card);
        }
    }

    private String formatRupiah(int number) {
        Locale localeID = new Locale("in", "ID");
        NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(localeID);
        return formatRupiah.format(number).replace(",00", "");
    }
}