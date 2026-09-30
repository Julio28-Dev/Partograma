package com.jninfo.partograma.partograma.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Fonte central de verdade sobre "a sessao atual pode acessar telas protegidas?". Usada
 * por {@link com.jninfo.partograma.partograma.BaseActivity} (gate de todas as telas
 * protegidas) e por quem faz login/logout, para nao duplicar essa logica em cada Activity.
 *
 * Duas camadas deliberadamente separadas:
 *
 * 1) sessaoValidaLocalmente(): 100% local/sincrono (Firebase Auth em cache + um timestamp
 *    em SharedPreferences), sem chamada de rede. E o "cadeado" de toda tela protegida --
 *    precisa ser rapido e funcionar offline, senao o app fica inutilizavel sem internet
 *    mesmo para quem ja tinha sessao valida e recente. So SharedPreferences NAO seria
 *    suficiente sozinho (alguem poderia forjar o valor fora do app), por isso ela sempre
 *    exige tambem um FirebaseUser real e nao-anonimo em cache -- SharedPreferences aqui e
 *    só o registro de "quando" foi a ultima vez que essa sessao foi validada, nunca a
 *    prova de autenticacao em si.
 *
 * 2) verificarAutorizacaoInstituicao(): assincrono, consulta o Firestore (admins/{uid} e
 *    instituicoes/{uid}.ativo) para pegar uma revogacao feita depois do login (instituicao
 *    desativada pelo admin). Deliberadamente "falha aberto" quando offline/erro -- ver
 *    javadoc do metodo.
 */
public final class SessaoUtil {

    private static final String ARQUIVO = "SessaoApp";
    private static final String CHAVE_ULTIMO_ACESSO_VALIDO = "ultimo_acesso_valido_ms";
    private static final long LIMITE_INATIVIDADE_MS = 30L * 24 * 60 * 60 * 1000; // 30 dias

    private SessaoUtil() {
    }

    /**
     * True somente se: existe um FirebaseUser em cache, ele NAO e anonimo (login anonimo e
     * usado exclusivamente para o envio de "Solicitar acesso", nunca para acessar dados
     * protegidos), e a ultima vez que essa sessao foi validada por este mecanismo foi ha
     * menos de 30 dias. Cobre sozinho, sem rede: primeiro acesso (timestamp inexistente),
     * logout (Auth + timestamp limpos), reinstalacao (SharedPreferences e o cache do
     * Firebase Auth são apagados junto com o app) e inatividade de 30 dias.
     */
    public static boolean sessaoValidaLocalmente(Context context) {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null || usuario.isAnonymous()) {
            return false;
        }
        long ultimoAcesso = prefs(context).getLong(CHAVE_ULTIMO_ACESSO_VALIDO, 0L);
        if (ultimoAcesso <= 0L) {
            return false;
        }
        long decorrido = System.currentTimeMillis() - ultimoAcesso;
        // "decorrido < 0" cobre o relogio do aparelho tendo sido adiantado e depois
        // corrigido para tras -- trata como sessao expirada em vez de confiar num valor
        // negativo (nunca deixa passar por conta de manipulacao/erro de relogio).
        return decorrido >= 0 && decorrido < LIMITE_INATIVIDADE_MS;
    }

    /**
     * Marca "agora" como o ultimo instante em que a sessao foi validada. Chamado apos um
     * login bem-sucedido e toda vez que uma tela protegida abre com sessao ja valida
     * (mantem a janela de 30 dias rolando enquanto a pessoa continuar usando o app).
     */
    public static void registrarAcessoValido(Context context) {
        prefs(context).edit().putLong(CHAVE_ULTIMO_ACESSO_VALIDO, System.currentTimeMillis()).apply();
    }

    /**
     * Encerra a sessao: sai do Firebase Auth e limpa o timestamp local. Nao mexe em
     * nenhum dado clinico (pacientes/instituicoes continuam intactos no Firestore) --
     * so estado de sessao neste aparelho.
     */
    public static void encerrarSessao(Context context) {
        FirebaseAuth.getInstance().signOut();
        prefs(context).edit().remove(CHAVE_ULTIMO_ACESSO_VALIDO).apply();
    }

    public interface AutorizacaoCallback {
        void onResultado(boolean autorizada);
    }

    /**
     * Verifica no Firestore se a conta logada continua com acesso: admin sempre esta
     * autorizado; instituicao comum precisa que instituicoes/{uid}.ativo nao seja
     * explicitamente false (campo ausente = ativa, mantem compatibilidade com contas
     * criadas antes deste campo existir).
     *
     * Modo offline/erro: FALHA ABERTO (considera autorizada) de proposito. O "cadeado"
     * de verdade contra quem nunca teve acesso e sessaoValidaLocalmente() (100% local,
     * sempre aplicado); esta checagem aqui e uma camada ADICIONAL para pegar uma
     * revogacao feita depois do login, e negar acesso so por estar sem internet
     * quebraria o app para gente que ja era legitima, o que e desproporcional para uma
     * funcionalidade de revogacao que hoje nem tem tela de administracao. Ver relatorio
     * final para essa decisao documentada.
     */
    public static void verificarAutorizacaoInstituicao(String uid, AutorizacaoCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("admins").document(uid).get()
                .addOnSuccessListener(adminDoc -> {
                    if (adminDoc.exists()) {
                        callback.onResultado(true);
                        return;
                    }
                    db.collection("instituicoes").document(uid).get()
                            .addOnSuccessListener(instDoc -> {
                                if (!instDoc.exists()) {
                                    callback.onResultado(true);
                                    return;
                                }
                                Boolean ativo = instDoc.getBoolean("ativo");
                                callback.onResultado(ativo == null || ativo);
                            })
                            .addOnFailureListener(erro -> callback.onResultado(true));
                })
                .addOnFailureListener(erro -> callback.onResultado(true));
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE);
    }
}
