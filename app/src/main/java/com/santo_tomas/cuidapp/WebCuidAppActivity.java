package com.santo_tomas.cuidapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.OnBackPressedCallback;

import com.google.android.material.progressindicator.LinearProgressIndicator;

/*
 * NUEVO - Pestaña "Web": muestra CuidApp Web (la versión web del proyecto) DENTRO de la app,
 * con un WebView. Así el cuidador ve la misma plataforma desde el teléfono.
 *
 *   - JavaScript activado: la web es una app de una sola página (Vite) y lo necesita.
 *   - WebViewClient: los enlaces se abren aquí mismo, no en otro navegador.
 *   - WebChromeClient: avisa el % de carga para la barra de progreso.
 *   - Botón atrás: retrocede dentro de la web antes de salir de la pantalla.
 *   - IMPLÍCITO: "Abrir en el navegador" -> ACTION_VIEW con la misma URL.
 */
public class WebCuidAppActivity extends BaseActivity {

    private WebView webView;
    private LinearProgressIndicator progreso;
    private View panelError;

    @SuppressLint("SetJavaScriptEnabled") // la web es nuestra y la necesita
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_cuidapp);
        InsetsUtil.aplicar(this);

        webView = findViewById(R.id.webCuidApp);
        progreso = findViewById(R.id.progresoWeb);
        panelError = findViewById(R.id.panelErrorWeb);

        WebSettings ajustes = webView.getSettings();
        ajustes.setJavaScriptEnabled(true);
        ajustes.setDomStorageEnabled(true); // la web guarda datos en localStorage

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                // Solo si falla la página principal (no una imagen suelta)
                if (request.isForMainFrame()) {
                    panelError.setVisibility(View.VISIBLE);
                    webView.setVisibility(View.GONE);
                }
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int nuevo) {
                progreso.setProgressCompat(nuevo, true);
                progreso.setVisibility(nuevo < 100 ? View.VISIBLE : View.GONE);
            }
        });

        configurarBoton(R.id.btnVolverWeb, v -> finish());
        configurarBoton(R.id.btnAbrirNavegador, v -> abrirEnNavegador());
        configurarBoton(R.id.btnReintentarWeb, v -> cargar());

        // Atrás: primero retrocede en el historial de la web
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack();
                else finish();
            }
        });

        if (savedInstanceState != null) webView.restoreState(savedInstanceState);
        else cargar();
    }

    private void cargar() {
        panelError.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        webView.loadUrl(getString(R.string.url_cuidapp_web));
    }

    // IMPLÍCITO: la misma web en el navegador del teléfono
    private void abrirEnNavegador() {
        String url = webView.getUrl() != null ? webView.getUrl() : getString(R.string.url_cuidapp_web);
        iniciarSeguro(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState); // al girar la pantalla no se recarga desde cero
    }

    @Override
    protected void onDestroy() {
        if (webView != null) webView.destroy(); // libera memoria
        super.onDestroy();
    }
}
