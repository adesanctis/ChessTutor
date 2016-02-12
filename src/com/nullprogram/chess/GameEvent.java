package com.nullprogram.chess;

/**
 * A semantic event that indicated an event occured in a Game.
 */
public class GameEvent {

    /** An event where a turn was taken. */
    public static final int TURN = 0;

    /** An event where the game's status (progress, status line) changed. */
    public static final int STATUS = 1;

    /** A game ending event. */
    public static final int GAME_END = 2;

    /** The type of event that occured. */
    private final int type;

    /** The type of event that occured. */
    private final Game game;
    private final String message;

    /**
     * Create a new game event.
     *
     * @param where  the game where the event occurred
     * @param eventtype  the type of event to create
     */
    public GameEvent(final Game where, final int eventtype, String message) {
        game = where;
        type = eventtype;
        this.message = message;
    }

    /**
     * Get the type of the event.
     *
     * @return the type of the event
     */
    public final int getType() {
        return type;
    }

    /**
     * Get the game where the event occured.
     *
     * @return the game where the event occured
     */
    public final Game getGame() {
        return game;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }
}
