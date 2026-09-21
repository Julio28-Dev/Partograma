package com.jninfo.partograma.partograma;

import android.app.Application;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;

/**
 * Classe de Application do app. Unica responsabilidade hoje: autenticar o aparelho de
 * forma anonima no Firebase assim que o processo sobe, ANTES de qualquer tela chegar a
 * chamar o Firestore.
 *
 * Por que autenticacao anonima e nao uma tela de login: o app nunca teve conceito de
 * usuario/senha (nem o original, nem esta evolucao pediu isso) -- toda a equipe de
 * enfermagem usa o mesmo aparelho/app igualmente, sem distincao de conta. Mas sem
 * NENHUMA autenticacao, as regras do Firestore (ver firestore.rules) nao teriam como
 * negar acesso a um script qualquer na internet que descobrisse o project_id -- so
 * poderiam ser "liberado para todo mundo" ou "bloqueado para todo mundo". O login
 * anonimo resolve isso sem exigir nenhuma tela nova: cada instalacao do app vira um
 * "usuario" tecnico (sem nome/senha, transparente para quem usa), e as regras passam a
 * exigir "request.auth != null", bloqueando quem nao passa pelo app.
 *
 * Limite conhecido, disclosed: isso nao substitui autenticacao real. Alguem que extraia
 * a api_key do APK ainda consegue chamar signInAnonymously() por fora e depois acessar o
 * banco. Para fechar essa brecha de vez seria necessario Firebase App Check e/ou contas
 * reais por usuario -- fora do escopo desta etapa.
 */
public class PartogramaApplication extends Application {

    private static final String TAG = "PartogramaApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        autenticarAnonimamenteSeNecessario();
    }

    private void autenticarAnonimamenteSeNecessario() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            return;
        }
        auth.signInAnonymously()
                .addOnFailureListener(e -> Log.w(TAG, "Falha ao autenticar anonimamente no Firebase", e));
    }
}
