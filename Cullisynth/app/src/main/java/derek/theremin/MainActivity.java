package derek.theremin;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.widget.CheckBox;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.lang.reflect.Field;


public class MainActivity extends AppCompatActivity {

    AudioTrack audioTrack;
    int Wavetype = 0;


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.norm:
                Wavetype = 0;
                return true;
            case R.id.abs_wave:
                Wavetype = 1;
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        final double pitches[] = {261.63,293.66,329.63,349.23,392,440,493.88,523.25};

        final View onTouchView = findViewById(R.id.textView1);
        onTouchView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                final int maxX = onTouchView.getWidth();
                final int maxY = onTouchView.getHeight();

                onTouchView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        int action = event.getAction();
                        switch (action & MotionEvent.ACTION_MASK) {
                            case MotionEvent.ACTION_DOWN: {
                                int y = (int)event.getY();
                                int x = (int)event.getX();

                                final double freq = pitches[(int)(java.lang.Math.floor(x/(maxX/8)))];
                                // Use a new tread as this can take a while
                                Log.d("DOWN","DOWN");
                                final Thread thread = new Thread(new Runnable() {
                                    public void run() {
                                        genTone(freq);
                                        handler.post(new Runnable() {

                                            public void run() {
                                                playSound(true, freq);
                                            }
                                        });
                                    }
                                });
                                thread.start();
                                return true;
                            }
                            case MotionEvent.ACTION_UP: {
                                final Thread thread = new Thread(new Runnable() {
                                    public void run() {
                                    audioTrack.stop();
                                    }
                                });
                                thread.start();
                                break;
                            }
                            case MotionEvent.ACTION_MOVE:{
                                int x = (int)event.getX();
                                int y = (int)event.getY();
                                break;
                            }
                        }
                        return false;
                    }
                });
            }
        });
    }


    // originally from http://marblemice.blogspot.com/2010/04/generate-and-play-tone-in-android.html
    // and modified by Steve Pomeroy <steve@staticfree.info>
    private final double duration = 60; // seconds
    private final int sampleRate = 10000;
    private final int numSamples = (int)(duration * sampleRate);
    private final double sample[] = new double[numSamples];

    private final byte generatedSnd[] = new byte[2 * numSamples];

    Handler handler = new Handler();

    void genTone(double frequency){
        Log.d("HEEEEEEEEY!!1!!!", String.valueOf(frequency));
        // fill out the array
        for (int i = 0; i < numSamples; ++i) {
            if(Wavetype == 0)
                sample[i] = Math.sin(2 * Math.PI * i / (sampleRate/frequency));
            if(Wavetype == 1)
                sample[i] = Math.abs(Math.sin(2 * Math.PI * i / (sampleRate/frequency)));
        }

        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        for (final double dVal : sample) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);

        }
    }

    void playSound(boolean onTouch, double freqOfTone){
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
                AudioTrack.MODE_STATIC);
        audioTrack.write(generatedSnd, 0, generatedSnd.length);
        if (onTouch)
            audioTrack.setLoopPoints(0, generatedSnd.length / 2, -1);
        audioTrack.play();
    }
}