package com.jninfo.partograma.partograma;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.jninfo.partograma.partograma.data.Paciente;

import java.util.ArrayList;
import java.util.List;

/** Adapter da lista funcional de pacientes (tela {@link PacientesActivity}). */
public class PacientesAdapter extends RecyclerView.Adapter<PacientesAdapter.PacienteViewHolder> {

    public interface OnPacienteClickListener {
        void onPacienteClicado(Paciente paciente);
    }

    public interface OnPacienteLongClickListener {
        void onPacienteSegurado(Paciente paciente);
    }

    private final List<Paciente> pacientes = new ArrayList<>();
    private final OnPacienteClickListener listener;
    private final OnPacienteLongClickListener longClickListener;

    public PacientesAdapter(OnPacienteClickListener listener, OnPacienteLongClickListener longClickListener) {
        this.listener = listener;
        this.longClickListener = longClickListener;
    }

    public void atualizar(List<Paciente> novaLista) {
        pacientes.clear();
        pacientes.addAll(novaLista);
        notifyDataSetChanged();
    }

    public boolean estaVazia() {
        return pacientes.isEmpty();
    }

    @NonNull
    @Override
    public PacienteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_paciente_card, parent, false);
        return new PacienteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PacienteViewHolder holder, int position) {
        Paciente paciente = pacientes.get(position);
        holder.bind(paciente, listener, longClickListener);
    }

    @Override
    public int getItemCount() {
        return pacientes.size();
    }

    static class PacienteViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtNome;
        private final TextView txtProntuarioIdade;
        private final TextView txtStatus;
        private final TextView txtUltimoAcesso;

        PacienteViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNome = itemView.findViewById(R.id.txtNomePaciente);
            txtProntuarioIdade = itemView.findViewById(R.id.txtProntuarioIdade);
            txtStatus = itemView.findViewById(R.id.txtStatusPaciente);
            txtUltimoAcesso = itemView.findViewById(R.id.txtUltimoAcesso);
        }

        void bind(Paciente paciente, OnPacienteClickListener listener, OnPacienteLongClickListener longClickListener) {
            txtNome.setText(paciente.getNome());

            int idade = paciente.getIdadeAnos();
            String prontuario = paciente.getProntuario() != null ? paciente.getProntuario() : "-";
            if (idade >= 0) {
                txtProntuarioIdade.setText("Prontuário: " + prontuario + "  •  " + idade + " anos");
            } else {
                txtProntuarioIdade.setText("Prontuário: " + prontuario);
            }

            String status = paciente.getStatus() != null
                    ? paciente.getStatus()
                    : itemView.getContext().getString(R.string.pacientes_status_avaliacao);
            txtStatus.setText(status);
            aplicarCorStatus(status);

            if (paciente.getUltimoAcesso() != null) {
                CharSequence dataFormatada = DateFormat.format("dd/MM/yyyy - HH:mm", paciente.getUltimoAcesso());
                txtUltimoAcesso.setText("Último acesso: " + dataFormatada);
                txtUltimoAcesso.setVisibility(View.VISIBLE);
            } else {
                txtUltimoAcesso.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPacienteClicado(paciente);
                }
            });
            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onPacienteSegurado(paciente);
                }
                return true;
            });
        }

        private void aplicarCorStatus(String status) {
            android.content.Context context = itemView.getContext();
            if (context.getString(R.string.pacientes_status_trabalho_ativo).equals(status)) {
                txtStatus.setBackgroundResource(R.drawable.bg_badge_pink);
                txtStatus.setTextColor(ContextCompat.getColor(context, R.color.primaryPink));
            } else if (context.getString(R.string.pacientes_status_fase_latente).equals(status)) {
                txtStatus.setBackgroundResource(R.drawable.bg_badge_lilac);
                txtStatus.setTextColor(ContextCompat.getColor(context, R.color.softPurple));
            } else {
                txtStatus.setBackgroundResource(R.drawable.bg_badge_teal);
                txtStatus.setTextColor(ContextCompat.getColor(context, R.color.softTeal));
            }
        }
    }
}
