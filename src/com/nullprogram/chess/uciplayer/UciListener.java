/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nullprogram.chess.uciplayer;

/**
 *
 * @author Administrator
 */
public interface UciListener {
    public void engineUpdate(UCIChess.InfoDetailed details);
}
