package com.santo_tomas.cuidapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.santo_tomas.cuidapp.model.Paciente;
import com.santo_tomas.cuidapp.util.Condiciones;

import java.util.ArrayList;
import java.util.List;

/*
 * NUEVO - ADAPTER del RecyclerView: dibuja una tarjeta (item_paciente.xml) por cada paciente
 * y avisa a la Activity cuando se toca una.
 */
public class PacienteAdapter extends RecyclerView.Adapter<PacienteAdapter.PacienteViewHolder> {

    // Interfaz para comunicar el clic hacia PacientesActivity
    public interface OnPacienteClick {
        void onClick(Paciente paciente);
    }

    private final List<Paciente> datos = new ArrayList<>();
    private final OnPacienteClick listener;

    public PacienteAdapter(OnPacienteClick listener) {
        this.listener = listener;
    }

    // Reemplaza la lista completa y redibuja
    public void setDatos(List<Paciente> nuevos) {
        datos.clear();
        if (nuevos != null) datos.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PacienteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_paciente, parent, false);
        return new PacienteViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull PacienteViewHolder holder, int position) {
        Paciente p = datos.get(position);
        Context ctx = holder.itemView.getContext();

        String nombre = p.getNombre() == null ? "" : p.getNombre();
        holder.tvInicial.setText(nombre.isEmpty() ? "?" : nombre.substring(0, 1).toUpperCase());
        holder.tvNombre.setText(nombre);
        holder.tvDetalle.setText(ctx.getString(R.string.paciente_item_detalle, p.getRut(), p.getEdad()));
        holder.tvSalud.setText(ctx.getString(R.string.paciente_item_salud,
                p.getTipoSangre(), Condiciones.texto(p.getCondiciones())));

        holder.itemView.setOnClickListener(v -> listener.onClick(p));
    }

    @Override
    public int getItemCount() {
        return datos.size();
    }

    // ViewHolder: guarda las referencias a las vistas de UNA tarjeta
    static class PacienteViewHolder extends RecyclerView.ViewHolder {
        final TextView tvInicial, tvNombre, tvDetalle, tvSalud;

        PacienteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInicial = itemView.findViewById(R.id.tvInicial);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvDetalle = itemView.findViewById(R.id.tvDetalle);
            tvSalud = itemView.findViewById(R.id.tvSalud);
        }
    }
}
