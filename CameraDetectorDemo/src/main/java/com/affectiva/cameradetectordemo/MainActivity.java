package com.affectiva.cameradetectordemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Vibrator;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.affectiva.android.affdex.sdk.Frame;
import com.affectiva.android.affdex.sdk.detector.CameraDetector;
import com.affectiva.android.affdex.sdk.detector.Detector;
import com.affectiva.android.affdex.sdk.detector.Face;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * This is a very bare sample app to demonstrate the usage of the CameraDetector object from Affectiva.
 * It displays statistics on frames per second, percenLOG_TAGe of time a face was detected, and the user's smile score.
 * <p>
 * The app shows off the maneuverability of the SDK by allowing the user to start and stop the SDK and also hide the camera SurfaceView.
 * <p>
 * For use with SDK 2.02
 */
public class MainActivity extends Activity implements Detector.ImageListener, CameraDetector.CameraEventListener, RecognitionListener {
    private final String LOG_TAG = "CameraDetectorDemo";
    private final static String[] EMOTIONS = {"Anger", "Fear", "Sadness", "Joy"};
    private static HashMap<String, Float> emotionsRecorded = new HashMap<>();

    EditText txtSpeechInput;
    TextToSpeech tts;
    ImageButton startVoiceRecordedButton;
    ImageButton switchViewButton;
    ImageButton textToSpeechButton;
    ImageButton textClearButton;
    ImageView emoticonImage;

    SurfaceView cameraPreview;

    boolean isCameraBack = false;
    boolean isVoiceRecorded = false;

    RelativeLayout mainLayout;

    CameraDetector detector;
    SpeechRecognizer speech;
    Vibrator vibrator;

    int previewWidth = 0;
    int previewHeight = 0;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mainLayout = (RelativeLayout) findViewById(R.id.main_layout);

