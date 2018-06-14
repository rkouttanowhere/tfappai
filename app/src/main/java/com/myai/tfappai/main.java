package com.myai.tfappai;

import android.graphics.PointF;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.myai.tfappai.models.*;
import com.myai.tfappai.views.*;

import java.util.ArrayList;
import java.util.List;


public class main extends AppCompatActivity implements View.OnClickListener, View.OnTouchListener {
private static final int PIXEL_SIZE=28;
    private Button detect,clear;
    private TextView tv;
    private DrawView dv;
    private DrawModel dm;
    private float lastX;
    private float lastY;
    private PointF p=new PointF();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv =(TextView) findViewById(R.id.result);
        dv=(DrawView)findViewById(R.id.dv);
        dm= new DrawModel(PIXEL_SIZE,PIXEL_SIZE);
        dv.setModel(dm);
        dv.setOnTouchListener(this);
        clear=(Button)findViewById(R.id.clr);
        detect=(Button)findViewById(R.id.detect);
        clear.setOnClickListener(this);
        detect.setOnClickListener(this);
        loadModel();
    }
    @Override
    protected void onPause() {
        dv.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        dv.onResume();
        super.onResume();
    }

    private List<Classifier> mClassifier=new ArrayList<>();
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

public void loadModel()
{
    new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                mClassifier.add(
                        TensorFlowClassifier.create(getAssets(), "TensorFlow",
                                "opt_mnist_convnet-tf.pb", "labels.txt", PIXEL_SIZE,
                                "input", "output", true));
                mClassifier.add(
                        TensorFlowClassifier.create(getAssets(), "Keras",
                                "opt_mnist_convnet-keras.pb", "labels.txt", PIXEL_SIZE,
                                "conv2d_1_input", "dense_2/Softmax", false));
            } catch (final Exception e) {
                throw new RuntimeException("Error initializing classifiers!", e);
            }
        }
    }).start();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.clr) {
            dm.clear();
            dv.reset();
            dv.invalidate();
            tv.setText("");
        } else if (view.getId() == R.id.detect) {
            float pixels[] = dv.getPixelData();
            String text = "";
            for (Classifier classifier : mClassifier) {
                final Classification res = classifier.recognize(pixels);
                if (res.getLabel() == null) {
                    text += classifier.name() + ": ?\n";
                } else {
                    //else output its name
                    text += String.format("%s: %s, %s: %f\n", classifier.name(), res.getLabel(),"Accuracy",res.getConf());
                }
            }
            tv.setText(text);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction() & MotionEvent.ACTION_MASK;

        if (action == MotionEvent.ACTION_DOWN) {
            processTouchDown(event);
            return true;
        } else if (action == MotionEvent.ACTION_MOVE) {
            processTouchMove(event);
            return true;
        } else if (action == MotionEvent.ACTION_UP) {
            processTouchUp();
            return true;
        }
        return false;
    }


    private void processTouchDown(MotionEvent event) {
        lastX = event.getX();
        lastY = event.getY();
        dv.calcPos(lastX, lastY, p);
        float lastConvX = p.x;
        float lastConvY = p.y;
        dm.startLine(lastConvX, lastConvY);
    }

    private void processTouchMove(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        dv.calcPos(x, y, p);
        float newConvX = p.x;
        float newConvY = p.y;
        dm.addLineElem(newConvX, newConvY);

        lastX = x;
        lastY = y;
        dv.invalidate();
    }

    private void processTouchUp() {
        dm.endLine();
    }

   
}
