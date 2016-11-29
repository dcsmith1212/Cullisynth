package derek.syntheremin;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import org.w3c.dom.Text;


public class MainActivity extends AppCompatActivity {
    final int minFreq = 100;
    final int maxFreq = 1500;
    final double minAmp = 0.0;
    final double maxAmp = 1.0;

    // Run on app startup
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    // Run when main screen is displayed
    @Override
    public void onResume() {
        super.onResume();

        // Used to display coordinates of last touch in middle of screen
        final TextView coords = (TextView)findViewById(R.id.textView2);

        // Defines touch-able region (the whole screen)
        final View onTouchView = findViewById(R.id.textView1);

        // This listener causes app to wait until screen is fully rendered
        // (otherwise we'd run into issues getting dimensions of touchable area)
        onTouchView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // Retrieves dimensions of touch-able area
                final int maxX = onTouchView.getWidth();
                final int maxY = onTouchView.getHeight();

                // Waits for user to interact with screen (touch, slide, lift)
                onTouchView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        final int action = event.getAction();
                        switch (action & MotionEvent.ACTION_MASK) {
                            // Case when user touches screen
                            case MotionEvent.ACTION_DOWN: {
                                // Captures (x,y) location of touch
                                int x = (int)event.getX();
                                int y = (int)event.getY();

                                // Displays location on screen
                                coords.setText("Last touch: (" + String.valueOf(x) + ", " + String.valueOf(y) + ")");

                                // Scales x and y value to represent amplitude and frequency of sine wave
                                final int freq = (int)(((double)y / (double)maxY) * (maxFreq - minFreq) + minFreq);
                                final double amp =(((double)x / (double)maxX) * (maxAmp - minAmp) + minAmp);

                                // Starts a new thread, creates sample, and plays it
                                // Note: This might not be the best way to do this, because if the user
                                // doesn't lift their finger we want the sound to keep playing. So using
                                // a predefined buffer of sound isn't ideal. We'll need something that changes
                                // length dynamically
                                final Thread thread = new Thread(new Runnable() {
                                    public void run() {
                                        genTone(freq, amp);
                                        handler.post(new Runnable() {

                                            public void run() {
                                                playSound(true);
                                            }
                                        });
                                    }
                                });
                                thread.start();
                                return true;
                            }
                            // Case when user lifts finger from screen
                            // Not currently working. This really only makes sense when we've implemented the
                            // dynamic buffer mentioned above. Otherwise the buffer will just play until it finishes.
                            /*
                            case MotionEvent.ACTION_UP: {
                                Log.d("ACTION UP", "Finger lifted!");
                                final Thread thread = new Thread(new Runnable() {
                                    public void run() {
                                        handler.post(new Runnable() {

                                            public void run() {
                                                playSound(false);
                                            }
                                        });
                                    }
                                });
                                thread.start();
                                break;
                            }
                            */
                            // Case when user moves finger across screen
                            // Also not working. Same issue as above.
                            case MotionEvent.ACTION_MOVE:{
                                // Recaptures (x,y) location of finger
                                int x = (int)event.getX();
                                int y = (int)event.getY();
                                coords.setText(String.valueOf(x) + "," + String.valueOf(y));

                                break;
                            }
                        }
                        return false;
                    }
                });
            }
        });
    }

    // Sound generation
    private final double duration = 0.5; // seconds (this will need to be dynamic instead of fixed)
    private final int sampleRate = 10000;
    private final int numSamples = (int)(duration * sampleRate);
    private final double sample[] = new double[numSamples];

    private final byte generatedSnd[] = new byte[2 * numSamples];

    Handler handler = new Handler();

    // Function to build sounder array
    void genTone(int frequency, double amplitude){
        Log.d("Pressed at frequency: ", String.valueOf(frequency));
        // Fills out the array
        for (int i = 0; i < numSamples; ++i) {
            sample[i] = amplitude*Math.sin(2 * Math.PI * i / (sampleRate/frequency));
        }

        // Converts to 16 bit sound array
        int idx = 0;
        for (final double dVal : sample) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }
    }

    // Function to play sound through an audio track
    // There are alternatives to this that may work better
    void playSound(boolean onTouch){
        final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
                AudioTrack.MODE_STATIC);
        audioTrack.write(generatedSnd, 0, generatedSnd.length);
        if (onTouch) {
            // Will loop sound, but it won't stop
            //audioTrack.setLoopPoints(0, generatedSnd.length / 2, -1);
            audioTrack.play();
        } else {
            audioTrack.stop();
        }
    }
}