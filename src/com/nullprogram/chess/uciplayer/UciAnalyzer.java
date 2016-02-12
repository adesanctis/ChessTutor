/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nullprogram.chess.uciplayer;

import com.nullprogram.chess.Board;
import com.nullprogram.chess.Game;
import com.nullprogram.chess.Move;
import com.nullprogram.chess.MoveList;
import com.nullprogram.chess.Piece.Side;
import com.nullprogram.chess.boards.StandardBoard;

/**
 *
 * @author Administrator
 */
public class UciAnalyzer {

    public static final int BEST_MOVE_NUMBER = 6;
    public static final int ANALYSIS_TIME = 30000;
    public static final int ANALYSIS_LEVEL = 14;
    private UCIChess uci = null;
    private String currentFen = ChessBoard.STARTPOSITION;
    private Game game;

    public UciAnalyzer(Game game) {
        this.game = game;
        uci = new UCIChess(UciPlayer.ENGINE_PATH);
        if (uci.get_UciOk(true)) {
        }
        uci.send_Option_Name_WithValue("MultiPV", "" + BEST_MOVE_NUMBER);
        uci.send_Option_Name_WithValue("Skill Level", "20");
        uci.send_Option_Name_WithValue("UCI_Elo","1800");
        uci.move_FromSTART("", true);
    }

    public UciAnalyzer(Game game, String startingFEN) {
        currentFen = startingFEN;
    }

    public MoveList analizePosition(Board board, Side side) {
        MoveList moves = null;
//        uci.move_FromSTART(board.moveHistory(), false);
        uci.move_FromFEN(board.getFEN() + " " + (side.equals(Side.BLACK) ? "b" : "w"), "", true);
//        uci.go_Think_MoveTime(ANALYSIS_TIME);
        uci.go_Think_MoveTime(ANALYSIS_TIME);
//        uci.go_Think_Depth(ANALYSIS_LEVEL);
//        uci.go_Think();
        String bestMove = uci.get_BestMove(true);
        moves = getAnalyzedMoves(uci, board);
        return moves;
    }
    
    public MoveList analizePositionFast(Board board, Side side) {
        MoveList moves = null;
//        uci.move_FromSTART(board.moveHistory(), false);
        uci.move_FromFEN(board.getFEN() + " " + (side.equals(Side.BLACK) ? "b" : "w"), "", true);
        uci.go_Think_MoveTime(ANALYSIS_TIME/10);
//        uci.go_Think_Depth(9);
//        uci.go_Think_Depth(ANALYSIS_LEVEL);
//        uci.go_Think();
        String bestMove = uci.get_BestMove(true);
        moves = getAnalyzedMoves(uci, board);
        return moves;
    }
    

    private MoveList getAnalyzedMoves(UCIChess uci, Board board) {
        Move[] moves = new Move[BEST_MOVE_NUMBER];
        int maxLines = uci.get_Number_DetailedInfo();
        Move move = null;
        for (int index = 0; index < maxLines; index++) {
            if (!"".equals(uci.get_DetailedInfo(index).getMultiPV())) {
                //System.out.println(uci.get_DetailedInfo(index).getScoreCP() + ": " + uci.get_DetailedInfo(index).getPv());

                move = new Move(uci.get_DetailedInfo(index).getPv().substring(0, 4));
                move.setScore(Double.parseDouble(uci.get_DetailedInfo(index).getScoreCP()));
                move.setAnalyzedMoves(uci.get_DetailedInfo(index).getPv());
                moves[Integer.parseInt(uci.get_DetailedInfo(index).getMultiPV()) - 1] = move;
            }
        }
        MoveList moveList = new MoveList(board);
        for (int i = 0; i < BEST_MOVE_NUMBER; i++) {
            moveList.add(moves[i]);
            System.out.println("Analyzer move: " + moves[i] + " with score " + moves[i].getScore() + " pv: " + moves[i].getAnalyzedMoves());
        }
        return moveList;
    }

    public void stop() {
    }

    public void addListener(UciListener listener) {
        uci.addListener(listener);
    }

    public static void main(String[] args) {
        UciAnalyzer analyzer = new UciAnalyzer(null);
        Board board = new StandardBoard();
        String boardFEN = ChessBoard.STARTPOSITION;
        MoveList moves = analyzer.analizePosition(board, Side.WHITE);
        System.out.println("Analysis: " + moves.get(0));
        board.move(moves.get(0));
        boardFEN = ChessBoard.moveFromFEN(boardFEN, moves.get(0).toString());
        moves = analyzer.analizePosition(board, Side.BLACK);
        System.out.println("Analysis: " + moves.get(0));
    }
}
