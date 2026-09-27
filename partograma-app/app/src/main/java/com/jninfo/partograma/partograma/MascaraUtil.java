package com.jninfo.partograma.partograma;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

/**
 * Mascaras de digitacao para campos de data e horario: o usuario digita so os numeros e as
 * barras/dois-pontos aparecem sozinhas. Pedido do cliente para reduzir erro de digitacao
 * (ex.: "28042005" vira "28/04/2005" enquanto digita).
 *
 * O ano curto (ex.: "28/04/05" ou "280405", 6 digitos ao todo) so e expandido para 4 digitos
 * quando o campo perde o foco -- nao da para saber, digito a digito, se o usuario vai parar
 * em 2 digitos de ano ou ainda vai completar para 4, entao expandir cedo demais quebraria
 * quem estivesse digitando o ano completo.
 */
final class MascaraUtil {

    private MascaraUtil() {
    }

    static void aplicarMascaraData(EditText campo) {
        campo.addTextChangedListener(new TextWatcher() {
            private boolean aplicando = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (aplicando) {
                    return;
                }
                aplicando = true;
                String digitos = somenteDigitos(s.toString());
                if (digitos.length() > 8) {
                    digitos = digitos.substring(0, 8);
                }
                String formatado = formatarData(digitos);
                if (!formatado.contentEquals(s)) {
                    s.replace(0, s.length(), formatado);
                }
                aplicando = false;
            }
        });

        campo.setOnFocusChangeListener((v, temFoco) -> {
            if (!temFoco) {
                expandirAnoCurto(campo);
            }
        });
    }

    static void aplicarMascaraHorario(EditText campo) {
        campo.addTextChangedListener(new TextWatcher() {
            private boolean aplicando = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (aplicando) {
                    return;
                }
                aplicando = true;
                String digitos = somenteDigitos(s.toString());
                if (digitos.length() > 4) {
                    digitos = digitos.substring(0, 4);
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < digitos.length(); i++) {
                    if (i == 2) {
                        sb.append(':');
                    }
                    sb.append(digitos.charAt(i));
                }
                if (!sb.toString().contentEquals(s)) {
                    s.replace(0, s.length(), sb.toString());
                }
                aplicando = false;
            }
        });
    }

    private static String formatarData(String digitos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digitos.length(); i++) {
            if (i == 2 || i == 4) {
                sb.append('/');
            }
            sb.append(digitos.charAt(i));
        }
        return sb.toString();
    }

    /** Ao sair do campo, "28/04/05" (6 digitos) vira "28/04/2005". */
    private static void expandirAnoCurto(EditText campo) {
        String digitos = somenteDigitos(campo.getText().toString());
        if (digitos.length() == 6) {
            String diaEMes = digitos.substring(0, 4);
            String anoCurto = digitos.substring(4, 6);
            String textoFinal = formatarData(diaEMes + "20" + anoCurto);
            campo.setText(textoFinal);
            campo.setSelection(textoFinal.length());
        }
    }

    private static String somenteDigitos(String texto) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (Character.isDigit(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
