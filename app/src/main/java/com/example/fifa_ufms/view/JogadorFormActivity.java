package com.example.fifa_ufms.view;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Patterns;
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
import com.example.fifa_ufms.databinding.ActivityJogadorFormBinding;
import com.example.fifa_ufms.entities.Jogador;

import java.io.File;
import java.io.FileOutputStream; // Adicionado para copiar o arquivo
import java.io.IOException;
import java.io.InputStream;    // Adicionado para copiar o arquivo
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

public class JogadorFormActivity extends AppCompatActivity {

    public static final String EXTRA_ID_JOGADOR = "extra_id_jogador";
    public static final String EXTRA_NOME_JOGADOR = "extra_nome_jogador";

    private static final int REQUEST_IMAGE_PICK = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private static final int REQUEST_PERMISSION_CODE_GALLERY = 1001;
    private static final int REQUEST_PERMISSION_CODE_CAMERA = 1002;

    // Esta URI agora apontará para o arquivo copiado no armazenamento privado
    private String imagemUriSelecionada = null;
    private Uri currentPhotoUri; // URI temporária para a imagem capturada pela câmera (antes de ser copiada)

    private ActivityJogadorFormBinding binding;
    private int jogadorId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityJogadorFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.backButton.setOnClickListener(v -> finish());

        binding.imageJogador.setOnClickListener(v -> showImageSourceDialog());

        Intent intent = getIntent();
        jogadorId = intent.getIntExtra(EXTRA_ID_JOGADOR, -1);
        String nomeJogador = intent.getStringExtra(EXTRA_NOME_JOGADOR);

