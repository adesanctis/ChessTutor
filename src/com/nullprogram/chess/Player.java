package com.nullprogram.chess;

/**
 * A Player is a class that can take a turn.
 *
 * The game driver will call takeTurn() on a player when it is the
 * player's turn. The player returns the desired move.
 */
public interface Player {

    /**
     * Inform the player it is time to take its turn.
     *
     * @param board the current board
     * @param side  the player's side
     * @return the selected move for this player
     */
    public Move takeTurn(Board board, Piece.Side side);
//    public void opponentTurn(Board board, Move move, Piece.Side side);
//    public MoveList analyze(Board board, Piece.Side side);
//    public MoveList analyzeMove(Board board, Move move, Piece.Side side);
    public void opponentTurn(Board board, Move move, Piece.Side side);
    
    public void dispose();
    
}
