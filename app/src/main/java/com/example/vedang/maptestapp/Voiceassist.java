package com.example.vedang.maptestapp;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rajat on 3/30/2018.
 */

public class Voiceassist {



    public int voice (double x1,double y1,double x2,double y2,double x3,double y3) {



        double magnitude;
        double X1 = x2 - x1;
        double Y1 = y2 - y1;
        double X2 = x3 - x2;
        double Y2 = y3 - y2;
        double product = abs(X1 * Y2 - X2 * Y1);
        double mag1 = sqrt(X1 * X1 + Y1 * Y1);
        double mag2 = sqrt(X2 * X2 + Y2 * Y2);
        double sign = X1 * Y2 - X2 * Y1;
        double cin = product / (mag1 * mag2);
        if (cin <= 0.173) {
            return 0;

        } else if (sign > 0) {
            return 1;
        } else {
            return -1;
        }

    }

}
