package framework.accessibilityframework.view;

import android.Manifest;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.annotation.Nullable;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;

import framework.accessibilityframework.control.LocaleUtils;
import framework.accessibilityframework.control.PermissionUtils;
import framework.accessibilityframework.R;

import static android.app.Activity.RESULT_OK;


public class MySpeechRecognizerFragment extends Fragment {

    public interface SpeechRecognitionListener{
        public void onSpeechRecognized(ArrayList<String> recognizedSentences);
    }

    SpeechRecognitionListener speechRecognitionListener;

    private String language = LocaleUtils.PORTUGUESE_BRASIL; //recognized language
    private static final int SPEECH_REQUEST_CODE = 1;
    private static final String PERMISSION_RECORD_AUDIO = Manifest.permission.RECORD_AUDIO;
    private boolean allPermissionsGranted = false;
    private ArrayList<String> recognizedSentences = new ArrayList<>(); //the text sentences recognized by the speech recognizer

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try{
            speechRecognitionListener = (SpeechRecognitionListener) context;
        }catch(ClassCastException e){
            throw new ClassCastException(context.toString() + " must implement SpeechRecognitionListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_speech_recognizer, container, false);

        try {
            //handle necessary permissions for devices with Android 6.0 or above
            String[] record_perm = {PERMISSION_RECORD_AUDIO};
            if (Build.VERSION.SDK_INT >= 16 && ! allPermissionsGranted){ //has Android 6.0, but no permission was granted for recording audio

                if (PermissionUtils.checkPermission(getActivity(),PERMISSION_RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED){
                    PermissionUtils.requestPermission(getActivity(),record_perm, SPEECH_REQUEST_CODE);
                }

                if (PermissionUtils.checkPermission(getActivity(), PERMISSION_RECORD_AUDIO)== PackageManager.PERMISSION_GRANTED){
                    allPermissionsGranted = true;
                }
            }

            recognizeSentence();
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getActivity(),
                    "Opps! Your device doesn’t support Speech to Text", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        return view;
    }

    public void recognizeSentence(){
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, language);

        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case SPEECH_REQUEST_CODE: {
                if (resultCode == RESULT_OK && null != data) {
                    //the sentences that were recognized by the speech recognizer are saved in the following arraylist,
                    //in descending order of accuracy according to the recording algorithm, ie, the sentence with the closest
                    //meaning will appear in position 0, the second sentence with the closest meaning will appear in position 1, and so on.
                    //For instance, if the user says "I'm going to live", the recognizer will probably return the sentence
                    //"I'm going to live" as the String in position 0 of the Arraylist, and will alson return the sentence
                    //"I'm going to leave" as the String in position 1 of the Arraylist

                    recognizedSentences = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    speechRecognitionListener.onSpeechRecognized(recognizedSentences);
                }
                break;
            }
        }
    }
}
