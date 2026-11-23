package com.santo_tomas.cuidapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.santo_tomas.cuidapp.model.Rutina;

import java.util.List;

/* NUEVO - ADAPTER de la lista de rutinas de ejercicio (item_rutina.xml). */
public class RutinaAdapter extends RecyclerView.Adapter<RutinaAdapter.VH> {

    public interface OnIniciar {
        void iniciar(Rutina rutina);
    }

    private final List<Rutina> rutinas;
    private final OnIniciar listener;

    public RutinaAdapter(List<Rutina> rutinas, OnIniciar listener) {
        this.rutinas = rutinas;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_rutina, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Rutina r = rutinas.get(position);
        h.tvEmoji.setText(r.emoji);
        h.tvNombre.setText(r.nombre);
        h.tvDescripcion.setText(r.descripcion);
        h.tvMinutos.setText(h.itemView.getContext().getString(R.string.ejercicio_minutos, r.minutos));
        h.btnIniciar.setOnClickListener(v -> listener.iniciar(r));
    }

    @Override
    public int getItemCount() {
        return rutinas.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvEmoji, tvNombre, tvDescripcion, tvMinutos;
        final MaterialButton btnIniciar;

        VH(@NonNull View v) {
            super(v);
            tvEmoji = v.findViewById(R.id.tvEmojiRutina);
            tvNombre = v.findViewById(R.id.tvNombreRutina);
            tvDescripcion = v.findViewById(R.id.tvDescripcionRutina);
            tvMinutos = v.findViewById(R.id.tvMinutosRutina);
            btnIniciar = v.findViewById(R.id.btnIniciarRutina);
        }
    }
}
