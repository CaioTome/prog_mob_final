package com.example.fifa_ufms.adapter;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // Import Glide
import com.example.fifa_ufms.R;
import com.example.fifa_ufms.database.CampeonatoDatabase;
import com.example.fifa_ufms.entities.Jogador;

import java.util.List;

public class JogadoresAdapter extends RecyclerView.Adapter<JogadoresAdapter.ViewHolder> {

    private final Context context;
    private final List<Jogador> jogadores;
    private final OnJogadorClickListener listener;

    public interface OnJogadorClickListener {
        void onJogadorClick(Jogador jogador);
    }

    public JogadoresAdapter(Context context, List<Jogador> jogadores, OnJogadorClickListener listener) {
        this.context = context;
        this.jogadores = jogadores;
        this.listener = listener;
    }

    @NonNull
    @Override
    public JogadoresAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_jogador, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JogadoresAdapter.ViewHolder holder, int position) {
        Jogador jogador = jogadores.get(position);

        holder.textNome.setText(jogador.getNome());
        holder.textNickname.setText(jogador.getNickname());
        holder.textEmail.setText(jogador.getEmail());
        holder.textDataNascimento.setText(jogador.getDataNascimento());
        holder.textGols.setText("Gols: " + jogador.getNumeroGols());
        holder.textCartoes.setText("Amarelos: " + jogador.getNumeroAmarelos() + " | Vermelhos: " + jogador.getNumeroVermelhos());

        String imageUriString = jogador.getImagemUri();

        if (imageUriString != null && !imageUriString.isEmpty()) {
            // Use Glide to load the image
            Glide.with(context)
                    .load(Uri.parse(imageUriString))
                    .placeholder(R.drawable.ic_user) // Show a placeholder while loading
                    .error(R.drawable.ic_user) // Show an error icon if it fails
                    .into(holder.imageJogador);
        } else {
            holder.imageJogador.setImageResource(R.drawable.ic_user);
        }

        boolean isAdmin = context.getSharedPreferences("usuario_prefs", Context.MODE_PRIVATE)
                .getBoolean("tipo_usuario", false);

        if (!isAdmin) {
            holder.buttonEdit.setVisibility(View.GONE);
            holder.buttonDelete.setVisibility(View.GONE);
        }

        holder.buttonEdit.setOnClickListener(v -> listener.onJogadorClick(jogador));

        holder.buttonDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Confirmar exclusão")
                    .setMessage("Deseja excluir este jogador e todas as partidas que ele jogou?")
                    .setPositiveButton("Sim", (dialog, which) -> {
                        CampeonatoDatabase db = CampeonatoDatabase.getInstance(context);
                        db.jogadorDao().deletarJogadorEPartidas(jogador.getNickname(), db);
                        jogadores.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, jogadores.size());
                        Toast.makeText(context, "Jogador e partidas deletados", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Não", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return jogadores.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textNome, textNickname, textEmail, textDataNascimento, textGols, textCartoes;
        ImageButton buttonEdit, buttonDelete;
        ImageView imageJogador;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textNome = itemView.findViewById(R.id.text_nome_jogador);
            textNickname = itemView.findViewById(R.id.text_nickname);
            textEmail = itemView.findViewById(R.id.text_email);
            textDataNascimento = itemView.findViewById(R.id.text_data_nascimento);
            textGols = itemView.findViewById(R.id.text_gols);
            textCartoes = itemView.findViewById(R.id.text_cartoes);
            buttonEdit = itemView.findViewById(R.id.button_edit_jogador);
            buttonDelete = itemView.findViewById(R.id.button_delete_jogador);
            imageJogador = itemView.findViewById(R.id.image_jogador_icon);
        }
    }
}