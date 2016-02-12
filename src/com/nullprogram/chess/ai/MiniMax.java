package com.nullprogram.chess.ai;

import com.nullprogram.chess.Board;
import com.nullprogram.chess.Game;
import com.nullprogram.chess.Move;
import com.nullprogram.chess.MoveList;
import com.nullprogram.chess.Piece;
import com.nullprogram.chess.Piece.Side;
import com.nullprogram.chess.Player;
import com.nullprogram.chess.Position;
import com.nullprogram.chess.pieces.Archbishop;
import com.nullprogram.chess.pieces.Bishop;
import com.nullprogram.chess.pieces.Chancellor;
import com.nullprogram.chess.pieces.King;
import com.nullprogram.chess.pieces.Knight;
import com.nullprogram.chess.pieces.Pawn;
import com.nullprogram.chess.pieces.Queen;
import com.nullprogram.chess.pieces.Rook;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MiniMax Chess AI player.
 *
 * This employs the dumb minimax algorithm to search the game tree for moves.
 * The board is currently only evaluated only by the pieces present, not their
 * positions.
 */
public class MiniMax implements Player {

    /**
     * This class's Logger.
     */
    private static final Logger LOG =
            Logger.getLogger("com.nullprogram.chess.ai.Minimax");
    /**
     * The number of threads to use.
     */
    private static final int NTHREADS =
            (Runtime.getRuntime().availableProcessors()) ;
    /**
     * Local friendly game controller.
     */
    private final Game game;
    /**
     * Side this AI plays.
     */
    private Piece.Side side = null;
    /**
     * Best move, the selected move.
     */
    private volatile Move bestMove;
    /**
     * Thread manager.
     */
    private final Executor executor = Executors.newFixedThreadPool(NTHREADS);
    /**
     * Values of each piece.
     */
    private Map<Class, Double> values;
    /**
     * Divisor for milliseconds.
     */
    static final double MILLI = 1000.0;
    /**
     * Maximum depth (configured).
     */
    private int maxDepth;
    /**
     * Material score weight (configured).
     */
    private double wMaterial;
    /**
     * King safety score weight (configured).
     */
    private double wSafety;
    /**
     * Mobility score weight (configured).
     */
    private double wMobility;
    private Random random = new Random();
    private boolean stopEvaluation = false;

    /**
     * Create the default MiniMax.
     *
     * @param active the game this AI is being seated at
     */
    public MiniMax(final Game active) {
        this(active, "default");
    }

    /**
     * Create a new AI from a given properties name.
     *
     * @param active the game this AI is being seated at
     * @param name name of configuration to use
     */
    public MiniMax(final Game active, final String name) {
        this(active, getConfig(name));
    }

    /**
     * Create a new AI for the given board.
     *
     * @param active the game this AI is being seated at
     * @param props properties for this player
     */
    public MiniMax(final Game active, final Properties props) {
        game = active;
        values = new HashMap<Class, Double>();
        Properties config = props;

        /*
         * Piece values
         */
        values.put((new Pawn(side)).getClass(),
                Double.parseDouble(config.getProperty("Pawn")));
        values.put((new Knight(side)).getClass(),
                Double.parseDouble(config.getProperty("Knight")));
        values.put((new Bishop(side)).getClass(),
                Double.parseDouble(config.getProperty("Bishop")));
        values.put((new Rook(side)).getClass(),
                Double.parseDouble(config.getProperty("Rook")));
        values.put((new Queen(side)).getClass(),
                Double.parseDouble(config.getProperty("Queen")));
        values.put((new King(side)).getClass(),
                Double.parseDouble(config.getProperty("King")));
        values.put((new Chancellor(side)).getClass(),
                Double.parseDouble(config.getProperty("Chancellor")));
        values.put((new Archbishop(side)).getClass(),
                Double.parseDouble(config.getProperty("Archbishop")));

        maxDepth = (int) Double.parseDouble(config.getProperty("depth"));
        wMaterial = Double.parseDouble(config.getProperty("material"));
        wSafety = Double.parseDouble(config.getProperty("safety"));
        wMobility = Double.parseDouble(config.getProperty("mobility"));
    }

