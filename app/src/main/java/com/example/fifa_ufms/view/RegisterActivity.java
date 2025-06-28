package com.example.fifa_ufms.view;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Patterns; // <-- 1. IMPORT ADICIONADO
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fifa_ufms.R;
import com.example.fifa_ufms.database.CampeonatoDatabase;
import com.example.fifa_ufms.entities.User;
import com.example.fifa_ufms.utils.PasswordHasher;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private EditText etName, etEmail, etPassword;
    private ImageView ivProfilePhoto;
    private Button btnRegister;
    private Uri photoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);

        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);

        ivProfilePhoto.setOnClickListener(v -> openGallery());
        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            photoUri = data.getData();
            ivProfilePhoto.setImageURI(photoUri);
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

        // validacao de email. Sem um formato de email valido, o usuario nao pode se cadastrar
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Por favor, insira um formato de e-mail válido");
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
            // verifica se o e-mail já existe no banco
            User existingUser = CampeonatoDatabase.getInstance(getApplicationContext()).userDao().getUserByEmail(email);

            runOnUiThread(() -> {
                if (existingUser != null) {
                    // Se o usuário já existe, mostra um erro
                    etEmail.setError("Este e-mail já está em uso!");
                    etEmail.requestFocus();
                    Toast.makeText(RegisterActivity.this, "Este e-mail já está cadastrado.", Toast.LENGTH_LONG).show();
                } else {
                    // Se não existe, prossegue com a inserção
                    executor.execute(() -> {
                        CampeonatoDatabase.getInstance(getApplicationContext()).userDao().insert(newUser);
                        runOnUiThread(() -> {
                            Toast.makeText(RegisterActivity.this, "Usuário cadastrado com sucesso!", Toast.LENGTH_LONG).show();
                            finish();
                        });
                    });
                }
            });
        });
    }
}