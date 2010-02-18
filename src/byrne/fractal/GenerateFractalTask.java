/*
This file is part of Fractoid
Copyright (C) 2010 David Byrne
david.r.byrne@gmail.com

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package byrne.fractal;

import android.os.AsyncTask;
import android.graphics.*;

public class GenerateFractalTask extends AsyncTask<Void, Bitmap, int[][]> {
  
  FractalParameters params;
  FractalView fractalView;
  long startTime;
  NativeLib mNativeLib;
  Paint paint;
  int[] colors;
  int prog = 0;
  
  public GenerateFractalTask(FractalParameters p, FractalView fv) {
    params = p;
    fractalView = fv;
    colors = params.getColorSet();
    paint = new Paint();
    paint.setStyle(Paint.Style.FILL_AND_STROKE);
    
    mNativeLib = new NativeLib();
  }
    
  private int[][] createBitmap() {
    
    ComplexEquation equation = params.getEquation();
    int power = equation.getPower();
    int trapFactor = params.getTrapFactor();
    Algorithm alg = params.getAlgorithm();
    
    double realmin = params.getRealMin();
    double realmax = params.getRealMax();
    double imagmin = params.getImagMin();
    double imagmax = params.getImagMax();
    
    double P = params.getP();
    double Q = params.getQ();
    
    double xtmp = 0;
    
    int xres = params.getXRes();
    int yres = params.getYRes();
    FractalType type = params.getType();
    
    Bitmap b = Bitmap.createBitmap(xres, yres, Bitmap.Config.ARGB_8888);
    Canvas c = new Canvas(b);
    
    int[] rowColors;
    int[][] fractalValues = params.getValues();

    double x=-1, y=-1, prev_x = -1, prev_y =-1,tmp_prev_x,tmp_prev_y, mu = 1;
    int index;
    boolean lessThanMax;

    double deltaP = (realmax - realmin)/xres;
    double deltaQ = (imagmax - imagmin)/yres;
    
    final int max = params.getMaxIterations();
    final int PASSES = 2;
    int updateCount=0;
    int state = 0;
      
    for (int rpass = 0; rpass < PASSES; rpass++) {
      paint.setStrokeWidth(PASSES-rpass);
      for (int row=0; row < yres; row += PASSES-rpass) {
        prog++;
        updateCount++;
        if (updateCount % 15 == 0) {
          fractalView.removeTouch();
          if (isCancelled()) {
            return fractalValues;
          }
          this.publishProgress(b);
        }
        if (row % 2 == 0) {
          if (rpass == 0) {
            state = 2;
          } else {
            state = 1;
          }
        } else {
          state = 0;
        }
        rowColors = mNativeLib.getFractalRow(row,state,fractalValues[row],power,max,trapFactor,
                                             equation.getInt(),type.getInt(),alg.getInt(),
                                             P,Q,realmin,realmax,imagmin,imagmax);
        
        //TODO Find a more elegant way to handle 2x2 and 1x1 rendering
        int step = 1;
        if (state > 0)
          step = 2;
        for(int col=(state%2); col < xres; col = col+step) {
          
          if (rowColors[col] >= 0) {
            if (params.getAlgorithm() == Algorithm.ESCAPE_TIME)
              paint.setColor(colors[(rowColors[col]%10200)/10]);  
            else
              paint.setColor(colors[rowColors[col]/10]);
          } else {
            paint.setColor(Color.BLACK);
          }
          fractalValues[row][col] = rowColors[col];
          //TODO Store results so color changes don't require recalculation
          c.drawPoint(col,row,paint);
        }
      }
    }
    this.publishProgress(b);
    return fractalValues;
  }
  
  @Override protected void onPreExecute() {
    startTime = System.currentTimeMillis();
  }
  @Override protected int[][] doInBackground(Void... unused) {   
    return createBitmap();
  }
  @Override protected void onProgressUpdate(Bitmap... b) {
    fractalView.setFractal(b[0]);
    fractalView.setProgress(((prog*2)/3.0f)/params.getYRes());
    fractalView.invalidate();
  }
  @Override protected void onPostExecute(int[][] v) {
    fractalView.setValues(v);
    fractalView.clearBackground();
    fractalView.setTime(System.currentTimeMillis()-startTime);
    fractalView.turnCalibrateButtonOn();
    fractalView.invalidate();
  }  
}
class NativeLib {
  public native void setResolution(int xres, int yres);
  public native int[] getFractalRow(int row,
                                    int state,
                                    int[] rowValues,
                                    int power,
                                    int max,
                                    int trapFactor,
                                    int equation,
                                    int type,
                                    int alg,
                                    double P,
                                    double Q,
                                    double realmin,
                                    double realmax,
                                    double imagmin,
                                    double imagmax);
  static {
    System.loadLibrary("FractalMath");
  }
}
