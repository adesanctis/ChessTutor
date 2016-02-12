/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nullprogram.chess.uciplayer;

import com.nullprogram.chess.Piece.Side;
import com.nullprogram.chess.*;
import com.nullprogram.chess.uciplayer.UCIChess.InfoDetailed;

/**
 *
 * @author Administrator
 */
public class UciPlayer implements Player, UciListener {

    public static final int BEST_MOVE_NUMBER = 20;
    public static final int ANALYSIS_TIME = 20000;
    public static final String ENGINE_PATH = "C:/Documents and Settings/Administrator/Documenti/NetBeansProjects/StockFish/engine/SOS-51_Arena.exe";
    private UCIChess uci = null;
    private Game game;
    private String currentFEN = ChessBoard.STARTPOSITION;

    public UciPlayer(int level, Game game) {
        this.game = game;
        uci = new UCIChess(ENGINE_PATH);//stockfish-6-32.exe");
        if (uci.get_UciOk(true)) {
        }
        uci.send_Option_Name_WithValue("MultiPV", "" + BEST_MOVE_NUMBER);
        uci.send_Option_Name_WithValue("Skill Level", "" + level);
        uci.send_Option_Name_WithValue("UCI_Elo", "1200");


//        uci.send_Option_Name_WithValue("UCI_LimitStrength", "ON");
//        uci.send_Option_Name_WithValue("UCI_Elo", "500");                

//        uci.move_FromSTART("", true);
    }

    @Override
    public Move takeTurn(Board board, Side side) {
        game.setStatus("Analyzing " + side.name().toLowerCase() + "'s moves...");
        Move move = analyze(board, side).get(0);
//        retVal = analyze(board, side).get(0);
        currentFEN = ChessBoard.moveFromFEN(currentFEN, move.toString());

        return move;
    }

    public MoveList analyze(Board board, Side side) {
        MoveList retVal = null;
//        uci.move_FromSTART(board.moveHistory(), false);
        uci.move_FromFEN(currentFEN + " " + (side.equals(Side.BLACK) ? "b" : "w"), "", true);
//        uci.go_Think_MoveTime(1);
//        uci.go_Think_Depth(UciAnalyzer.ANALYSIS_LEVEL);
//        uci.go_Think_Depth(12);
        uci.go_Think_MoveTime(ANALYSIS_TIME);
        String bestMove = uci.get_BestMove(false);
        retVal = getAnalyzedMoves(board, uci);
        for (int i = 0; i < retVal.size(); i++) {
            game.setStatus("Analysis: " + retVal.get(i) + ": " + retVal.get(i).getScore() + " - " + retVal.get(i).getAnalyzedMoves());
        }

        return retVal;
    }

    public MoveList analyzeMove(Board board, Move move, Side side) {
        MoveList retVal = null;
        uci.move_FromFEN(ChessBoard.moveFromFEN(currentFEN, move.toString()) + " " + (side.equals(Side.BLACK) ? "b" : "w"), null, false);
        uci.go_Think_MoveTime(ANALYSIS_TIME);
//        uci.go_Think_MoveTime(1);
//        uci.go_Think_Depth(UciAnalyzer.ANALYSIS_LEVEL);
        String bestMove = uci.get_BestMove(true);
        retVal = getAnalyzedMoves(board, uci);
        return retVal;
    }

    private MoveList getAnalyzedMoves(Board board, UCIChess uci) {
        MoveList moveList = null;
        int counter = BEST_MOVE_NUMBER;
        int maxLines = uci.get_Number_DetailedInfo();
        Move move = null;
        for (int index = 0; index < maxLines; index++) {
            if (!"".equals(uci.get_DetailedInfo(index).getScoreCP())) {
                counter++;
                if (counter >= BEST_MOVE_NUMBER) {
                    counter = 0;
                    moveList = new MoveList(board);
                }
                move = new Move(uci.get_DetailedInfo(index).getPv().substring(0, 4));
                move.setScore(Double.parseDouble(uci.get_DetailedInfo(index).getScoreCP()));
                move.setAnalyzedMoves(uci.get_DetailedInfo(index).getPv());
                moveList.add(move);
            }
        }
        return moveList;
    }

    public static void main(String[] args) {
    }
    private MoveList analyzedMoves = null;

    @Override
    public void engineUpdate(InfoDetailed details) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void opponentTurn(Board board, Move move, Side side) {
        currentFEN = ChessBoard.moveFromFEN(currentFEN, move.toString());
    }
}
