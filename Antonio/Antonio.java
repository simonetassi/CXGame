package connectx.Antonio;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;
import java.util.TreeSet;
import java.util.Random;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

public class Antonio implements CXPlayer {
    private Random rand;
    private CXBoard B;
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
        TIMEOUT = timeout_in_secs;
    }

    private void checktime() throws TimeoutException {
        if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
            throw new TimeoutException();
    }

    private int singleMoveWin(CXBoard B, Integer[] L) throws TimeoutException {
        for (int i : L) {
            checktime(); // Check timeout at every iteration
            CXGameState state = B.markColumn(i);
            if (state == myWin)
                return i; // Winning column found: return immediately
            B.unmarkColumn();
        }
        return -1;
    }

    // ABBIAMO CONTROLLATO TUTTE LE MOSSE CHE POTREBBE FARE L'AVVERSARIO
    // IMMEDIATAMENTE
    // SENZA CONSIDERARE LE MOSSE CHE POTREBBE FARE SOPRA ALLA NOSTRA FUTURA MOSSA

    // NON SERVE CONTROLLARE TUTTE LE VOLTE TUTTA LA TABELLA
    // BASTA CONTROLLARE QUELLA IN CUI SI METTE L'IPOTETICA MOSSA
    private int singleMoveBlock(CXBoard B, Integer[] L) throws TimeoutException {
        B.markColumn(L[0]);
        for (int j = L[0 + 1]; j < L.length; j++) {
            // try {Thread.sleep((int)(0.2*1000*TIMEOUT));} catch (Exception e) {} //
            // Uncomment to test timeout
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

    public int selectColumn(CXBoard B) {
        START = System.currentTimeMillis();

        Integer[] L = B.getAvailableColumns();
        int toMark = L[rand.nextInt(L.length)]; // inizializzo a random

        // una sola colonna libera
        if (L.length == 1) {
            // B.markColumn(L[0]);
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
            if ((B.M * B.N) < 50) {
                depth = 10;
            } else if ((B.M * B.N) < 275) {
                depth = 7;
            } else {
                depth = 3;
            }
            int outcome = Integer.MIN_VALUE, maxOutcome = outcome;

            // esamino tutte le colonne libere
            for (int colIt : L) {
                checktime();
                CXGameState stateAB = B.markColumn(colIt); // marco mossa da valutare
                outcome = AlphaBetaPruning(B, false, Integer.MIN_VALUE, Integer.MAX_VALUE, depth, stateAB);
                B.unmarkColumn();
                if (outcome > maxOutcome) { // confronto il risultato della visita alpha beta corrente con
                                            // quella
                                            // precedente per salvare quello piu' vantaggioso
                    maxOutcome = outcome;
                    toMark = colIt;
                }

            }
            // B.markColumn(toMark);
            return toMark;
        } catch (TimeoutException e) {
            System.err.println("Timeout!!! Last computed column selected");
            return toMark;
        }
    }

    public int AlphaBetaPruning(CXBoard B, boolean playerAntonio, int alpha, int beta, int depth, CXGameState stateAB) {
        int eval;
        if (!stateAB.equals(CXGameState.OPEN) || (depth == 0)
                || (System.currentTimeMillis() - START) / 1000.0 > TIMEOUT * (99.0 / 100.0)) {
            return evaluate(stateAB);
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
    }

    public int evaluate(CXGameState s) {
        if (s == myWin) { // vinco
            return MAX;
        } else if (s == yourWin) { // perdo
            return MIN;
        } else if (s == CXGameState.DRAW) { // pareggio
            return 0;
        } else { // max depth
            return 0;
        }
    }

    public String playerName() {
        return "Antonio";
    }
}