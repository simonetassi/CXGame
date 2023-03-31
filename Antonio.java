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
    private long start;
    private static int MAX = 100, MIN = -MAX;

    public Antonio() {
    }

    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        rand = new Random(System.currentTimeMillis());
        // B = new CXBoard(M, N, K);
        myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
        yourWin = first ? CXKGameState.WINP2 : CXGameState.WINP1;
        TIMEOUT = timeout_in_secs;
    }

    private void checktime() throws TimeoutException {
		if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
			throw new TimeoutException();
	}

    private int singleMoveWin(CXBoard B, Integer[] L) throws TimeoutException {
        for(int i : L) {
                checktime(); // Check timeout at every iteration
          CXGameState state = B.markColumn(i);
          if (state == myWin)
            return i; // Winning column found: return immediately
          B.unmarkColumn();
        }
            return -1;
    }

    // TODO NON SERVE CONTROLLARE TUTTE LE VOLTE TUTTA LA TABELLA
    // BASTA CONTROLLARE QUELLA IN CUI SI METTE L'IPOTETICA MOSSA
    private int singleMoveBlock(CXBoard B, Integer[] L) throws TimeoutException {
		TreeSet<Integer> T = new TreeSet<Integer>(); // We collect here safe column indexes

		for(int i : L) {
			checktime();
			T.add(i); // We consider column i as a possible move
			B.markColumn(i);

			int j;
			boolean stop;

			for(j = 0, stop=false; j < L.length && !stop; j++) {
				//try {Thread.sleep((int)(0.2*1000*TIMEOUT));} catch (Exception e) {} // Uncomment to test timeout
				checktime();
				if(!B.fullColumn(L[j])) {
					CXGameState state = B.markColumn(L[j]);
					if (state == yourWin) {
						T.remove(i); // We ignore the i-th column as a possible move
						stop = true; // We don't need to check more
					}
					B.unmarkColumn(); // 
				}
			}
			B.unmarkColumn();
		}

		if (T.size() > 0) {
			Integer[] X = T.toArray(new Integer[T.size()]);
 			return X[rand.nextInt(X.length)];
		} else {
			return L[rand.nextInt(L.length)];
		}
	}


    public MNKCell selectColumn(CXBoard B) {
        start = System.currentTimeMillis();

        Integer[] L = B.getAvailableColumns();
        int save    = L[rand.nextInt(L.length)]; // TODO PENSARE A COSA RESTITUIRE 

        // recupero l'ultima mossa dell'avversario (se e' stata fatta)
        if (MC.length > 0) {
            MNKCell c = MC[MC.length - 1];
            B.markCell(c.i, c.j);
        }

        // una sola mossa possibile
        if (L.length == 1) {
            B.markColumn(L[0]);
            return L[0];
        }

        
        try {
            // vincita con una mossa sola
			int col = singleMoveWin(B,L);
			if(col != -1) 
				return col;
			else
                // sconfitta alla prossima mossa dell'avversario
				return singleMoveBlock(B,L);
		} catch (TimeoutException e) {
			System.err.println("Timeout!!! Random column selected");
			return save;
		}
        B.unmarkCell(); // annullo la mossa iniziale del bot e procedo a valutazione dei casi generici

        // alphabeta
        // seleziono della depth massima in base a dimensione board
        MNKCell toMark = FC[0];
        int depth;
        if ((B.M * B.N) < 50) {
            depth = 5;
        } else if ((B.M * B.N) < 275) {
            depth = 3;
        } else {
            depth = 1;
        }
        int outcome = Integer.MIN_VALUE, maxOutcome = outcome;

        MNKCell[] cells = B.getFreeCells();

        // esamino tutte le celle libere
        for (MNKCell cellIt : cells) {
            if ((System.currentTimeMillis() - start) / 1000.0 > TIMEOUT * (97.0 / 100.0)) { // se non faccio in tempo a
                                                                                            // trovare la mossa migliore
                                                                                            // -> break
                break;
            } else {
                B.markCell(cellIt.i, cellIt.j); // marco mossa da valutare
                outcome = AlphaBetaPruning(B, false, Integer.MIN_VALUE, Integer.MAX_VALUE, depth);
                B.unmarkCell();
                if (outcome > maxOutcome) { // confronto il risultato della visita alpha beta corrente con quella
                                            // precedente per salvare quello piu' vantaggioso
                    maxOutcome = outcome;
                    toMark = cellIt;
                }
            }
        }

        B.markCell(toMark.i, toMark.j);
        return toMark;
    }

    public int AlphaBetaPruning(MNKBoard B, boolean playerAntonio, int alpha, int beta, int depth) {
        int eval;
        if (!B.gameState().equals(MNKGameState.OPEN) || (depth == 0)
                || (System.currentTimeMillis() - start) / 1000.0 > TIMEOUT * (97.0 / 100.0)) {
            return evaluate(B.gameState());
        } else if (playerAntonio) { // MAX player
            eval = Integer.MIN_VALUE;
            MNKCell[] cells = B.getFreeCells();
            for (MNKCell c : cells) {
                B.markCell(c.i, c.j);
                eval = Math.max(eval, AlphaBetaPruning(B, !playerAntonio, alpha, beta, depth - 1));
                alpha = Math.max(eval, alpha);
                B.unmarkCell();
                if (beta <= alpha) { // β cutoff
                    break;
                }
            }
            return eval;
        } else {// MIN player
            eval = Integer.MAX_VALUE;
            MNKCell[] cells = B.getFreeCells();
            for (MNKCell c : cells) {
                B.markCell(c.i, c.j);
                eval = Math.min(eval, AlphaBetaPruning(B, !playerAntonio, alpha, beta, depth - 1));
                beta = Math.min(eval, beta);
                B.unmarkCell();
                if (beta <= alpha) { // α cutoff
                    break;
                }
            }
            return eval;
        }
    }

    public int evaluate(MNKGameState s) {
        if (s == myWin) { // vinco
            return MAX;
        } else if (s == yourWin) { // perdo
            return MIN;
        } else if (s == MNKGameState.DRAW) { // pareggio
            return 0;
        } else { // max depth
            return 0;
        }
    }

    public String playerName() {
        return "Antonio";
    }
}