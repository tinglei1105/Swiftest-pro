package com.example.swiftest.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.swiftest.R;

import java.util.ArrayList;
import java.util.Collections;

public class SampleView extends View {
    private static final String TAG = "MyView";
    private Paint paint = new Paint();
    private Path path1 = new Path();

    final static private int sample_num = 100;
    final static private int window_size = 10;
    ArrayList<Double> speedSample;


    public SampleView(Context context) {
        super(context);
        this.speedSample = new ArrayList<>();
    }

    public SampleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        Log.i(TAG,"2");
        //就是通过修改画笔的一些参数设置
        paint = new Paint(); //新建一个画笔对象
        paint.setAntiAlias(true);//抗锯齿功能
        paint.setColor(getResources().getColor(R.color.sampleColor));  //设置画笔颜色
        paint.setStyle(Paint.Style.STROKE);//设置填充样式 中空
        paint.setStrokeWidth(10);//设置画笔宽度 ，单位px
        this.speedSample = new ArrayList<>();
//        this.speedSample.add(0.0);
    }

    public SampleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Log.i(TAG,"3");

    }

    public SampleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        Log.i(TAG,"4");
    }

    public void setSpeedSamples(ArrayList<Double> speedSample){
        this.speedSample = (ArrayList) speedSample.clone();
//        Log.d("!!!!!setSpeedSamples:", Integer.toString(this.speedSample.size()));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //canvas.drawColor();
        paint.setAntiAlias(true);//抗锯齿
        paint.setColor(getResources().getColor(R.color.sampleColor));//画笔颜色
        paint.setStyle(Paint.Style.FILL);//描边模
        paint.setStrokeWidth(4f);//设置画笔粗细度
        paint.setTextSize(52f);
        paint.setTextAlign(Paint.Align.CENTER);
        drawsth(canvas, paint, path1, 0f);
//            paint.setStyle(Paint.Style.FILL);//填充模式
//            paint.setColor(Color.BLUE);//画笔颜色
//            drawsth(canvas,paint,path2,220f);
//            paint.setColor(Color.MAGENTA);
//            paint.setStyle(Paint.Style.STROKE);//描边模式
//            drawTextOnCurve(canvas,paint,path3);
    }

    //绘制曲线和曲线上的文字
    private void drawTextOnCurve(Canvas canvas, Paint paint, Path path) {
        path.moveTo(440f, 10f);
        path.lineTo(460f, 80f);
        path.lineTo(490f, 150f);
        path.lineTo(550f, 250f);
        path.lineTo(700f, 400f);
        canvas.drawPath(path, paint);
        canvas.drawTextOnPath("曲线上的文字", path, 10f, 10f, paint);
    }

    //绘制基本图形、Path不规则图形和文字
    private void drawsth(Canvas canvas, Paint paint, Path path, float offset) {
        int canvas_height = canvas.getHeight();
        int canvas_width = canvas.getWidth();
        float x_min = canvas_width/7;
        float x_max = canvas_width/10*9;
        float y_min = canvas_height/7;
        float y_max = x_max;
        float tick_len = canvas_width/75;

        //坐标轴绘制

        canvas.drawLine(x_min, y_max,x_max, y_max, paint);
        //xticks
        canvas.drawLine(x_max, y_max, x_max, y_max-tick_len, paint);
        canvas.drawLine(x_max/4*3+x_min/4, y_max, x_max/4*3+x_min/4, y_max-tick_len, paint);
        canvas.drawLine(x_max/2+x_min/2, y_max, x_max/2+x_min/2, y_max-tick_len, paint);
        canvas.drawLine(x_max/4+x_min/4*3, y_max, x_max/4+x_min/4*3, y_max-tick_len, paint);
        paint.setTextSize(canvas_width/20);
        canvas.drawText("Time (s)", x_max-60, y_max+120, paint);
        paint.setTextSize(40f);
        canvas.drawText("0", x_min-tick_len, y_max+50, paint);
        canvas.drawText("0.5", x_max/4+x_min/4*3, y_max+50, paint);
        canvas.drawText("1", x_max/2+x_min/2, y_max+50, paint);
        canvas.drawText("1.5", x_max/4*3+x_min/4, y_max+50, paint);
        canvas.drawText("2", x_max, y_max+50, paint);

        canvas.drawLine(x_min, y_min, x_min, y_max, paint);
        canvas.drawLine(x_min, y_min, x_min+tick_len, y_min, paint);
        canvas.drawLine(x_min, y_min/4*3 + y_max/4, x_min+tick_len, y_min/4*3 + y_max/4, paint);
        canvas.drawLine(x_min, y_min/2 + y_max/2, x_min+tick_len, y_min/2 + y_max/2, paint);
        canvas.drawLine(x_min, y_min/4 + y_max/4*3, x_min+tick_len, y_min/4 + y_max/4*3, paint);

        paint.setTextSize(canvas_width/20);
        canvas.drawText("Bandwidth", x_min+canvas_width/20, y_min-canvas_width/10, paint);
        canvas.drawText("(Mbps)", x_min+canvas_width/20, y_min-canvas_width/20, paint);
        paint.setTextSize(40f);
        canvas.drawText("0", x_min-tick_len, y_max+50, paint);
        canvas.drawText("0.5", x_max/4+x_min/4*3, y_max+50, paint);
        canvas.drawText("1", x_max/2+x_min/2, y_max+50, paint);
        canvas.drawText("1.5", x_max/4*3+x_min/4, y_max+50, paint);
        canvas.drawText("2", x_max, y_max+50, paint);

        double max_speed_sample;
        if(this.speedSample.size() == 0){
            max_speed_sample =  0.0;
        }
        else{
            max_speed_sample = Collections.max(this.speedSample);
        }

        int my_speed_max;
        if(max_speed_sample < 4) my_speed_max = 4;
        else if(max_speed_sample < 8) my_speed_max = 8;
        else if(max_speed_sample < 20) my_speed_max = 20;
        else if(max_speed_sample < 40) my_speed_max = 40;
        else if(max_speed_sample < 60) my_speed_max = 60;
        else if(max_speed_sample < 80) my_speed_max = 80;
        else{my_speed_max = (int) (max_speed_sample/100)*100 + 100;}
        canvas.drawText( Integer.toString(my_speed_max), x_min-50, y_min+20, paint);
        canvas.drawText( Integer.toString(my_speed_max/4), x_min-50, y_max/4*3 + y_min/4 + 20, paint);
        canvas.drawText( Integer.toString(my_speed_max/2), x_min-50, y_max/2 + y_min/2+20, paint);
        canvas.drawText( Integer.toString(my_speed_max/4*3), x_min-50, y_min/4*3 + y_max/4+20, paint);

        //绘制速度折线
        float old_x = x_min, old_y = y_max;
        for (int i = 0; i < speedSample.size(); i++) {
            double bw_sample = speedSample.get(i);
            double x = x_min + (x_max-x_min) * (i+1) / sample_num; //如果interval变了，这个也得变
            double y = y_max - (y_max-y_min)*(bw_sample/(float) my_speed_max);
            canvas.drawLine(old_x, old_y, (float) x, (float) y, paint);

            old_x = (float) x;
            old_y = (float) y;
        }

        //绘制折线框
        if(this.speedSample.size() > this.window_size){
            int rect_left = this.speedSample.size() - 10;
            int rect_right = this.speedSample.size();
            double rect_midheight = 0;
            double rect_max = 0;
            double stable_bias = 15;
            for (int i = this.speedSample.size()-1; i>=0; i--){
                double tmp = this.speedSample.get(i);
                if (rect_max < tmp){
                    rect_max = tmp;
                }
            }
            double tmp_max = 0;
            double tmp_min = this.speedSample.get(this.speedSample.size()-1);
            int valid_count=0;
            for (int i = this.speedSample.size()-1; i >= this.speedSample.size()-10; i--) {
                double tmp = this.speedSample.get(i);
                if(tmp>0){
                    rect_midheight += tmp;
                    valid_count++;
                }

                if(tmp_max < tmp){
                    tmp_max = tmp;
                }
                if(tmp_min > tmp){
                    tmp_min = tmp;
                }
            }
            rect_midheight /= valid_count;
            paint.setColor(Color.BLUE);
            paint.setStyle(Paint.Style.STROKE);//描边模式
            canvas.drawRect(x_min + (x_max-x_min) * (rect_left+1) / sample_num,
            (float) (y_max - (y_max-y_min)*(rect_midheight*0.93/(float) my_speed_max)),
                    x_min + (x_max-x_min) * (rect_right+1) / sample_num,
                    (float) (y_max - (y_max-y_min)*(rect_midheight*1.07/(float) my_speed_max)),
                    paint);
//            if(this.speedSample.size() <= this.sample_num - 10){
//
//            }else{
//                float top=(float) (y_max - (y_max-y_min)*(my_speed_max-tmp_min)/(float) my_speed_max);
//                Log.d(TAG, String.format("top: %f, y_max: %f",top,y_max));
//                canvas.drawRect(x_min + (x_max-x_min) * (rect_left+1) / sample_num,
//                        (float) (y_max - (y_max-y_min)*((my_speed_max-tmp_min)/(float) my_speed_max)),
//                        x_min + (x_max-x_min) * (rect_right+1) / sample_num,
//                        (float) (y_max - (y_max-y_min)*((my_speed_max-tmp_max)/(float) my_speed_max)),
//                        paint);
//            }


            paint.setStyle(Paint.Style.FILL);//描边模
            paint.setTextSize(40f);
            float mx = (float) (x_min + (x_max-x_min) * ((rect_right+1+stable_bias) / sample_num));
            float my = (float) (y_max - (y_max-y_min)*(rect_midheight*0.96/(float) my_speed_max)) -110;
            if (mx > 840){
                mx = 840;
                my = (float) (y_max - (y_max-y_min)*(rect_max/(float) my_speed_max)-30);
            }
            canvas.drawText("Stable Window", mx,
                    my, paint);
        }
//        Log.d("samples num", Integer.toString(this.speedSample.size()));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= speedSample.size() - 1; i++) {
            double num = speedSample.get(i);
            sb.append(num);
            sb.append(",");

        }
        sb.append(";");
        String result = sb.toString();
//        Log.d("samples", result);
    }
}
