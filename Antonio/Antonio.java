package connectx.Antonio;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;
import connectx.CXCellState;

import java.util.TreeSet;
import java.util.Random;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import javax.swing.plaf.basic.BasicBorders.RolloverButtonBorder;

public class Antonio implements CXPlayer {
    private boolean firstPlayer;
    private Random rand;
    private CXGameState myWin;
    private CXGameState yourWin;
    private int TIMEOUT;
    private long START;
    private static int MAX = 100, MIN = -MAX;

    public Antonio() {
    }

    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        rand = new Random(System.currentTimeMillis());
        myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
        yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
        firstPlayer = first;
        TIMEOUT = timeout_in_secs;
    }

    private void checktime() throws TimeoutException {
        if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
            throw new TimeoutException();
    }

    private int singleMoveWin(CXBoard B, Integer[] L) throws TimeoutException {
        for (int i : L) {
            checktime(); // controlla timeout a ogni iterazione
            CXGameState state = B.markColumn(i);
            if (state == myWin)
                return i; // colonna vincente trovata: ritorna immediatamente
            B.unmarkColumn();
        }
        return -1;
    }

    // se Antonio è primo allora avversario è secondo e viceversa
    private CXCellState opponentState() {
        if (firstPlayer) {
            return CXCellState.P2;
        } else {
            return CXCellState.P1;
        }
    }

    // stato di Antonio
    private CXCellState myState() {
        if (firstPlayer) {
            return CXCellState.P1;
        } else {
            return CXCellState.P2;
        }
    }

    // ABBIAMO CONTROLLATO TUTTE LE MOSSE CHE POTREBBE FARE L'AVVERSARIO
    // IMMEDIATAMENTE
    // SENZA CONSIDERARE LE MOSSE CHE POTREBBE FARE SOPRA ALLA NOSTRA FUTURA MOSSA

    // NON SERVE CONTROLLARE TUTTE LE VOLTE TUTTA LA TABELLA
    // BASTA CONTROLLARE QUELLA IN CUI SI METTE L'IPOTETICA MOSSA
    private int singleMoveBlock(CXBoard B, Integer[] L) throws TimeoutException {
        B.markColumn(L[0]);
        for (int j = L[0 + 1]; j < L.length; j++) {
            checktime();
            CXGameState state = B.markColumn(L[j]);
            if (state == yourWin) {
                B.unmarkColumn();
                B.unmarkColumn();
                return L[j];
            }
            B.unmarkColumn();
        }
        B.unmarkColumn();
        B.markColumn(L[0 + 1]);
        CXGameState state = B.markColumn(L[0]);
        if (state == yourWin) {
            B.unmarkColumn();
            B.unmarkColumn();
            return L[0];
        }
        B.unmarkColumn();
        B.unmarkColumn();
        return -1;
    }

    // ordinamento vettore colonne libere dal centro agli estremi
    public Integer[] sortFromMiddle(Integer[] L) {
        Integer[] V = new Integer[L.length];
        int cont = 0;
        for (int i = 0; (i <= L.length / 2); i++) {
            V[cont] = L[L.length / 2 + i];
            cont++;
            if (i != 0) {
                V[cont] = L[L.length / 2 - i];
                cont++;
            }

            if ((i + 1 == L.length / 2) && ((L.length % 2) == 0)) {
                V[cont] = L[0];
                break;
            }
        }
        return V;
    }

    public int selectColumn(CXBoard B) {
        START = System.currentTimeMillis();

        Integer[] L = B.getAvailableColumns();
        int toMark = L[rand.nextInt(L.length)]; // inizializzo a random

        // una sola colonna libera
        if (L.length == 1) {
            return L[0];
        }

        try {
            // vincita con una mossa sola
            int colW = singleMoveWin(B, L);
            if (colW != -1)
                return colW;
            // sconfitta alla prossima mossa dell'avversario
            int colL = singleMoveBlock(B, L);
            if (colL != -1)
                return colL;

            // alphabeta
            // seleziono della depth massima in base a dimensione board
            int depth;
            if ((B.M * B.N) < 100) {
                depth = 7;
            } else if ((B.M * B.N) < 350) {
                depth = 3;
            } else {
                depth = 1;
            }
            int outcome = Integer.MIN_VALUE, maxOutcome = outcome;
            Integer[] S = sortFromMiddle(L);

            // esamino tutte le colonne libere
            for (int colIt : S) {
                checktime();
                CXGameState stateAB = B.markColumn(colIt); // marco mossa da valutare
                outcome = AlphaBetaPruning(B, false, Integer.MIN_VALUE, Integer.MAX_VALUE,
                        depth, stateAB);
                B.unmarkColumn();
                if (outcome > maxOutcome) { // confronto il risultato della visita alpha beta
                                            // corrente con quella precedente per salvare quello piu' vantaggioso
                    maxOutcome = outcome;
                    toMark = colIt;
                }
            }

            return toMark;
        } catch (TimeoutException e) {
            System.err.println("Timeout!!! Last computed column selected");
            return toMark;
        }
    }

    public int AlphaBetaPruning(CXBoard B, boolean playerAntonio, int alpha, int beta, int depth, CXGameState stateAB) {
        int eval = 0;
        try {
            checktime();
            if (!stateAB.equals(CXGameState.OPEN) || (depth == 0)) {
                return evaluate(stateAB, B);
            } else if (playerAntonio) { // MAX player
                eval = Integer.MIN_VALUE;
                Integer[] cols = B.getAvailableColumns();
                for (int c : cols) {
                    CXGameState state = B.markColumn(c);
                    eval = Math.max(eval, AlphaBetaPruning(B, !playerAntonio, alpha, beta, depth - 1, state));
                    alpha = Math.max(eval, alpha);
                    B.unmarkColumn();
                    if (beta <= alpha) { // β cutoff
                        break;
                    }
                }
                return eval;
            } else {// MIN player
                eval = Integer.MAX_VALUE;
                Integer[] cols = B.getAvailableColumns();
                for (int c : cols) {
                    CXGameState state = B.markColumn(c);
                    eval = Math.min(eval, AlphaBetaPruning(B, !playerAntonio, alpha, beta, depth - 1, state));
                    beta = Math.min(eval, beta);
                    B.unmarkColumn();
                    if (beta <= alpha) { // α cutoff
                        break;
                    }
                }
                return eval;
            }
        } catch (TimeoutException e) {
            // System.err.println("Timeout!!! Last computed column selected");
            return eval;
        }
    }

    public int evaluate(CXGameState state, CXBoard B) {
        if (state == myWin) { // vinco
            return MAX;
        } else if (state == yourWin) { // perdo
            return MIN;
        } else if (state == CXGameState.DRAW) { // pareggio
            return 0;
        } else { // max depth
            int maxd = evalMaxDepth(B);
            return maxd;
        }
    }

    public int evalVertical(CXBoard B, CXCell move) {
        int cont;
        for (cont = 1; (move.i - cont) >= 0; cont++) {
            if (B.cellState((move.i - cont), move.j) == opponentState()) {
                cont++;
                break;
            }
        }
        // controlla se è possibile vincere verso l'alto
        if ((B.M - move.i) + cont - 1 < B.X) {
            return 0;
        }

        return cont = cont - 1;
    }

    public int evalHorizontal(CXBoard B, CXCell move) {
        int l, r, tot; // contatori
        boolean l_bound = true, r_bound = true; // variabili per il controllo della presenza di avversari alla fine
                                                // della serie di dischi consecutive

        // conta verso sinistra
        for (l = 1; (move.j - l) >= 0; l++) {
            if (B.cellState(move.i, (move.j - l)) != myState()) {
                l_bound = (B.cellState(move.i, (move.j - l)) == opponentState());
                l++;
                break;
            }
        }
        l = l - 1;

        // conta verso destra
        for (r = 1; (move.j + r) < B.N; r++) {
            if (B.cellState(move.i, (move.j + r)) != myState()) {
                r_bound = (B.cellState(move.i, (move.j + r)) == opponentState());
                r++;
                break;
            }
        }
        r = r - 1;

        // se sia a sinistra che a destra siamo bloccati da avversario allora la mossa
        // non ha valore (orizzontalmente)
        if (r_bound && l_bound) {
            tot = 0;
        } else {
            tot = r + l + 1;
        }
        return tot;
    }

    public int evalDiagonal(CXBoard B, CXCell move) {
        // controllo diagonale
        int l_down, r_up, tot; // contatori left e right e tot
        boolean l_down_bound = true, r_up_bound = true; // variabili per il controllo
                                                        // della presenza di avversari alla
                                                        // fine della serie di dischi consecutivi

        // conta in basso a sinistra
        for (l_down = 1; (((move.i - l_down) >= 0) && ((move.j - l_down) >= 0)); l_down++) {
            if (B.cellState((move.i - l_down), (move.j - l_down)) != myState()) {
                l_down_bound = (B.cellState((move.i - l_down), (move.j - l_down)) == opponentState());
                l_down++;
                break;
            }
        }
        l_down = l_down - 1;

        // conta in alto a destra
        for (r_up = 1; (((move.i + r_up) < B.M) && ((move.j + r_up) < B.N)); r_up++) {
            if (B.cellState((move.i + r_up), (move.j + r_up)) != myState()) {
                r_up_bound = (B.cellState((move.i + r_up), (move.j + r_up)) == opponentState());
                r_up++;
                break;
            }
        }
        r_up = r_up - 1;

        // se sia a sinistra che a destra siamo bloccati da avversario allora la
        // mossa non ha valore (diagonalmente)
        if (r_up_bound && l_down_bound) {
            tot = 0;
        } else {
            tot = r_up + l_down;
        }

        return tot;
    }

    public int evalAntiDiagonal(CXBoard B, CXCell move) {
        // controllo anti-diagonale
        int l_up, r_down, tot; // contatori left e right e tot
        boolean l_up_bound = true, r_down_bound = true; // variabili per il controllo
                                                        // della presenza di avversari alla
                                                        // fine della serie di dischi consecutive

        // conta in basso a destra
        for (r_down = 1; (((move.i - r_down) >= 0) && ((move.j + r_down) < B.N)); r_down++) {
            if (B.cellState((move.i - r_down), (move.j + r_down)) != myState()) {
                r_down_bound = (B.cellState((move.i - r_down), (move.j + r_down)) == opponentState());
                r_down++;
                break;
            }
        }
        r_down = r_down - 1;

        // conta in alto a sinistra
        for (l_up = 1; (((move.i + l_up) < B.M) && ((move.j - l_up) >= 0)); l_up++) {
            if (B.cellState((move.i + l_up), (move.j - l_up)) != myState()) {
                l_up_bound = (B.cellState((move.i + l_up), (move.j - l_up)) == opponentState());
                l_up++;
                break;
            }
        }
        l_up = l_up - 1;

        // se sia a sinistra che a destra siamo bloccati da avversario allora la
        // mossa non ha valore (antidiagonalmente)
        if (l_up_bound && r_down_bound) {
            tot = 0;
        } else {
            tot = l_up + r_down;
        }

        return tot;
    }

    public int evalMaxDepth(CXBoard B) {
        CXCell move = B.getLastMove();
        return evalVertical(B, move) + evalHorizontal(B, move) + evalDiagonal(B, move) + evalAntiDiagonal(B, move);
    }

    public String playerName() {
        return "Antonio";
    }
}