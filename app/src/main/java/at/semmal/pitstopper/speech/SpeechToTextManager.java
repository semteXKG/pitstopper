package at.semmal.pitstopper.speech;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Push-to-talk speech recognition using Android's built-in SpeechRecognizer.
 * Must be created and used on the main thread.
 */
public class SpeechToTextManager {

    public interface ResultListener {
        void onResult(String text);
        void onError(int errorCode);
    }

    private static final String TAG = "SpeechToText";

    private final Context context;
    private SpeechRecognizer recognizer;
    private ResultListener listener;
    private boolean listening = false;

    public SpeechToTextManager(Context context) {
        this.context = context;
    }

    public void setListener(ResultListener listener) {
        this.listener = listener;
    }

    /** Call on main thread when TALK button is pressed. */
    public void startListening() {
        Log.d(TAG, "startListening called");
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "Speech recognition not available on this device");
            return;
        }
        destroyRecognizer();
        recognizer = SpeechRecognizer.createSpeechRecognizer(context);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { Log.d(TAG, "Ready"); }
            @Override public void onBeginningOfSpeech() { Log.d(TAG, "Speaking"); }
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() { Log.d(TAG, "End of speech"); }

            @Override
            public void onResults(Bundle results) {
                listening = false;
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                Log.d(TAG, "onResults: matches=" + matches);
                if (matches != null && !matches.isEmpty()) {
                    String text = matches.get(0).trim();
                    Log.d(TAG, "Best result: \"" + text + "\"");
                    if (!text.isEmpty() && listener != null) {
                        listener.onResult(text);
                    } else {
                        Log.w(TAG, "Result was empty after trim");
                        if (listener != null) listener.onError(-1);
                    }
                } else {
                    Log.w(TAG, "No matches returned");
                    if (listener != null) listener.onError(-1);
                }
            }

            @Override
            public void onError(int error) {
                listening = false;
                String reason = errorName(error);
                Log.w(TAG, "Recognition error " + error + " (" + reason + ")");
                if (listener != null) {
                    listener.onError(error);
                }
            }

            @Override public void onPartialResults(Bundle partial) {}
            @Override public void onEvent(int type, Bundle extras) {}
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        // Prefer cloud recognition for better noise robustness
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false);
        // Allow longer silences between words in noisy environments
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000);

        listening = true;
        Log.d(TAG, "recognizer.startListening() fired");
        recognizer.startListening(intent);
    }

    /** Call on main thread when TALK button is released — triggers onResults. */
    public void stopListening() {
        Log.d(TAG, "stopListening called, listening=" + listening);
        if (recognizer != null && listening) {
            recognizer.stopListening();
        }
    }

    public void destroy() {
        destroyRecognizer();
    }

    private void destroyRecognizer() {
        if (recognizer != null) {
            recognizer.cancel();
            recognizer.destroy();
            recognizer = null;
        }
        listening = false;
    }

    private static String errorName(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:                    return "ERROR_AUDIO";
            case SpeechRecognizer.ERROR_CLIENT:                   return "ERROR_CLIENT";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return "ERROR_INSUFFICIENT_PERMISSIONS";
            case SpeechRecognizer.ERROR_NETWORK:                  return "ERROR_NETWORK";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:          return "ERROR_NETWORK_TIMEOUT";
            case SpeechRecognizer.ERROR_NO_MATCH:                 return "ERROR_NO_MATCH";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:          return "ERROR_RECOGNIZER_BUSY";
            case SpeechRecognizer.ERROR_SERVER:                   return "ERROR_SERVER";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:           return "ERROR_SPEECH_TIMEOUT";
            case 10:                                              return "ERROR_TOO_MANY_REQUESTS";
            case 11:                                              return "ERROR_SERVER_DISCONNECTED";
            case 12:                                              return "ERROR_LANGUAGE_NOT_SUPPORTED";
            case 13:                                              return "ERROR_LANGUAGE_UNAVAILABLE";
            default:                                              return "UNKNOWN(" + error + ")";
        }
    }
}
