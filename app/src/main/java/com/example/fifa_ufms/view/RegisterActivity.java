package com.example.fifa_ufms.view;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.example.fifa_ufms.R;

public class RegisterActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private EditText etName, etEmail, etPassword;
    private ImageView ivProfilePhoto;
    private Button btnRegister;
    private Uri photoUri;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        ivProfilePhoto = findViewById(R.id.btnRegister);

        ivProfilePhoto.setOnClickListener(v -> openGallery());
        btnRegister.setOnClickListener(v -> registerUser());
    }
}
