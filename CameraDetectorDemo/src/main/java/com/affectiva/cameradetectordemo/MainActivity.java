package com.affectiva.cameradetectordemo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

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
    private final static String[] EMOTIONS = {"Anger", "Fear", "Disgust", "Contempt", "Sadness", "Surprise", "Joy"};
    private static HashMap<String, Float> emotionsRecorded = new HashMap<>();

    /**
     * MIC
     **/
    TextView txtSpeechInput;
    /**
     * EMOTIONS
     **/
    Button startVoiceRecordedButton;
    TextView emotionsTextView;
    ToggleButton toggleButton;
    SurfaceView cameraPreview;

    boolean isCameraBack = false;
    boolean isVoiceRecorded = false;

    RelativeLayout mainLayout;

    CameraDetector detector;
    SpeechRecognizer speech;

    int previewWidth = 0;
    int previewHeight = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtSpeechInput = (TextView) findViewById(R.id.txtSpeechInput);

        emotionsTextView = (TextView) findViewById(R.id.emotions_textview);
        toggleButton = (ToggleButton) findViewById(R.id.front_back_toggle_button);

        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isCameraBack = isChecked;
                switchCamera(isCameraBack ? CameraDetector.CameraType.CAMERA_BACK : CameraDetector.CameraType.CAMERA_FRONT);
            }
        });

        startVoiceRecordedButton = (Button) findViewById(R.id.voice_start_button);
        startVoiceRecordedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isVoiceRecorded) {
                    startDetector();
                    promptSpeechInput();

                    startVoiceRecordedButton.setText("is recording...");
                    startVoiceRecordedButton.setEnabled(false);

                    txtSpeechInput.setText(null);
                    txtSpeechInput.setBackgroundColor(Color.TRANSPARENT);
                }
            }
        });
        startVoiceRecordedButton.setText("Start record");

        //We create a custom SurfaceView that resizes itself to match the aspect ratio of the incoming camera frames
        mainLayout = (RelativeLayout) findViewById(R.id.main_layout);
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
                setMeasuredDimension(width, height);
            }
        };
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        cameraPreview.setLayoutParams(params);
        mainLayout.addView(cameraPreview, 0);

        detector = new CameraDetector(this, CameraDetector.CameraType.CAMERA_FRONT, cameraPreview);
        setEmotionsDetectors(detector);
        detector.setImageListener(this);
        detector.setOnCameraEventListener(this);
    }

    void startDetector() {
        if (!detector.isRunning()) {
            detector.start();
        }
    }

    void stopDetector() {
        if (detector.isRunning()) {
            detector.stop();
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
            emotionsTextView.setText("NO FACE");
        } else {
            Face face = list.get(0);
            emotionsTextView.setText(getEmotionsExpressions(face.emotions));
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
        Log.d(LOG_TAG, "onReadyForSpeech");
    }

    public void onBeginningOfSpeech() {
        isVoiceRecorded = true;
    }

    public void onRmsChanged(float rmsdB) {
        Log.d(LOG_TAG, "onRmsChanged");
    }

    public void onBufferReceived(byte[] buffer) {
        Log.d(LOG_TAG, "onBufferReceived");
    }

    public void onEndOfSpeech() {
        Log.d(LOG_TAG, "onEndofSpeech");
    }

    public void onError(int error) {
        Log.d(LOG_TAG, "error " + error);
        txtSpeechInput.setText(R.string.speech_error);
    }

    public void onResults(Bundle results) {
        stopDetector();
        isVoiceRecorded = false;

        startVoiceRecordedButton.setText("Start record");
        startVoiceRecordedButton.setEnabled(true);

        ArrayList<String> textSpeech = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        txtSpeechInput.setText(String.valueOf(textSpeech.get(0)));

        if (emotionsRecorded.size() > 0) {
            txtSpeechInput.setBackgroundColor(
                    getEmotionColor(getEmotionFromDegree(Collections.max(emotionsRecorded.values()))
                    ));

            emotionsRecorded.clear();
        }
    }

    public void onPartialResults(Bundle partialResults) {
        Log.d(LOG_TAG, "onPartialResults");
    }

    public void onEvent(int eventType, Bundle params) {
        Log.d(LOG_TAG, "onEvent " + eventType);
    }

    private String getEmotionsExpressions(Face.Emotions emotions) {
        StringBuilder faceEmotions = new StringBuilder();

        try {
            Class detectorClass = Class.forName("com.affectiva.android.affdex.sdk.detector.Face$Emotions");
            for (String emotion : EMOTIONS) {
                Object emotionDegree = detectorClass.getMethod("get" + emotion)
                        .invoke(emotions);
                faceEmotions.append(
                        String.format(emotion + ": %.2f",
                                emotionDegree
                        ) + "\n");

                if (isVoiceRecorded) {
                    saveEmotion((Float) emotionDegree, emotion);
                }
            }
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return faceEmotions.toString();
    }

    private void saveEmotion(Float emotionDegree, String emotion) {
        if (emotionsRecorded.containsKey(emotion)) {
            emotionsRecorded.put(emotion, (emotionsRecorded.get(emotion) + emotionDegree) / 2);
        } else {
            emotionsRecorded.put(emotion, emotionDegree);
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
        Color color = new Color();
        switch (emotion) {
            case "Joy":
                return color.YELLOW;
            case "Anger":
                return color.RED;
        }
        return color.WHITE;
    }

    private String getEmotionFromDegree(Float emotionDegree) {
        for (String emo : emotionsRecorded.keySet()) {
            if (emotionsRecorded.get(emo).equals(emotionDegree)) {
                return emo;
            }
        }
        return null;
    }
}
