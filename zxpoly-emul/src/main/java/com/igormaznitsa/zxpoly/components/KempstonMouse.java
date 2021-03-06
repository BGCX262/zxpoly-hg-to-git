/*
 * Copyright (C) 2015 Raydac Research Group Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.igormaznitsa.zxpoly.components;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;

public final class KempstonMouse extends MouseAdapter implements IODevice {

  private final int MOUSE_BUTTON_LEFT = 1;
  private final int MOUSE_BUTTON_RIGHT = 2;
  private final int MOUSE_BUTTON_CENTRAL = 4;
  private final int MOUSE_BUTTONS_NON_ACTIVE = 0xFF;

  private final Motherboard board;
  private final AtomicInteger coordX = new AtomicInteger();
  private final AtomicInteger coordY = new AtomicInteger();
  private final AtomicInteger buttons = new AtomicInteger(MOUSE_BUTTONS_NON_ACTIVE);

  private final VideoController videoController;

  private final Robot robot;

  private int pcMouseX;
  private int pcMouseY;
  
  public KempstonMouse(final Motherboard board) {
    try {
      this.robot = new Robot();
    }
    catch (AWTException ex) {
      throw new Error("Can't create robot", ex);
    }

    this.board = board;
    this.videoController = board.getVideoController();
    this.videoController.addMouseListener(this);
    this.videoController.addMouseMotionListener(this);
  }

  @Override
  public Motherboard getMotherboard() {
    return this.board;
  }

  @Override
  public int readIO(final ZXPolyModule module, final int port) {
    int result = 0;
    if (!module.isTRDOSActive()) {
      switch (port) {
        case 0xFBDF: {
          // coord X
          result = this.coordX.get();
        }
        break;
        case 0xFFDF: {
          // coord Y
          result = this.coordY.get();
        }
        break;
        case 0xFADF: {
          // buttons
          result = this.buttons.get();
        }
        break;
      }
    }
    return result;
  }

  @Override
  public void writeIO(final ZXPolyModule module, final int port, final int value) {
  }

  @Override
  public void preStep(boolean signalReset, boolean signalInt) {
    if (signalReset) {
      this.coordX.set(128);
      this.coordY.set(86);
      this.buttons.set(0);
    }
  }

  @Override
  public String getName() {
    return "KempstonMouse";
  }

  @Override
  public void mouseExited(final MouseEvent e) {
    if (this.videoController.isHoldMouse()) {
      int x = e.getX();
      int y = e.getY();
      
      if (x<0){
        this.pcMouseX += this.videoController.getWidth();
        x = this.videoController.getWidth()+x;
      }else if (x>=this.videoController.getWidth()){
        this.pcMouseX -= this.videoController.getWidth();
        x = x-this.videoController.getWidth();
      }
      
      if (y<0){
        this.pcMouseY += this.videoController.getHeight();
        y = this.videoController.getHeight()+y;
      }else if (y>=this.videoController.getHeight()){
        this.pcMouseY += this.videoController.getHeight();
        y = y-this.videoController.getHeight();
      }
      
      final Point thepoint = new Point(x, y);
      SwingUtilities.convertPointToScreen(thepoint, this.videoController);
      this.robot.mouseMove(thepoint.x,thepoint.y);
    }
    else {
      this.buttons.set(MOUSE_BUTTONS_NON_ACTIVE);
    }
  }

  @Override
  public void mouseEntered(final MouseEvent e) {
    if (this.videoController.isHoldMouse()) {
    }
    else {
      this.buttons.set(MOUSE_BUTTONS_NON_ACTIVE);
    }
  }

  @Override
  public void mouseReleased(final MouseEvent e) {
    if (this.videoController.isHoldMouse()) {
      int button = 0;
      switch (e.getButton()) {
        case MouseEvent.BUTTON1: {
          button = MOUSE_BUTTON_LEFT;
        }
        break;
        case MouseEvent.BUTTON2: {
          button = MOUSE_BUTTON_CENTRAL;
        }
        break;
        case MouseEvent.BUTTON3: {
          button = MOUSE_BUTTON_RIGHT;
        }
        break;
      }
      this.buttons.set(this.buttons.get() | button);
    }
    else {
      this.buttons.set(MOUSE_BUTTONS_NON_ACTIVE);
    }
  }

  @Override
  public void mousePressed(final MouseEvent e) {
    if (this.videoController.isHoldMouse()) {

      int button = 0;
      switch (e.getButton()) {
        case MouseEvent.BUTTON1: {
          button = MOUSE_BUTTON_LEFT;
        }
        break;
        case MouseEvent.BUTTON2: {
          button = MOUSE_BUTTON_CENTRAL;
        }
        break;
        case MouseEvent.BUTTON3: {
          button = MOUSE_BUTTON_RIGHT;
        }
        break;
      }
      this.buttons.set((this.buttons.get() & ~button) & MOUSE_BUTTONS_NON_ACTIVE);
    }
    else {
      this.buttons.set(MOUSE_BUTTONS_NON_ACTIVE);
      this.videoController.setHoldMouse(true);
      this.pcMouseX = e.getX();
      this.pcMouseY = e.getY();
    }
  }

  @Override
  public void mouseDragged(final MouseEvent e) {
    this.mouseMoved(e);
  }

  @Override
  public void mouseMoved(final MouseEvent e) {
    if (this.videoController.isHoldMouse()) {
      final int dx = (e.getX() - this.pcMouseX) / this.videoController.getZoom();
      final int dy = (e.getY() - this.pcMouseY) / this.videoController.getZoom();
      this.coordX.set((this.coordX.get()+dx)&0xFF);
      this.coordY.set((this.coordY.get()-dy) & 0xFF);
      this.pcMouseX = e.getX();
      this.pcMouseY = e.getY();
    }
    else {
      this.buttons.set(MOUSE_BUTTONS_NON_ACTIVE);
    }
  }

  @Override
  public void postStep(long spentMachineCyclesForStep) {
  }
  
  public void reset(){
    this.buttons.set(MOUSE_BUTTONS_NON_ACTIVE);
    this.coordX.set(0x80);
    this.coordY.set(0x70);
  }

}