    /**
     * Get the configuration.
     *
     * @param name name of the configuration to load
     * @return the configuration
     */
    public static Properties getConfig(final String name) {
        Properties props;
        if ("default".equals(name)) {
            props = new Properties();
        } else {
            props = new Properties(getConfig("default"));
        }

        String filename = name + ".properties";
        InputStream in = MiniMax.class.getResourceAsStream(filename);
        try {
            props.load(in);
        } catch (java.io.IOException e) {
            LOG.warning("Failed to load AI config: " + name + ": " + e);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                LOG.info("failed to close stream: " + e.getMessage());
            }
        }
        return props;
    }
    private Move currentThinkingBestMove = null;
    private MoveList analyzedMoves = null;

    public final Move takeTurn(final Board board,
            final Piece.Side currentSide) {
        return takeTurn(board,currentSide, Double.POSITIVE_INFINITY);
    }
    
    

    public final Move takeTurn(final Board board,
            final Piece.Side currentSide, final double minBeta) {
        side = currentSide;
        stopEvaluation = false;
        analyzedMoves = new MoveList(board, true);
        currentThinkingBestMove = null;

        /*
         * Gather up every move.
         */
        MoveList moves = board.allMoves(side, true);
        moves.shuffle();

        /*
         * Initialize the shared structures.
         */
        if (game != null) {
            game.setProgress(0);
            game.setStatus("Thinking ...");
        }
        long startTime = System.currentTimeMillis();
        final Map<Move, List<Move>> selectedBestMoves = new HashMap<Move, List<Move>>();

        /*
         * Spin off threads to evaluate each move's tree.
         */
        CompletionService<Move> service =
                new ExecutorCompletionService<Move>(executor);
        int submitted = 0;
        bestMove = null;
        for (final Move move : moves) {
            final Board callboard = board.copy();
            service.submit(new Callable<Move>() {

                public Move call() {
                    callboard.move(move);
                    double beta = Double.POSITIVE_INFINITY;
                    if (minBeta!=Double.POSITIVE_INFINITY) {
                        beta = minBeta;
                    }
                    if (bestMove != null) {
                        //if (beta<bestMove.getScore()) {
                            beta = -bestMove.getScore();
                        //}
                    }
                    List<Move> bestMoves = new ArrayList<Move>();
                    double v = search(callboard, maxDepth - 1,
                            Piece.opposite(side),
                            Double.NEGATIVE_INFINITY, beta, bestMoves);
//                    v = v - random.nextDouble() * v / 10;
                    move.setScore(-v);
                    if (currentThinkingBestMove == null) {
                        currentThinkingBestMove = move;
                        selectedBestMoves.put(move, bestMoves);
                        StringBuilder sb = new StringBuilder();
                        for (Move m : bestMoves) {
                            sb.append(m).append(",");
                        }
                        sb.append("...");
                        game.setStatus("Thinking on " + currentThinkingBestMove + "," + sb.toString() + currentThinkingBestMove.getScore());
                    }
                    if (currentThinkingBestMove.getScore() < move.getScore()) {
                        currentThinkingBestMove = move;
                        selectedBestMoves.put(move, bestMoves);
                        StringBuilder sb = new StringBuilder();
                        for (Move m : bestMoves) {
                            sb.append(m).append(",");
                        }
                        sb.append("...");
                        game.setStatus("Thinking on " + currentThinkingBestMove + "," + sb.toString() + currentThinkingBestMove.getScore());
                    }
                    currentThinkingBestMove = null;

                    return move;
                }
            });
            submitted++;
            if (stopEvaluation) {
                break;
            }
        }

        /*
         * Gather up results and pick the best move.
         */
        for (int i = 0; i < submitted; i++) {
            try {
                Move m = null;
                m = service.take().get();
                if (bestMove == null || m.getScore() > bestMove.getScore()) {
                    bestMove = m;
                }
                analyzedMoves.add(m);
            } catch (Exception e) {
                if (!stopEvaluation) {
                    LOG.log(Level.WARNING, "move went unevaluated: {0}", e.getMessage());
                    e.printStackTrace();
                }
                return null;
            }
            if (game != null) {
                game.setProgress(i / (1.0f * (submitted - 1)));
            }
        }
        
        StringBuilder sb = new StringBuilder();
        for (Move m : selectedBestMoves.get(bestMove)) {
            sb.append(m).append(",");
        }
        sb.append("...");
        game.setStatus("Selected Move " + bestMove + "," + sb.toString() + bestMove.getScore());


        long time = (System.currentTimeMillis() - startTime);
        LOG.log(Level.INFO, "AI took {0} seconds ({1} threads, {2} plies)", new Object[]{time / MILLI, NTHREADS, maxDepth});
        return bestMove;
    }

    /**
     * Recursive move searching.
     *
     * @param b board to search
     * @param depth current depth
     * @param s side for current move
     * @param alpha lower bound to check
     * @param beta upper bound to check
     * @return best valuation found at lowest depth
     */
    private double search(final Board b, final int depth, final Piece.Side s,
            final double alpha, final double beta, List<Move> moves) {
        if (depth == 0) {
            double v = valuate(b);
            return (s != side) ? -v : v;
        }
        Piece.Side opps = Piece.opposite(s);  // opposite side
        double best = alpha;
        double oldBest = Double.NEGATIVE_INFINITY;
        double score = 0;
        MoveList list = b.allMoves(s, true);
        for (Move move : list) {
            b.move(move);
            List<Move> bestMoves = new ArrayList<Move>();
            score = -search(b, depth - 1, opps, -beta, -best, bestMoves);
            best = Math.max(best, score);
            if (best > oldBest) {
//                scannedMoves[maxDepth-depth]=move;
                moves.clear();
                moves.add(move);
                for (Move m : bestMoves) {
                    moves.add(m);
                }
                oldBest = best;
            }
            b.undo();
            /*
             * alpha-beta prune
             */
            if (beta <= best) {
                return best;
            }
            if (stopEvaluation) {
                return best;
            }
        }
        return best;
    }

    public void stop() {
        stopEvaluation = true;
    }

    /**
     * Determine value of this board.
     *
     * @param b board to be valuated
     * @return valuation of this board
     */
    private double valuate(final Board b) {
        double material = materialValue(b);
        double kingSafety = kingInsafetyValue(b);
        double mobility = mobilityValue(b);
        return material * wMaterial
                + kingSafety * wSafety
                + mobility * wMobility;
    }

    /**
     * Add up the material value of the board only.
     *
     * @param b board to be evaluated
     * @return material value of the board
     */
    private double materialValue(final Board b) {
        double value = 0;
        for (int y = 0; y < b.getHeight(); y++) {
            for (int x = 0; x < b.getWidth(); x++) {
                Position pos = new Position(x, y);
                Piece p = b.getPiece(pos);
                if (p != null) {
                    value += values.get(p.getClass()) * p.getSide().value();
                }
            }
        }
        return value * side.value();
    }

    /**
     * Determine the safety of each king. Higher is worse.
     *
     * @param b board to be evaluated
     * @return king insafety score
     */
    private double kingInsafetyValue(final Board b) {
        return kingInsafetyValue(b, Piece.opposite(side))
                - kingInsafetyValue(b, side);
    }

    /**
     * Helper function: determine safety of a single king.
     *
     * @param b board to be evaluated
     * @param s side of king to be checked
     * @return king insafety score
     */
    private double kingInsafetyValue(final Board b, final Piece.Side s) {
        /*
         * Trace lines away from the king and count the spaces.
         */
        Position king = b.findKing(s);
        if (king == null) {
            /*
             * Weird, but may happen during evaluation.
             */
            return Double.POSITIVE_INFINITY;
        }
        MoveList list = new MoveList(b, false);
        /*
         * Take advantage of the Rook and Bishop code.
         */
        Rook.getMoves(b.getPiece(king), list);
        Bishop.getMoves(b.getPiece(king), list);
        return list.size();
    }

    /**
     * Mobility score for this board.
     *
     * @param b board to be evaluated
     * @return score for this board
     */
    private double mobilityValue(final Board b) {
        return b.allMoves(side, false).size()
                - b.allMoves(Piece.opposite(side), false).size();
    }

    /**
     * @return the evalMoves
     */
    public MoveList getAnalyzedMoves() {
        return analyzedMoves;
    }

    @Override
    public void opponentTurn(Board board, Move move, Side side) {
        
    }

    @Override
    public void dispose() {
        
    }

}
