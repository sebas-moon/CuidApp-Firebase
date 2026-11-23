package com.santo_tomas.cuidapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.santo_tomas.cuidapp.model.RegistroDiario;
import com.santo_tomas.cuidapp.viewmodel.RegistroViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/*
 * NUEVO - ADAPTER de la lista de registros diarios (item_registro.xml).
 * Cada tarjeta muestra la fecha, un resumen de los valores medidos y si hubo ALERTAS.
 * Los días con alertas se marcan con borde y texto rojo para verlos de un vistazo.
 */
public class RegistroAdapter extends RecyclerView.Adapter<RegistroAdapter.RegistroViewHolder> {

    public interface OnRegistroClick {
        void onClick(RegistroDiario registro);
    }

    private final List<RegistroDiario> datos = new ArrayList<>();
    private final OnRegistroClick listener;

    public RegistroAdapter(OnRegistroClick listener) {
        this.listener = listener;
    }

    public void setDatos(List<RegistroDiario> nuevos) {
        datos.clear();
        if (nuevos != null) datos.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RegistroViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_registro, parent, false);
        return new RegistroViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull RegistroViewHolder holder, int position) {
        RegistroDiario r = datos.get(position);
        Context ctx = holder.itemView.getContext();

        holder.tvFecha.setText("📅 " + RegistroViewModel.fechaLegible(r.getFecha()));
        holder.tvResumen.setText(resumen(r));
        holder.tvBloqueo.setText(r.esEditable()
                ? R.string.registro_item_editable
                : R.string.registro_item_bloqueado);

        if (r.tieneAlertas()) {
            holder.tvEstado.setText(ctx.getString(R.string.registro_item_alertas,
                    String.join(" · ", r.getAlertas())));
            holder.tvEstado.setTextColor(ContextCompat.getColor(ctx, R.color.accent_move));
            holder.card.setStrokeColor(ContextCompat.getColor(ctx, R.color.accent_move));
        } else {
            holder.tvEstado.setText(R.string.registro_item_ok);
            holder.tvEstado.setTextColor(ContextCompat.getColor(ctx, R.color.accent_exercise));
            holder.card.setStrokeColor(ContextCompat.getColor(ctx, R.color.chip_background));
        }

        holder.itemView.setOnClickListener(v -> listener.onClick(r));
    }

    // Arma "PA 128/82 · FC 74 · Glucemia 112 · Dolor 2/10" solo con los valores que se midieron
    private String resumen(RegistroDiario r) {
        List<String> partes = new ArrayList<>();
        if (r.getSistolica() != null && r.getDiastolica() != null) {
            partes.add("PA " + r.getSistolica() + "/" + r.getDiastolica());
        }
        if (r.getFrecuenciaCardiaca() != null) partes.add("FC " + r.getFrecuenciaCardiaca());
        if (r.getGlucemia() != null) partes.add("Glucemia " + r.getGlucemia());
        if (r.getSaturacion() != null) partes.add("SpO2 " + r.getSaturacion() + "%");
        if (r.getFrecuenciaRespiratoria() != null) partes.add("FR " + r.getFrecuenciaRespiratoria());
        if (r.getPesoDiario() != null) partes.add(String.format(Locale.US, "Peso %.1f kg", r.getPesoDiario()));
        if (r.getDiuresis() != null) partes.add("Diuresis " + r.getDiuresis() + " ml");
        partes.add("Dolor " + r.getDolor() + "/10");
        return String.join(" · ", partes);
    }

    @Override
    public int getItemCount() {
        return datos.size();
    }

    static class RegistroViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView tvFecha, tvResumen, tvEstado, tvBloqueo;

        RegistroViewHolder(@NonNull View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            tvFecha = itemView.findViewById(R.id.tvFecha);
            tvResumen = itemView.findViewById(R.id.tvResumen);
            tvEstado = itemView.findViewById(R.id.tvEstado);
            tvBloqueo = itemView.findViewById(R.id.tvBloqueo);
        }
    }
}
