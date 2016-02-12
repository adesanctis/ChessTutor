package com.nullprogram.chess.gui;

import com.nullprogram.chess.*;
import com.nullprogram.chess.boards.EmptyBoard;
import com.nullprogram.chess.pieces.ImageServer;
import com.nullprogram.chess.uciplayer.UciAnalyzer;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

/**
 * The JFrame that contains all GUI elements.
 */
public class ChessFrame extends JFrame
        implements ComponentListener, GameListener, AnalysisListener, TableModel {

    /**
     * Version for object serialization.
     */
    private static final long serialVersionUID = 1L;
    /**
     * The board display.
     */
    private final BoardPanel display;
    /**
     * The progress bar on the display.
     */
    private final StatusBar progress;
    private final JTable analysisTable;
    /**
     * The current game.
     */
    private Game game;

    /**
     * Create a new ChessFrame for the given board.
     */
    public ChessFrame() {
        super(Chess.getTitle());
        setResizable(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setIconImage(ImageServer.getTile("King-WHITE"));
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        MenuHandler handler = new MenuHandler(this);
        handler.setUpMenu();

        display = new BoardPanel(new EmptyBoard(), this);
        analysisTable = new JTable(5, 1);
        analysisTable.setModel(this);
        progress = new StatusBar(null);
        add(display);
        ScrollPane analysisPane = new ScrollPane();
        analysisPane.add(analysisTable);
        add(analysisPane);
        add(progress);
        pack();

        addComponentListener(this);
        setLocationRelativeTo(null);
        setVisible(true);

        analysisTable.addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent evnt) {
                if (evnt.getClickCount() == 1) {
                    String str = getValueAt(analysisTable.getSelectedRow(), 0).toString();
                    String moves = str.substring(str.indexOf("pv: ")+3).trim();
                    display.showMoves(moves, game.getBoard());
                }
            }
        });
    }

    public final void analysis() {
        game.setStatus("Analyzing " + game.getTurn().name().toLowerCase() + "'s moves...");
        display.analyzePosition(game.getTurn());
    }
    

    

    /**
     * Set up a new game.
     */
    public final void newGame() {
        NewGame ngFrame = new NewGame(this);
        ngFrame.setVisible(true);
        Game newGame = ngFrame.getGame();
        if (newGame == null) {
            return;
        }
        if (game != null) {
            game.end();
        }
        game = newGame;
        Board board = game.getBoard();
        display.setBoard(board);
        display.invalidate();
//        display.setAnalyzer(new MiniMax(game, "depth5"));
        display.setAnalyzer(new UciAnalyzer(game));
        setSize(getPreferredSize());

        progress.setGame(game);
        game.addGameListener(this);
        game.addGameListener(display);
        game.begin();
    }

    /**
     * Return the GUI (human) play handler.
     *
     * @return the player
     */
    public final Player getPlayer() {
        return display;
    }

    public final Player getEnginePlayer() {
        return display;
    }
    private MoveList analyzedMoves = null;

    @Override
    public void updateAnalysis(MoveList analyzedMoves) {
        this.analyzedMoves = analyzedMoves;
        analysisTable.repaint();
    }

    @Override
    public int getRowCount() {
        return 20;
    }

    @Override
    public int getColumnCount() {
        return 1;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return "Analysis";
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (analyzedMoves != null && analyzedMoves.size()>rowIndex && analyzedMoves.get(rowIndex)!=null) {
            return analyzedMoves.get(rowIndex) + " with score " + analyzedMoves.get(rowIndex).getScore() + " pv: " + analyzedMoves.get(rowIndex).getAnalyzedMoves();
        } else {
            return "-";
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
    }

    /**
     * Used for manaing menu events.
     */
    private class MenuHandler implements ActionListener {

        /**
         * The "Game" menu.
         */
        private JMenu game;
        /**
         * The parent chess frame, for callbacks.
         */
        private final ChessFrame frame;

        /**
         * Create the menu handler.
         *
         * @param parent parent frame
         */
        public MenuHandler(final ChessFrame parent) {
            frame = parent;
        }

        @Override
        public final void actionPerformed(final ActionEvent e) {
            if ("New Game".equals(e.getActionCommand())) {
                frame.newGame();
            } else if ("Analysis".equals(e.getActionCommand())) {
                frame.analysis();
            } else if ("Exit".equals(e.getActionCommand())) {
                System.exit(0);
            }
        }

        /**
         * Set up the menu bar.
         */
        public final void setUpMenu() {
            JMenuBar menuBar = new JMenuBar();

            game = new JMenu("Game");
            game.setMnemonic('G');
            JMenuItem newGame = new JMenuItem("New Game");
            newGame.addActionListener(this);
            newGame.setMnemonic('N');
            game.add(newGame);
            game.add(new JSeparator());
            JMenuItem analysis = new JMenuItem("Analysis");
            analysis.addActionListener(this);
            game.add(analysis);
            game.add(new JSeparator());
            JMenuItem exitGame = new JMenuItem("Exit");
            exitGame.addActionListener(this);
            exitGame.setMnemonic('x');
            game.add(exitGame);
            menuBar.add(game);

            setJMenuBar(menuBar);
        }
    }

    @Override
    public final void componentResized(final ComponentEvent e) {
        if ((getExtendedState() & JFrame.MAXIMIZED_BOTH) != 0) {
            /*
             * If the frame is maxmized, the battle has been lost.
             */
            return;
        }
        double ratio = display.getRatio();
        double barh = progress.getPreferredSize().getHeight() * 6;
        Container p = getContentPane();
        Dimension d = null;
        if (p.getWidth() * ratio < (p.getHeight() - barh)) {
            d = new Dimension((int) ((p.getHeight() - barh) * ratio),
                    p.getHeight());
        } else if (p.getWidth() * ratio > (p.getHeight() - barh)) {
            d = new Dimension(p.getWidth(),
                    (int) (p.getWidth() / ratio + barh));
        }
        if (d != null) {
            p.setPreferredSize(d);
            pack();
        }
    }

    @Override
    public final void gameEvent(final GameEvent e) {
        progress.repaint();
    }

    @Override
    public void componentHidden(final ComponentEvent e) {
        /*
         * Do nothing.
         */
    }

    @Override
    public void componentMoved(final ComponentEvent e) {
        /*
         * Do nothing.
         */
    }

    @Override
    public void componentShown(final ComponentEvent e) {
        /*
         * Do nothing.
         */
    }
}
