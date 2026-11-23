package com.santo_tomas.cuidapp;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/*
 * NUEVO - Pestaña "Navegador": accesos directos a sitios útiles para el cuidado.
 * A diferencia de la pestaña Web (WebView dentro de la app), aquí todo se abre en el
 * NAVEGADOR del teléfono con Intents IMPLÍCITOS:
 *   - ACTION_VIEW  + https://...  -> abre el sitio en Chrome (u otro navegador)
 *   - ACTION_WEB_SEARCH + texto    -> busca en la web lo que escriba el usuario
 *   - ACTION_DIAL  + tel:          -> Salud Responde (no necesita permiso de llamada)
 */
public class NavegadorActivity extends BaseActivity {

    /** Sitio: emoji, nombre, descripción y dirección (https o tel). */
    private static final String[][] SITIOS = {
            {"🌐", "CuidApp Web", "La versión web de CuidApp, en el navegador", "https://bksp.github.io/cuidApp-demo/"},
            {"🏥", "Ministerio de Salud (MINSAL)", "Campañas, vacunas y noticias de salud", "https://www.minsal.cl/"},
            {"💳", "FONASA", "Bonos, licencias y beneficios de salud", "https://www.fonasa.cl/"},
            {"👵", "SENAMA", "Servicio Nacional del Adulto Mayor: programas y apoyo", "https://www.senama.gob.cl/"},
            {"🏛️", "ChileAtiende", "Trámites del Estado: pensiones, certificados y más", "https://www.chileatiende.gob.cl/"},
            {"💊", "Instituto de Salud Pública (ISP)", "Información oficial sobre medicamentos", "https://www.ispch.cl/"},
            {"📞", "Salud Responde", "Orientación de salud por teléfono: 600 360 7777", "tel:6003607777"},
    };

    private TextInputLayout tilBuscar;
    private TextInputEditText etBuscar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navegador);
        InsetsUtil.aplicar(this);

        tilBuscar = findViewById(R.id.tilBuscarWeb);
        etBuscar = findViewById(R.id.etBuscarWeb);

        // Una tarjeta (item_enlace.xml) por cada sitio
        LinearLayout lista = findViewById(R.id.listaSitios);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (String[] sitio : SITIOS) {
            View tarjeta = inflater.inflate(R.layout.item_enlace, lista, false);
            ((TextView) tarjeta.findViewById(R.id.tvEmojiEnlace)).setText(sitio[0]);
            ((TextView) tarjeta.findViewById(R.id.tvNombreEnlace)).setText(sitio[1]);
            ((TextView) tarjeta.findViewById(R.id.tvDescripcionEnlace)).setText(sitio[2]);
            tarjeta.setOnClickListener(v -> abrir(sitio[3]));
            lista.addView(tarjeta);
        }

        // Buscar: con el botón o con la tecla "buscar" del teclado
        configurarBoton(R.id.btnBuscarWeb, v -> buscar());
        etBuscar.setOnEditorActionListener((v, accion, evento) -> {
            if (accion == EditorInfo.IME_ACTION_SEARCH) {
                buscar();
                return true;
            }
            return false;
        });
        configurarBoton(R.id.btnVolverNavegador, v -> finish());
    }

    // IMPLÍCITO: ACTION_DIAL para teléfonos, ACTION_VIEW para sitios web
    private void abrir(String destino) {
        String accion = destino.startsWith("tel:") ? Intent.ACTION_DIAL : Intent.ACTION_VIEW;
        iniciarSeguro(new Intent(accion, Uri.parse(destino)));
    }

    // IMPLÍCITO: ACTION_WEB_SEARCH abre el buscador con el texto escrito
    private void buscar() {
        String texto = etBuscar.getText() == null ? "" : etBuscar.getText().toString().trim();
        if (texto.isEmpty()) {
            tilBuscar.setError(getString(R.string.navegador_buscar_vacio));
            return;
        }
        tilBuscar.setError(null);
        Intent busqueda = new Intent(Intent.ACTION_WEB_SEARCH);
        busqueda.putExtra(SearchManager.QUERY, texto);
        try {
            startActivity(busqueda);
        } catch (ActivityNotFoundException e) {
            // Algunos teléfonos no tienen app para ACTION_WEB_SEARCH: se busca en Google por URL
            iniciarSeguro(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/search?q=" + Uri.encode(texto))));
        }
    }
}
