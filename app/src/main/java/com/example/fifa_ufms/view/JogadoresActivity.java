package com.example.fifa_ufms.view;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fifa_ufms.R;
import com.example.fifa_ufms.adapter.JogadoresAdapter;
import com.example.fifa_ufms.database.CampeonatoDatabase;
import com.example.fifa_ufms.entities.Jogador;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class JogadoresActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private JogadoresAdapter adapter;
    private List<Jogador> jogadores = new ArrayList<>(); // Initialize the list

    private static final int PERMISSION_REQUEST_CODE_STORAGE = 200;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jogadores);

        SharedPreferences prefs = getSharedPreferences("usuario_prefs", MODE_PRIVATE);
        boolean isAdmin = prefs.getBoolean("tipo_usuario", false);

        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recycler_jogadores);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Create and set the adapter with an empty list
        adapter = new JogadoresAdapter(this, jogadores, jogador -> {
            Intent intent = new Intent(JogadoresActivity.this, JogadorFormActivity.class);
            intent.putExtra(JogadorFormActivity.EXTRA_ID_JOGADOR, jogador.getIdJogador());
            intent.putExtra(JogadorFormActivity.EXTRA_NOME_JOGADOR, jogador.getNome());
            intent.putExtra("nickname", jogador.getNickname());
            intent.putExtra("email", jogador.getEmail());
            intent.putExtra("dataNascimento", jogador.getDataNascimento());
            intent.putExtra("numeroGols", jogador.getNumeroGols());
            intent.putExtra("numeroAmarelos", jogador.getNumeroAmarelos());
            intent.putExtra("numeroVermelhos", jogador.getNumeroVermelhos());
            intent.putExtra("idTime", jogador.getIdTime());
            intent.putExtra("imagemUri", jogador.getImagemUri());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        Button addButton = findViewById(R.id.button_add_jogador);
        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(JogadoresActivity.this, JogadorFormActivity.class);
            startActivity(intent);
        });

        if (!isAdmin) {
            addButton.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkStoragePermission()) {
            loadJogadoresFromDatabase();
        } else {
            requestStoragePermission();
        }
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                    PERMISSION_REQUEST_CODE_STORAGE);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadJogadoresFromDatabase();
            } else {
                Toast.makeText(this, "Permissão de armazenamento negada. Imagens dos jogadores podem não ser exibidas.", Toast.LENGTH_LONG).show();
                loadJogadoresFromDatabase();
            }
        }
    }

    private void loadJogadoresFromDatabase() {
        Executors.newSingleThreadExecutor().execute(() -> {
            CampeonatoDatabase db = CampeonatoDatabase.getInstance(getApplicationContext());
            List<Jogador> updatedJogadores = db.jogadorDao().listarTodosJogadores();

            runOnUiThread(() -> {
                jogadores.clear();
                jogadores.addAll(updatedJogadores);
                adapter.notifyDataSetChanged();
            });
        });
    }
}