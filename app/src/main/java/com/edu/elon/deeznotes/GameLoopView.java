package com.edu.elon.deeznotes;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;

import java.util.ArrayList;

/**
 * Created by scottarmstrong on 10/21/15.
 */
public class GameLoopView extends SurfaceView implements SurfaceHolder.Callback {

    private GameLoopThread thread;
    private SurfaceHolder surfaceHolder;
    private Context context;

    private float downTouchX, downTouchY;
    private float upTouchX, upTouchY;
    private float moveTouchX, moveTouchY;
    private boolean isDownTouch;
    private boolean moved;
    private Delete deleteButton;
    private ArrayList<Note> notes;
    private int whichNote = -1;
    protected long downTouch;
    protected long upTouch;


    //private Notes notes;
    private ArrayList<Note> noteArray;

    public GameLoopView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        thread = new GameLoopThread();
       // notes = new Notes();
    }

    public void setNotes(ArrayList<Note> noteArray) {
        this.noteArray = noteArray;
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if(thread.getState() == Thread.State.TERMINATED){
            thread = new GameLoopThread();
        }

        thread.setIsRunning(true);
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        thread.setIsRunning(false);

        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // remember the last touch point

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            downTouchX = event.getX();
            downTouchY = event.getY();
            moveTouchX = downTouchX;
            moveTouchY = downTouchY;
            isDownTouch = true;
            moved = false;
            downTouch = System.currentTimeMillis();
        }

        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            moveTouchX = event.getX();
            moveTouchY = event.getY();
            moved = true;
        }

        if(event.getAction()== MotionEvent.ACTION_UP){
            upTouchX = event.getX();
            upTouchY = event.getY();
            upTouch = System.currentTimeMillis();

            if(moved){
                moveTouchX = upTouchX;
                moveTouchY = upTouchY;
            }




            if (upTouchX > deleteButton.x - deleteButton.width/2
                    && upTouchX < deleteButton.x + deleteButton.width/2
                    && upTouchY > deleteButton.y - deleteButton.height/2
                    && upTouchY < deleteButton.y + deleteButton.height/2) {
                System.out.println("SHOULD DELETE " + whichNote);

                // want to delete something?
                if (whichNote >= 0) {
                    noteArray.get(whichNote).delete();
                    whichNote = -1;
                }
            }

            isDownTouch = false;
        }
        return true;

    }

    private class GameLoopThread extends Thread{

        ImageButton button = (ImageButton) findViewById(R.id.addButton);
        private boolean isRunning = false;
        private long lastTime;

        private Note note;
        private Note note2;

        private Add addButton;
        private int ID;

        private boolean intentsForIdiots = false;


        public GameLoopThread() {
            ID = 0;
            note = new Note(context);
            note2 = new Note(context);
            deleteButton  = new Delete(context);
            addButton = new Add(context);

            moveTouchX = note.x;
            moveTouchY = note.y;

            notes = new ArrayList<Note>();
            notes.add(note);

            //note2 = new Note(context);
            moveTouchX = note2.x;
            moveTouchY = note2.y;
        }

        public void setIsRunning(boolean isRunning) {
            this.isRunning = isRunning;
        }

        // the main loop
        @Override
        public void run() {

            lastTime = System.currentTimeMillis();

            while (isRunning) {

                // grab hold of the canvas
                Canvas canvas = surfaceHolder.lockCanvas();
                if (canvas == null) {
                    // trouble -- exit nicely
                    isRunning = false;
                    continue;
                }

                synchronized (surfaceHolder) {

                    // compute how much time since last time around
                    long now = System.currentTimeMillis();
                    double elapsed = (now - lastTime) / 1000.0;
                    lastTime = now;

                    // update/draw
                    doUpdate(elapsed);
                    doDraw(canvas);

                    //updateFPS(now);
                }

                // release the canvas
                if (canvas != null) {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
        // move all objects in the game
        private void doUpdate(double elapsed) {
            for (int i = 0; i <noteArray.size(); i ++) {
                if (downTouchX <= noteArray.get(i).x + (noteArray.get(i).width/2) && downTouchX >= noteArray.get(i).x - (noteArray.get(i).width/2) && downTouchY <= noteArray.get(i).y + (noteArray.get(i).height/2) && downTouchY >= noteArray.get(i).y - (noteArray.get(i).height/2)) {
                    if (downTouch - upTouch < 500){
                        intentsForIdiots = true;
                    }
                    noteArray.get(i).isSelected = true;
                    whichNote = i;
                    //notes.get(i).doUpdate(elapsed, moveTouchX, moveTouchY);
                }
                if (isDownTouch == false) {
                    noteArray.get(i).isSelected = false;
                }

                if (isDownTouch == true && noteArray.get(i).isSelected == true) {
                    noteArray.get(i).doUpdate(elapsed, moveTouchX, moveTouchY);
                }

                if(upTouchX <= addButton.x + (addButton.width/2) && upTouchX >= addButton.x - (addButton.width/2) && upTouchY <= addButton.y + (addButton.height/2) && upTouchY >= addButton.y - (addButton.height/2)){
                    System.out.println("new note");
                    Note note3 = new Note(context);
                    noteArray.add(new Note(context));
                    int size = noteArray.size();
                    noteArray.get(size-1).IDNumber = ID;
                    ID ++;
                    upTouchX = 25;
                    upTouchY = 25;
                }
            }
        }

        // draw all objects in the game
        private void doDraw(Canvas canvas) {

            // draw the background
            canvas.drawColor(Color.argb(255, 126, 192, 238));
            deleteButton.doDraw(canvas);
            addButton.doDraw(canvas);

            for (int i = 0; i < noteArray.size(); i ++) {
                noteArray.get(i).doDraw(canvas);
            }

        }

        public String getStrings() {
            String butthead = new String("");
            //butthead = title + text;
            return butthead;

        }

        public Boolean booleanCheck(){
            return intentsForIdiots;
        }
    }
}
