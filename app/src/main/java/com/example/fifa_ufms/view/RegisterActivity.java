package com.example.fifa_ufms.view;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.fifa_ufms.R;
import com.example.fifa_ufms.database.CampeonatoDatabase;
import com.example.fifa_ufms.entities.User;
import com.example.fifa_ufms.utils.PasswordHasher;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {

    // Constantes para requests
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_PICK_IMAGE = 2;
    private static final int REQUEST_CAMERA_PERMISSION = 101;

    private EditText etName, etEmail, etPassword;
    private ImageView ivProfilePhoto;
    private Button btnRegister;
    private Uri photoUri;
    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        CheckBox checkBoxAdmin = findViewById(R.id.checkBoxAdmin);

        // Ao clicar na foto, mostra um diálogo de escolha
        ivProfilePhoto.setOnClickListener(v -> showImageSourceDialog());
        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void showImageSourceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Escolha uma fonte para a imagem");
        builder.setItems(new CharSequence[]{"Tirar foto", "Escolher da Galeria"}, (dialog, which) -> {
            switch (which) {
                case 0: // Tirar foto
                    checkCameraPermissionAndDispatch();
                    break;
                case 1: // Escolher da Galeria
                    openGallery();
                    break;
            }
        });
        builder.show();
    }

    private void checkCameraPermissionAndDispatch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            dispatchTakePictureIntent();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Tratar erro
                Toast.makeText(this, "Erro ao criar o arquivo de imagem.", Toast.LENGTH_SHORT).show();
            }

            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(this,
                        "com.example.fifa_ufms.provider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Permissão da câmera negada.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                // A URI já foi definida no dispatchTakePictureIntent
                ivProfilePhoto.setImageURI(photoUri);
            } else if (requestCode == REQUEST_PICK_IMAGE && data != null && data.getData() != null) {
                photoUri = data.getData();
                ivProfilePhoto.setImageURI(photoUri);
            }
        }
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        CheckBox checkBoxAdmin = findViewById(R.id.checkBoxAdmin);

        if (name.isEmpty()) {
            etName.setError("O nome é obrigatório!");
            etName.requestFocus();
            return;
        }
        if (email.isEmpty()) {
            etEmail.setError("O e-mail é obrigatório!");
            etEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("A senha é obrigatória!");
            etPassword.requestFocus();
            return;
        }
        if (photoUri == null) {
            Toast.makeText(this, "Por favor, selecione uma foto.", Toast.LENGTH_SHORT).show();
            return;
        }

        String passwordHash = PasswordHasher.hashPassword(password);
        boolean permissao = checkBoxAdmin.isChecked();

        User newUser = new User();
        newUser.setName(name);
        newUser.setEmail(email);
        newUser.setPasswordHash(passwordHash);
        newUser.setPhotoUri(photoUri.toString());
        newUser.setAdmin(permissao);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            CampeonatoDatabase.getInstance(getApplicationContext()).userDao().insert(newUser);

            runOnUiThread(() -> {
                Toast.makeText(RegisterActivity.this, "Usuário cadastrado com sucesso!", Toast.LENGTH_LONG).show();
                finish();
            });
        });
    }
}