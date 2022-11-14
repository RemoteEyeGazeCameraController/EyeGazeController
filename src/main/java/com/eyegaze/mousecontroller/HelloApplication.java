package com.eyegaze.mousecontroller;// Java program to move a mouse from the initial
// location to a specified location
import java.awt.*;
import java.awt.event.*;
import java.util.TimerTask;
import javax.swing.*;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

class HelloApplication extends Frame implements ActionListener {
    // Frame
    static JFrame f;

    // default constructor
    HelloApplication()
    {
    }

    // main function
    public static void main(String args[])
    {
        // object of class
        HelloApplication rm = new HelloApplication();
        // create a frame
        f = new JFrame("robomouse");
        // set the frame to close on exit
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // create a button
        Button b = new Button("OK");
        // add actionListener
        b.addActionListener(rm);
        // create a panel
        Panel p = new Panel();
        // add items to panel
        p.add(b);
        f.add(p);
        // setsize of frame
        f.setSize(300, 300);
        f.show();
    }

    // if button is pressed
    public void actionPerformed(ActionEvent e)
    {
        Timer timer = new Timer();
        try {
            Robot r = new Robot();
            int xi1, yi1, xi, yi;

//            // get initial location
//            Point p = MouseInfo.getPointerInfo().getLocation();
//            xi = p.x;
//            yi = p.y;

            // get x and y points
            xi1 = 55;
            yi1 = 46;
            //int i = xi, j = yi;


            r.mouseMove(xi1, yi1);
            r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);


            TimeUnit.MILLISECONDS.sleep(2000);
            //r.keyPress(KeyEvent.VK_F11);
            //r.keyRelease(KeyEvent.VK_F11);
            r.mouseMove(235, 192);
            r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            r.mouseMove(645, 321);
            r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        }
        catch (Exception evt) {
            System.err.println(evt.getMessage());
        }
    }
}