        txtSpeechInput = (EditText) findViewById(R.id.txt_speech_input);
        textClearButton = (ImageButton) findViewById(R.id.txt_clear_imageButton);
        switchViewButton = (ImageButton) findViewById(R.id.front_back_imageButton);
        startVoiceRecordedButton = (ImageButton) findViewById(R.id.voice_start_imageButton);
        textToSpeechButton = (ImageButton) findViewById(R.id.text_to_speech_imageButton);
        emoticonImage = (ImageView) findViewById(R.id.emoticon_imageView);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        textClearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtSpeechInput.setText(null);
                emoticonImage.setImageDrawable(null);
                emoticonImage.setBackground(null);
            }
        });

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    tts.setLanguage(Locale.getDefault());
                }
            }
        });

        textToSpeechButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speech();
            }
        });

        switchViewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isCameraBack = !isCameraBack;
                switchCamera(isCameraBack ? CameraDetector.CameraType.CAMERA_BACK : CameraDetector.CameraType.CAMERA_FRONT);
            }
        });

        startVoiceRecordedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isVoiceRecorded = true;

                startVoiceRecordedButton.setEnabled(false);
                startVoiceRecordedButton.setImageResource(R.drawable.voice_recorder_disabled_icon);

                txtSpeechInput.setText(null);

                promptSpeechInput();
            }
        });
        startVoiceRecordedButton.setEnabled(false);

        //We create a custom SurfaceView that resizes itself to match the aspect ratio of the incoming camera frames
        cameraPreview = new SurfaceView(this) {
            @Override
            public void onMeasure(int widthSpec, int heightSpec) {
                int measureWidth = MeasureSpec.getSize(widthSpec);
                int measureHeight = MeasureSpec.getSize(heightSpec);
                int width;
                int height;
                if (previewHeight == 0 || previewWidth == 0) {
                    width = measureWidth;
                    height = measureHeight;
                } else {
                    float viewAspectRatio = (float) measureWidth / measureHeight;
                    float cameraPreviewAspectRatio = (float) previewWidth / previewHeight;

                    if (cameraPreviewAspectRatio > viewAspectRatio) {
                        width = measureWidth;
                        height = (int) (measureWidth / cameraPreviewAspectRatio);
                    } else {
                        width = (int) (measureHeight * cameraPreviewAspectRatio);
                        height = measureHeight;
                    }
                }
                setMeasuredDimension(width + 200, height + 200);
            }
        };
        RelativeLayout.LayoutParams params =
                new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        cameraPreview.setLayoutParams(params);
        mainLayout.addView(cameraPreview, 0);

        detector = new CameraDetector(this,
                CameraDetector.CameraType.CAMERA_FRONT,
                cameraPreview);
        setEmotionsDetectors(detector);
        detector.setImageListener(this);
        detector.setOnCameraEventListener(this);
        startDetector();
    }

    void startDetector() {
        if (!detector.isRunning()) {
            detector.start();
        }
    }

    void switchCamera(CameraDetector.CameraType type) {
        detector.setCameraType(type);
    }

    @Override
    public void onImageResults(List<Face> list, Frame frame, float v) {
        if (list == null)
            return;
        if (list.size() == 0) {
            if (!isVoiceRecorded) {
                startVoiceRecordedButton.setEnabled(false);
                startVoiceRecordedButton.setImageResource(R.drawable.voice_recorder_disabled_icon);
            }

        } else {
            if (isVoiceRecorded) {
                recordEmotions(list.get(0).emotions);
            } else {
                startVoiceRecordedButton.setEnabled(true);
                startVoiceRecordedButton.setImageResource(R.drawable.voice_recorder_icon);
            }
        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    public void onCameraSizeSelected(int width, int height, Frame.ROTATE rotate) {
        if (rotate == Frame.ROTATE.BY_90_CCW || rotate == Frame.ROTATE.BY_90_CW) {
            previewWidth = height;
            previewHeight = width;
        } else {
            previewHeight = height;
            previewWidth = width;
        }
        cameraPreview.requestLayout();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void onReadyForSpeech(Bundle params) {
        startVoiceRecordedButton.setImageResource(R.drawable.voice_recording_icon);
    }

    @Override
    public void onBeginningOfSpeech() {
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        Log.d(LOG_TAG, "onRmsChanged");
    }

    public void onBufferReceived(byte[] buffer) {
        Log.d(LOG_TAG, "onBufferReceived");
    }

    @Override
    public void onEndOfSpeech() {
        Log.d(LOG_TAG, "onEndofSpeech");
    }

    @Override
    public void onError(int error) {
        txtSpeechInput.setHint(R.string.speech_error);
        stopRecordingVoice();
    }

    @Override
    public void onResults(Bundle results) {
        speechToText(results);
        stopRecordingVoice();
    }

    public void onPartialResults(Bundle partialResults) {
        Log.d(LOG_TAG, "onPartialResults");
    }

    public void onEvent(int eventType, Bundle params) {
        Log.d(LOG_TAG, "onEvent " + eventType);
    }

    private void recordEmotions(Face.Emotions emotions) {
        try {
            Class detectorClass =
                    Class.forName("com.affectiva.android.affdex.sdk.detector.Face$Emotions");
            for (String emotion : EMOTIONS) {
                Object emotionDegree =
                        detectorClass.getMethod("get" + emotion).invoke(emotions);

                if (emotionsRecorded.containsKey(emotion)) {
                    emotionsRecorded.put(
                            emotion,
                            (emotionsRecorded.get(emotion) + (Float) emotionDegree) / 2
                    );
                } else {
                    emotionsRecorded.put(emotion, (Float) emotionDegree);
                }
            }
        } catch (ClassNotFoundException
                | NoSuchMethodException
                | IllegalAccessException
                | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void setEmotionsDetectors(CameraDetector detector) {
        try {
            for (String emotion : EMOTIONS) {
                detector.getClass().getSuperclass()
                        .getMethod("setDetect" + emotion, Boolean.TYPE)
                        .invoke(detector, true);
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        speech = SpeechRecognizer.createSpeechRecognizer(this);
        speech.setRecognitionListener(this);
        speech.startListening(intent);
    }

    private int getEmotionColor(String emotion) {
        switch (emotion) {
            case "Joy":
                return R.color.Joy;
            case "Anger":
                return R.color.Anger;
            case "Fear":
                return R.color.Fear;
            case "Sadness":
                return R.color.Sadness;
            default:
                return R.color.Neutral;
        }
    }

    private int getEmotionEmoticon(String emotion) {
        switch (emotion) {
            case "Joy":
                return R.drawable.joy;
            case "Anger":
                return R.drawable.anger;
            case "Fear":
                return R.drawable.fear;
            case "Sadness":
                return R.drawable.sadness;
            default:
                return R.drawable.neutral;
        }
    }

    private String getEmotionFromDegree(Float emotionDegree) {
        for (String emo : emotionsRecorded.keySet()) {
            if (emotionsRecorded.get(emo).equals(emotionDegree)) {
                return emo;
            }
        }
        return null;
    }

    private void stopRecordingVoice() {
        isVoiceRecorded = false;
        startVoiceRecordedButton.setEnabled(true);
        startVoiceRecordedButton.setImageResource(R.drawable.voice_recorder_icon);
        emotionsRecorded.clear();
    }

    private void speechToText(Bundle results) {
        ArrayList<String> textSpeech =
                results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        txtSpeechInput.setText(String.valueOf(textSpeech.get(0)));

        if (emotionsRecorded.size() > 0) {
            String emotion = getEmotionFromDegree(Collections.max(emotionsRecorded.values()));
            int emotionColor =getResources().getColor(getEmotionColor(emotion));
            Drawable emoImg = getResources().getDrawable(getEmotionEmoticon(emotion));
            emoticonImage.setBackgroundColor(emotionColor);
            emoticonImage.setImageDrawable(emoImg);

            txtSpeechInput.setTextColor(emotionColor);
        }
    }

    private void speech() {
        String text = txtSpeechInput.getText().toString();
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        // Morse m = new Morse(text, vibrator, 100);
        // m.morseToImpulses();
    }
}
