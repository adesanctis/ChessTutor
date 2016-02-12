package com.nullprogram.chess.gui;

import com.nullprogram.chess.*;
import com.nullprogram.chess.Piece.Side;
import com.nullprogram.chess.uciplayer.UciAnalyzer;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import sun.misc.BASE64Encoder;

/**
 * Displays a board and exposes local players.
 *
 * This swing element displays a game board and can also behave as a player as
 * needed.
 */
public class BoardPanel extends JComponent
        implements MouseListener, Player, GameListener {

    /**
     * This class's Logger.
     */
    private static final Logger LOG =
            Logger.getLogger("com.nullprogram.chess.gui.BoardPanel");
    /**
     * Size of a tile in working coordinates.
     */
    private static final double TILE_SIZE = 200.0;
    /**
     * Shape provided for drawing background tiles.
     */
    private static final Shape TILE =
            new Rectangle2D.Double(0, 0, TILE_SIZE, TILE_SIZE);
    /**
     * Padding between the highlight and tile border.
     */
    static final int HIGHLIGHT_PADDING = 15;
    /**
     * Thickness of highlighting.
     */
    static final Stroke HIGHLIGHT_STROKE = new BasicStroke(12);
    /**
     * Shape for drawing the highlights.
     */
    private static final Shape[] HIGHLIGHT = new Shape[]{
        new RoundRectangle2D.Double(HIGHLIGHT_PADDING, HIGHLIGHT_PADDING,
        TILE_SIZE - HIGHLIGHT_PADDING * 2,
        TILE_SIZE - HIGHLIGHT_PADDING * 2,
        HIGHLIGHT_PADDING * 4,
        HIGHLIGHT_PADDING * 4),
        new RoundRectangle2D.Double(HIGHLIGHT_PADDING * 2, HIGHLIGHT_PADDING * 2,
        TILE_SIZE - HIGHLIGHT_PADDING * 4,
        TILE_SIZE - HIGHLIGHT_PADDING * 4,
        HIGHLIGHT_PADDING * 4,
        HIGHLIGHT_PADDING * 4),
        new RoundRectangle2D.Double(HIGHLIGHT_PADDING * 3, HIGHLIGHT_PADDING * 3,
        TILE_SIZE - HIGHLIGHT_PADDING * 6,
        TILE_SIZE - HIGHLIGHT_PADDING * 6,
        HIGHLIGHT_PADDING * 4,
        HIGHLIGHT_PADDING * 4),
        new RoundRectangle2D.Double(HIGHLIGHT_PADDING * 4, HIGHLIGHT_PADDING * 4,
        TILE_SIZE - HIGHLIGHT_PADDING * 8,
        TILE_SIZE - HIGHLIGHT_PADDING * 8,
        HIGHLIGHT_PADDING * 4,
        HIGHLIGHT_PADDING * 4),
        new RoundRectangle2D.Double(HIGHLIGHT_PADDING * 5, HIGHLIGHT_PADDING * 5,
        TILE_SIZE - HIGHLIGHT_PADDING * 10,
        TILE_SIZE - HIGHLIGHT_PADDING * 10,
        HIGHLIGHT_PADDING * 4,
        HIGHLIGHT_PADDING * 4),};
    /**
     * Version for object serialization.
     */
    private static final long serialVersionUID = 1L;
    /**
     * The board being displayed.
     */
    private Board board;
    /**
     * Indicate flipped status.
     */
    private boolean flipped = true;
    /**
     * The currently selected tile.
     */
    private Position selected = null;
    /**
     * The list of moves for the selected tile.
     */
    private MoveList moves = null;
    /**
     * The color for the dark tiles on the board.
     */
    static final Color DARK = new Color(0xD1, 0x8B, 0x47);
    /**
     * The color for the light tiles on the board.
     */
    static final Color LIGHT = new Color(0xFF, 0xCE, 0x9E);
    /**
     * Border color for a selected tile.
     */
    static final Color SELECTED = new Color(0x00, 0xFF, 0xFF);
    /**
     * Border color for a selected tile.
     */
    static final Color USER_POSSIBLE = new Color(0x00, 0xFF, 0x00);
    static final Color ENGINE_POSSIBLE = new Color(0x00, 0x00, 0xff, 128);
    static final Color OPPONENT_POSSIBLE = new Color(0xFF, 0x00, 0x00, 128);
    static final Color SEQUENCED = new Color(0x80, 0x80, 0xff, 176);
    static final Color CAPTURED = new Color(0xFF, 0x00, 0x00);
    static final Color SUGGESTED = new Color(0x00, 0x00, 0x00);
    /**
     * Border color for a highlighted movement tile.
     */
    static final Color MOVEMENT = new Color(0x7F, 0x00, 0x00);
    /**
     * Last move highlight color.
     */
    static final Color LAST = new Color(0x00, 0x7F, 0xFF);
    /**
     * Minimum size of a tile, in pixels.
     */
    static final int MIN_SIZE = 25;
    /**
     * Preferred size of a tile, in pixels.
     */
    static final int PREF_SIZE = 75;
    /**
     * The current interaction mode.
     */
    private Mode mode = Mode.WAIT;
    /**
     * Current player making a move, when interactive.
     */
    private Piece.Side side;
    /**
     * Latch to hold down the Game thread while the user makes a selection.
     */
    private CountDownLatch latch;
    /**
     * The move selected by the player.
     */
    private Move selectedMove;
    private Move suggestedMove;
    private UciAnalyzer analyzer;

    /**
     * @return the analyzer
     */
    public UciAnalyzer getAnalyzer() {
        return analyzer;
    }

    /**
     * @param analyzer the analyzer to set
     */
    public void setAnalyzer(UciAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    @Override
    public void opponentTurn(Board board, Move move, Side side) {
        opponentResponses = null;
    }

    /**
     * The interaction modes.
     */
    private enum Mode {

        /**
         * Don't interact with the player.
         */
        WAIT,
        /**
         * Interact with the player.
         */
        PLAYER;
    }
    private Writer document = null;

    /**
     * Hidden constructor.
     */
    protected BoardPanel() {
        initWriter();
    }
    
    private AnalysisListener analysisListener = null;

    /**
     * Create a new display for given board.
     *
     * @param displayBoard the board to be displayed
     */
    public BoardPanel(final Board displayBoard, AnalysisListener listener) {
        board = displayBoard;
        this.analysisListener = listener;
        updateSize();
        addMouseListener(this);
        initWriter();
    }

    private void initWriter() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmmss");
        String date = sdf.format(new Date());
        String path = System.getProperty("user.home")+"/Chess Games";
        try {
            new File(path).mkdirs();
            document = new FileWriter(new File(path + "/Game-" + date + ".html"));
//            document = new StringWriter();
            document.append("<html>").append("<body>").append("\n");
            document.append("<h1>").append("Game played on ").append(date).append("</h1>").append("\n");
        } catch (IOException ex) {
            Logger.getLogger(BoardPanel.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     * Set the preferred board size.
     */
    private void updateSize() {
        setPreferredSize(new Dimension(PREF_SIZE * board.getWidth(),
                PREF_SIZE * board.getHeight()));
        setMinimumSize(new Dimension(MIN_SIZE * board.getWidth(),
                MIN_SIZE * board.getHeight()));
    }

    @Override
    public final Dimension getPreferredSize() {
        return new Dimension(PREF_SIZE * board.getWidth(),
                PREF_SIZE * board.getHeight());
    }

    /**
     * Change the board to be displayed.
     *
     * @param b the new board
     */
    public final void setBoard(final Board b) {
        board = b;
        updateSize();
        repaint();
    }

    /**
     * Change the board to be displayed.
     *
     * @return display's board
     */
    public final Board getBoard() {
        return board;
    }

    /**
     * Return the transform between working space and drawing space.
     *
     * @return display transform
     */
    public final AffineTransform getTransform() {
        AffineTransform at = new AffineTransform();
        at.scale(getWidth() / (TILE_SIZE * board.getWidth()),
                getHeight() / (TILE_SIZE * board.getHeight()));
        return at;
    }

    /**
     * Standard painting method.
     *
     * @param graphics the drawing surface
     */
    public final void paintComponent(final Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics;
        int h = board.getHeight();
        int w = board.getWidth();
        g.transform(getTransform());
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);

        /*
         * Temp AffineTransform for the method
         */
        AffineTransform at = new AffineTransform();

        /*
         * Draw the background
         */
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if ((x + y) % 2 == 0) {
                    g.setColor(LIGHT);
                } else {
                    g.setColor(DARK);
                }
                at.setToTranslation(x * TILE_SIZE, y * TILE_SIZE);
                g.fill(at.createTransformedShape(TILE));
                g.setColor(new Color(0,0,0,128));
                g.setFont(new Font("Arial",Font.PLAIN,35));
                if (flipped) {
                    g.drawString("abcdefgh".substring(x,x+1)+"87654321".substring(y,y+1), (int)(x* TILE_SIZE), (int)(y* TILE_SIZE)+35);            
                } else {
                    g.drawString("abcdefgh".substring(x,x+1)+"12345678".substring(y,y+1), (int)(x* TILE_SIZE), (int)(y* TILE_SIZE)+35);                                
                }
            }
        }



        /*
         * Draw white avaliable moves
         */
        MoveList list = board.allCaptureMoves(side, false);
        for (Move move : list) {
            g.setColor(USER_POSSIBLE);
            highlight(g, move.getDest(), 1);
        }

        list = board.allCaptureMoves(Piece.opposite(side), false);
        for (Move move : list) {
            g.setColor(CAPTURED);
            highlight(g, move.getDest(), 2);
        }

        int drawingPosition = 5;
        int maximumStrength = 60;
        int lineRatio = maximumStrength / drawingPosition;

        if (analyzedMoves != null) {
            list = analyzedMoves;
            int counter = 0;
            for (Move move : list) {
                g.setColor(ENGINE_POSSIBLE);
//                if (move.getScore() > 0) {
                if (counter < drawingPosition) {
//                    highlight(g, move.getDest(), 2);
                    drawArrow(g, move, new BasicStroke(lineRatio * (drawingPosition - counter)));
//                } else {
//                    highlight(g, move.getDest(), 4);
//                    drawArrow(g, move, new BasicStroke(10));
                }
                counter++;
                if (counter >= drawingPosition) {
                    break;
                }
//                }
            }
        }

        if (opponentResponses != null) {
            list = opponentResponses;
            int counter = 1;
            for (Move move : list) {
                g.setColor(OPPONENT_POSSIBLE);
//                if (move.getScore() > 0) {
                if (counter < drawingPosition) {
//                    highlight(g, move.getDest(), counter);
                    drawArrow(g, move, new BasicStroke(lineRatio * (drawingPosition - counter)));
//                } else {
//                    highlight(g, move.getDest(), 4);
//                    drawArrow(g, move, new BasicStroke(10));
                }
                counter++;
                if (counter > drawingPosition) {
                    break;
                }
//                }
            }
        }
        
        if (sequencedMoves != null) {
            list = sequencedMoves;
            int counter = 0;
            drawingPosition = list.size();
            for (Move move : list) {
                g.setColor(SEQUENCED);
//                if (move.getScore() > 0) {
                if (counter < drawingPosition) {
//                    highlight(g, move.getDest(), 2);
                    drawArrow(g, move, new BasicStroke(( (maximumStrength/list.size()) * (list.size()- counter))));
//                } else {
//                    highlight(g, move.getDest(), 4);
//                    drawArrow(g, move, new BasicStroke(10));
                }
                counter++;
                if (counter >= drawingPosition) {
                    break;
                }
//                }
            }
        }
        

        /*
         * Draw last move
         */
        Move last = board.last();
        if (last != null) {
            g.setColor(LAST);
            highlight(g, last.getOrigin(), 0);
            highlight(g, last.getDest(), 0);
        }

        if (suggestedMove != null) {
            g.setColor(SUGGESTED);
            highlight(g, suggestedMove.getOrigin(), 0);
            highlight(g, suggestedMove.getDest(), 0);
        }


        /*
         * Draw selected square
         */
        if (selected != null) {
            g.setColor(SELECTED);
            highlight(g, selected, 0);

            /*
             * Draw piece moves
             */
            if (moves != null) {
                g.setColor(MOVEMENT);
                for (Move move : moves) {
                    highlight(g, move.getDest(), 0);
                }
            }
        }

        /*
         * Place the pieces
         */
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Piece p = board.getPiece(new Position(x, y));
                if (p != null) {
                    Image tile = p.getImage();
                    int yy = y;
                    if (flipped) {
                        yy = board.getHeight() - 1 - y;
                    }
                    at.setToTranslation(x * TILE_SIZE, yy * TILE_SIZE);
                    g.drawImage(tile, at, null);
                }
            }
        }

    }

    private void drawArrow(Graphics2D g, Move move, Stroke stroke) {
        g.setStroke(stroke);
        AffineTransform at = new AffineTransform();

        int x = (int) (move.getOrigin().getX() * TILE_SIZE + TILE_SIZE / 2);
        int y = (int) (move.getOrigin().getY() * TILE_SIZE + TILE_SIZE / 2);
        int xx = (int) (move.getDest().getX() * TILE_SIZE + TILE_SIZE / 2);
        int yy = (int) (move.getDest().getY() * TILE_SIZE + TILE_SIZE / 2);
        if (flipped) {
            y = board.getHeight() * (int) TILE_SIZE - 1 - y;
            yy = board.getHeight() * (int) TILE_SIZE - 1 - yy;
        }

        float arrowWidth = 10.0f;
        float theta = 0.423f;
        int[] xPoints = new int[3];
        int[] yPoints = new int[3];
        float[] vecLine = new float[2];
        float[] vecLeft = new float[2];
        float fLength;
        float th;
        float ta;
        float baseX, baseY;

        xPoints[ 0] = xx;
        yPoints[ 0] = yy;

        // build the line vector
        vecLine[ 0] = (float) xPoints[ 0] - x;
        vecLine[ 1] = (float) yPoints[ 0] - y;

        // build the arrow base vector - normal to the line
        vecLeft[ 0] = -vecLine[ 1];
        vecLeft[ 1] = vecLine[ 0];

        // setup length parameters
        fLength = (float) Math.sqrt(vecLine[0] * vecLine[0] + vecLine[1] * vecLine[1]);
        th = arrowWidth / (2.0f * fLength) * 10;
        ta = arrowWidth / (2.0f * ((float) Math.tan(theta) / 2.0f) * fLength) * 4;

        // find the base of the arrow
        baseX = ((float) xPoints[ 0] - ta * vecLine[0]);
        baseY = ((float) yPoints[ 0] - ta * vecLine[1]);

        // build the points on the sides of the arrow
        xPoints[ 1] = (int) (baseX + th * vecLeft[0]);
        yPoints[ 1] = (int) (baseY + th * vecLeft[1]);
        xPoints[ 2] = (int) (baseX - th * vecLeft[0]);
        yPoints[ 2] = (int) (baseY - th * vecLeft[1]);

        g.drawLine(x, y, (int) baseX, (int) baseY);
        g.fillPolygon(xPoints, yPoints, 3);
    }

    /**
     * Highlight the given tile on the board using the current color.
     *
     * @param g the drawing surface
     * @param pos position to highlight
     */
    private void highlight(final Graphics2D g, final Position pos, int shape) {
        int x = pos.getX();
        int y = pos.getY();
        if (flipped) {
            y = board.getHeight() - 1 - y;
        }
        g.setStroke(HIGHLIGHT_STROKE);
        AffineTransform at = new AffineTransform();
        at.translate(x * TILE_SIZE, y * TILE_SIZE);
        try {
            g.draw(at.createTransformedShape(HIGHLIGHT[shape]));
        } catch (Exception e) {
            int yo = 0;
        }
    }

    @Override
    public final void mouseReleased(final MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            leftClick(e);
        }
        if (SwingUtilities.isRightMouseButton(e)) {
            rightClick(e);
        }
        repaint();
    }

    /**
     * Handle the event when the left button is clicked.
     *
     * @param e the mouse event
     */
    private void leftClick(final MouseEvent e) {
        if (mode == Mode.WAIT) {
            return;
        }

        Position pos = getPixelPosition(e.getPoint());
        if (!board.inRange(pos)) {
            /*
             * Click was outside the board, somehow.
             */
            return;
        }
        if (pos != null) {
            if (pos.equals(selected)) {
                /*
                 * Deselect
                 */
                selected = null;
                moves = null;
            } else if (moves != null && moves.containsDest(pos)) {
                /*
                 * Move selected piece
                 */
                mode = Mode.WAIT;
                Move move = moves.getMoveByDest(pos);
                selected = null;
                moves = null;
                selectedMove = move;
                latch.countDown();
                analyzedMoves = null;
                opponentResponses = null;
                analysisListener.updateAnalysis(null);
            } else {
                /*
                 * Select this position
                 */
                Piece p = board.getPiece(pos);
                if (p != null && p.getSide() == side) {
                    selected = pos;
                    moves = p.getMoves(true);
                }
                if (p != null && p.getSide() != side) {
                    selected = pos;
                    moves = p.getMoves(true);
                }
            }
        }
        repaint();
    }
    private MoveList opponentResponses;

    private void rightClick(final MouseEvent e) {
        if (mode == Mode.WAIT) {
            return;
        }

        Position pos = getPixelPosition(e.getPoint());
        if (!board.inRange(pos)) {
            /*
             * Click was outside the board, somehow.
             */
            return;
        }
        if (pos != null) {
            if (pos.equals(selected)) {
                /*
                 * Deselect
                 */
                selected = null;
                moves = null;
            } else if (moves != null && moves.containsDest(pos)) {
                /*
                 * Move selected piece
                 */
//                mode = Mode.WAIT;
                Move move = moves.getMoveByDest(pos);
                selected = null;
                moves = null;
                final Board analysisBoard = board.copy();
                analysisBoard.move(move);
                suggestedMove = move;
                repaint();
                analyzer.stop();
                Thread t = new Thread(new Runnable() {

                    public void run() {
//                        analyzer.takeTurn(analysisBoard, Piece.opposite(side));
                        opponentResponses = analyzer.analizePosition(analysisBoard,Piece.opposite(side));
                        if (analysisListener!=null) {
                            analysisListener.updateAnalysis(opponentResponses);
                        }
                        repaint();
                        synchronized (document) {

                            try {
                                document.append("<img src=\"").append("data:image/").append("png").append(";base64,").append(encodeToString("png")).append("\"/>").append("\n");
                                document.append("<p>").append("\n");

                                for (int i = 0; i < opponentResponses.size(); i++) {
                                    document.append(opponentResponses.get(i).toString() + ": " + opponentResponses.get(i).getScore() + " - " + opponentResponses.get(i).getAnalyzedMoves()).append("<br/>\n");
                                }
                                document.append("</p>").append("\n");
                            } catch (IOException ex) {
                                Logger.getLogger(BoardPanel.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                });
                try {
                    synchronized (document) {
                        document.append("<h3>").append("Starting analysis for " + move).append("</h3>").append("\n");
                    }
                } catch (IOException ex) {
                    Logger.getLogger(BoardPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
                t.start();


//                selectedMove = move;
//                latch.countDown();
            } else {
                /*
                 * Select this position
                 */
                Piece p = board.getPiece(pos);
                if (p != null && p.getSide() == side) {
                    selected = pos;
                    moves = p.getMoves(true);
                }
            }
        }
    }

    /**
     * Determine which tile a pixel point belongs to.
     *
     * @param p the point
     * @return the position on the board
     */
    private Position getPixelPosition(final Point2D p) {
        Point2D pout = null;
        try {
            pout = getTransform().inverseTransform(p, null);
        } catch (java.awt.geom.NoninvertibleTransformException t) {
            /*
             * This will never happen.
             */
            return null;
        }
        int x = (int) (pout.getX() / TILE_SIZE);
        int y = (int) (pout.getY() / TILE_SIZE);
        if (flipped) {
            y = board.getHeight() - 1 - y;
        }
        return new Position(x, y);
    }
    private MoveList analyzedMoves = null;    
    private MoveList sequencedMoves = null;    

    public void showMoves(String moves, Board board) {
        String[] splittedMoves = moves.split(" ");
        analyzedMoves = null;
        opponentResponses = null;
        sequencedMoves = new MoveList(board);
        for (int i=0; i<splittedMoves.length; i++) {
            Move move = new Move(splittedMoves[i]);
            sequencedMoves.add(move);
        }
        repaint();
    }
    
    
    public void analyzePosition(final Piece.Side currentSide) {
        suggestedMove = null;
        sequencedMoves = null;
        Thread analyzerThread = new Thread(new Runnable() {

            public void run() {
//                suggestedMove = analyzer.takeTurn(board.copy(), currentSide);
////                suggestedMove = analyzer.takeTurn(board.copy(), currentSide, suggestedMove.getScore()*.9);
                analyzedMoves = analyzer.analizePositionFast(board,currentSide);
//                analyzedMoves = analyzer.analizePosition(board,currentSide);
                analysisListener.updateAnalysis(analyzedMoves);
                repaint();
                analyzedMoves = analyzer.analizePosition(board,currentSide);
//                analyzedMoves = analyzer.analizePosition(board,currentSide);
                analysisListener.updateAnalysis(analyzedMoves);
                repaint();
                synchronized (document) {
                    try {
                        document.append("<img src=\"").append("data:image/").append("png").append(";base64,").append(encodeToString("png")).append("\"/>").append("\n");
                        document.append("<p>").append("\n");

                        for (int i = 0; i < analyzedMoves.size(); i++) {
                            document.append(analyzedMoves.get(i).toString() + ": " + analyzedMoves.get(i).getScore() + " - " + analyzedMoves.get(i).getAnalyzedMoves()).append("<br/>\n");
                        }
                        document.append("</p>").append("\n");
                    } catch (IOException ex) {
                        Logger.getLogger(BoardPanel.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }
            }
        });
        try {
            document.append("<h3>").append("Starting analysis for current board").append("</h3>").append("\n");
        } catch (IOException ex) {
            Logger.getLogger(BoardPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
        analyzerThread.start();
        

    }

    @Override
    public final Move takeTurn(final Board turnBoard, final Piece.Side currentSide) {
        try {

            suggestedMove = null;
            document.append("<hr/>").append("\n");


            latch = new CountDownLatch(1);
            board = turnBoard;
            side = currentSide;
            analyzedMoves = analyzer.analizePositionFast(board,currentSide);
            analysisListener.updateAnalysis(analyzedMoves);
            repaint();
            mode = Mode.PLAYER;
            try {
                latch.await();
            } catch (InterruptedException e) {
                LOG.warning("BoardPanel interrupted during turn.");
            }
            analyzer.stop();
            Board copyBoard = board.copy();
            copyBoard.move(selectedMove);
            sequencedMoves = null;
            opponentResponses = analyzer.analizePositionFast(copyBoard,Piece.opposite(currentSide));
            analysisListener.updateAnalysis(opponentResponses);
            
            document.append("<h2>").append("Move done: " + selectedMove).append("</h2>").append("\n");
            return selectedMove;
        } catch (IOException ex) {
            Logger.getLogger(BoardPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public final void gameEvent(final GameEvent e) {
        board = e.getGame().getBoard();
        if (e.getType() == GameEvent.TURN) {
            try {
                synchronized (document) {
                    document.append("<hr/>\n");
                    document.append("<img src=\"").append("data:image/").append("png").append(";base64,").append(encodeToString("png")).append("\"/>").append("\n");
                    document.flush();
                }
            } catch (IOException ex) {
                Logger.getLogger(BoardPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (e.getType() != GameEvent.STATUS) {
            repaint();
        }
        if (e.getMessage() != null) {
            try {
                synchronized (document) {
                    document.append("<p>").append(e.getMessage()).append("</p>").append("\n");
                }
            } catch (IOException ex) {
                Logger.getLogger(BoardPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Return the desired aspect ratio of the board.
     *
     * @return desired aspect ratio
     */
    public final double getRatio() {
        return board.getWidth() / (1.0 * board.getHeight());
    }

    /**
     * Set whether or not the board should be displayed flipped.
     *
     * @param value the new flipped state
     */
    public final void setFlipped(final boolean value) {
        flipped = value;
    }

    @Override
    public void mouseExited(final MouseEvent e) {
        /*
         * Do nothing
         */
    }

    @Override
    public void mouseEntered(final MouseEvent e) {
        /*
         * Do nothing
         */
    }

    @Override
    public void mouseClicked(final MouseEvent e) {
        /*
         * Do nothing
         */
    }

    @Override
    public void mousePressed(final MouseEvent e) {
        /*
         * Do nothing
         */
    }

    public void saveComponentAsJPEG(String filename) {
        Component myComponent = this;
        Dimension size = myComponent.getSize();
        BufferedImage myImage = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = myImage.createGraphics();
        myComponent.paint(g2);
        try {
            OutputStream out = new FileOutputStream(filename);
            JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
            encoder.encode(myImage);
            out.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private String encodeToString(String type) {
        Component myComponent = this;
        Dimension size = myComponent.getSize();
        BufferedImage myImage = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = myImage.createGraphics();
        myComponent.paint(g2);
        String imageString = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ImageIO.write(myImage, type, bos);
            byte[] imageBytes = bos.toByteArray();
            BASE64Encoder encoder = new BASE64Encoder();
            imageString = encoder.encode(imageBytes);
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imageString;
    }
}