        if (jogadorId != -1 && nomeJogador != null) {
            // Modo edição
            binding.titleText.setText("Editar Jogador");
            binding.buttonSave.setText("Salvar");

            binding.edittextNomeJogador.setText(nomeJogador);
            binding.editNickname.setText(intent.getStringExtra("nickname"));
            binding.editEmail.setText(intent.getStringExtra("email"));
            binding.editDataNascimento.setText(intent.getStringExtra("dataNascimento"));
            binding.editNumeroGols.setText(String.valueOf(intent.getIntExtra("numeroGols", 0)));
            binding.editNumeroAmarelos.setText(String.valueOf(intent.getIntExtra("numeroAmarelos", 0)));
            binding.editNumeroVermelhos.setText(String.valueOf(intent.getIntExtra("numeroVermelhos", 0)));
            binding.editIdTime.setText(String.valueOf(intent.getIntExtra("idTime", 0)));

            String imagemUri = intent.getStringExtra("imagemUri");
            if (imagemUri != null && !imagemUri.isEmpty()) {
                // Tentamos carregar a imagem do URI salvo.
                // Se for um caminho de arquivo interno, funcionará.
                // Se for um URI de conteúdo antigo que não foi copiado, pode falhar.
                // No futuro, todos os URIs deverão ser caminhos de arquivo internos.
                try {
                    Uri uriToLoad = Uri.parse(imagemUri);
                    binding.imageJogador.setImageURI(uriToLoad);
                    imagemUriSelecionada = imagemUri; // Mantém o URI salvo para possível atualização
                } catch (Exception e) {
                    Log.e("JogadorFormActivity", "Erro ao carregar imagem salva: " + imagemUri, e);
                    binding.imageJogador.setImageResource(R.drawable.ic_user); // Fallback para ícone padrão
                    imagemUriSelecionada = null; // Limpa o URI se não puder ser carregado
                }
            } else {
                binding.imageJogador.setImageResource(R.drawable.ic_user); // Fallback para ícone padrão
                imagemUriSelecionada = null;
            }

            binding.buttonSave.setOnClickListener(v -> salvarJogador(false));

        } else {
            // Modo cadastro
            binding.titleText.setText("Cadastrar Jogador");
            binding.buttonSave.setText("Cadastrar");
            binding.imageJogador.setImageResource(R.drawable.ic_user); // Ícone padrão para novo jogador

            binding.buttonSave.setOnClickListener(v -> salvarJogador(true));
        }
    }

    private void showImageSourceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Escolher Imagem")
                .setItems(new CharSequence[]{"Galeria", "Câmera"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: // Galeria
                                if (hasGalleryPermission()) {
                                    openGallery();
                                } else {
                                    requestGalleryPermission();
                                }
                                break;
                            case 1: // Câmera
                                if (hasCameraPermission()) {
                                    dispatchTakePictureIntent();
                                } else {
                                    requestCameraPermission();
                                }
                                break;
                        }
                    }
                })
                .show();
    }

    private boolean hasGalleryPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestGalleryPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                    REQUEST_PERMISSION_CODE_GALLERY);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION_CODE_GALLERY);
        }
    }

    private void openGallery() {
        Intent intentImagem = new Intent(Intent.ACTION_PICK);
        intentImagem.setType("image/*");
        startActivityForResult(intentImagem, REQUEST_IMAGE_PICK);
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                REQUEST_PERMISSION_CODE_CAMERA);
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e("JogadorFormActivity", "Erro ao criar arquivo de imagem para câmera: " + ex.getMessage());
                Toast.makeText(this, "Erro ao preparar foto para câmera.", Toast.LENGTH_SHORT).show();
            }
            if (photoFile != null) {
                currentPhotoUri = FileProvider.getUriForFile(this,
                        "com.example.fifa_ufms.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        } else {
            Toast.makeText(this, "Nenhum aplicativo de câmera encontrado.", Toast.LENGTH_SHORT).show();
        }
    }

    // Cria um arquivo temporário no diretório de arquivos internos para a câmera
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(null); // Usa o diretório de arquivos externos para que a câmera possa salvar.
        // Depois, copiaremos para o interno se necessário.
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        return image;
    }

    // Método para copiar a URI da imagem para o armazenamento privado do aplicativo
    private String copyImageToInternalStorage(Uri sourceUri) {
        if (sourceUri == null) {
            return null;
        }

        try {
            InputStream inputStream = getContentResolver().openInputStream(sourceUri);
            if (inputStream == null) {
                Log.e("JogadorFormActivity", "Não foi possível abrir InputStream para a URI: " + sourceUri);
                return null;
            }

            // Diretório para salvar as imagens dos jogadores (dentro do armazenamento interno do app)
            File appSpecificDir = new File(getFilesDir(), "player_images");
            if (!appSpecificDir.exists()) {
                appSpecificDir.mkdirs(); // Cria o diretório se ele não existir
            }

            // Cria um nome de arquivo único
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "player_image_" + timeStamp + ".jpg";
            File destinationFile = new File(appSpecificDir, fileName);

            FileOutputStream outputStream = new FileOutputStream(destinationFile);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.close();
            inputStream.close();

            // Retorna o URI do arquivo salvo localmente
            return Uri.fromFile(destinationFile).toString();

        } catch (Exception e) {
            Log.e("JogadorFormActivity", "Erro ao copiar imagem para o armazenamento interno: " + e.getMessage(), e);
            Toast.makeText(this, "Erro ao salvar imagem localmente.", Toast.LENGTH_SHORT).show();
            return null;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION_CODE_GALLERY) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Permissão negada para acessar galeria", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_PERMISSION_CODE_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Permissão negada para usar a câmera", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void salvarJogador(boolean isNew) {
        String nome = binding.edittextNomeJogador.getText().toString().trim();
        String nickname = binding.editNickname.getText().toString().trim();
        String email = binding.editEmail.getText().toString().trim();
        String dataNascimento = binding.editDataNascimento.getText().toString().trim();
        String numeroGolsStr = binding.editNumeroGols.getText().toString().trim();
        String numeroAmarelosStr = binding.editNumeroAmarelos.getText().toString().trim();
        String numeroVermelhosStr = binding.editNumeroVermelhos.getText().toString().trim();
        String idTimeStr = binding.editIdTime.getText().toString().trim();

        if (nome.isEmpty()) {
            binding.edittextNomeJogador.setError("O nome é obrigatório!");
            binding.edittextNomeJogador.requestFocus();
            return;
        }
        if (nickname.isEmpty()) {
            binding.editNickname.setError("O nickname é obrigatório!");
            binding.editNickname.requestFocus();
            return;
        }
        if (email.isEmpty()) {
            binding.editEmail.setError("O e-mail é obrigatório!");
            binding.editEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.editEmail.setError("Por favor, insira um formato de e-mail válido!");
            binding.editEmail.requestFocus();
            return;
        }
        if (dataNascimento.isEmpty()) {
            binding.editDataNascimento.setError("A data de nascimento é obrigatória!");
            binding.editDataNascimento.requestFocus();
            return;
        }
        if (numeroGolsStr.isEmpty()) {
            binding.editNumeroGols.setError("O número de gols é obrigatório!");
            binding.editNumeroGols.requestFocus();
            return;
        }
        if (numeroAmarelosStr.isEmpty()) {
            binding.editNumeroAmarelos.setError("O número de cartões amarelos é obrigatório!");
            binding.editNumeroAmarelos.requestFocus();
            return;
        }
        if (numeroVermelhosStr.isEmpty()) {
            binding.editNumeroVermelhos.setError("O número de cartões vermelhos é obrigatório!");
            binding.editNumeroVermelhos.requestFocus();
            return;
        }
        if (idTimeStr.isEmpty()) {
            binding.editIdTime.setError("O ID do time é obrigatório!");
            binding.editIdTime.requestFocus();
            return;
        }

        int numeroGols = parseIntOrZero(numeroGolsStr);
        int numeroAmarelos = parseIntOrZero(numeroAmarelosStr);
        int numeroVermelhos = parseIntOrZero(numeroVermelhosStr);
        int idTime = parseIntOrZero(idTimeStr);

        Jogador jogador = new Jogador(
                numeroVermelhos,
                numeroAmarelos,
                numeroGols,
                dataNascimento,
                email,
                nickname,
                nome,
                idTime,
                imagemUriSelecionada != null ? imagemUriSelecionada : "" // Usa o URI copiado ou vazio
        );

        if (!isNew) {
            jogador.setIdJogador(jogadorId);
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            CampeonatoDatabase db = CampeonatoDatabase.getInstance(getApplicationContext());
            if (isNew) {
                db.jogadorDao().inserirJogador(jogador);
            } else {
                db.jogadorDao().atualizarJogador(jogador);
            }

            runOnUiThread(() -> {
                Toast.makeText(this, (isNew ? "Jogador cadastrado: " : "Jogador atualizado: ") + nome, Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    private int parseIntOrZero(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            Uri originalUri = null;
            if (requestCode == REQUEST_IMAGE_PICK && data != null && data.getData() != null) {
                originalUri = data.getData();
            } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                originalUri = currentPhotoUri; // URI da imagem capturada pela câmera
            }

            if (originalUri != null) {
                // Copia a imagem para o armazenamento interno do aplicativo
                String copiedUriString = copyImageToInternalStorage(originalUri);
                if (copiedUriString != null) {
                    imagemUriSelecionada = copiedUriString;
                    binding.imageJogador.setImageURI(Uri.parse(copiedUriString));
                    Toast.makeText(this, "Imagem salva localmente.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Falha ao copiar imagem. Por favor, tente novamente.", Toast.LENGTH_LONG).show();
                    // Limpa a seleção se a cópia falhar
                    imagemUriSelecionada = null;
                    binding.imageJogador.setImageResource(R.drawable.ic_user);
                }
            } else {
                Toast.makeText(this, "Nenhuma imagem selecionada.", Toast.LENGTH_SHORT).show();
            }
        } else if (resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "Seleção de imagem cancelada.", Toast.LENGTH_SHORT).show();
        }
    }
}