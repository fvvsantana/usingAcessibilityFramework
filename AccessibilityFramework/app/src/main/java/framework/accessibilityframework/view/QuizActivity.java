package framework.accessibilityframework.view;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import framework.accessibilityframework.R;

public class QuizActivity extends Activity implements MySpeechRecognizerFragment.SpeechRecognitionListener {
    private String[] quizPhrases;
    private TextView phraseTextView;
    private int currentPhraseIndex = 0;
    Resources res;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        //get the phrase textView
        phraseTextView = findViewById(R.id.phrase_text);
        //get the string-array phrases
        res = getResources();
        quizPhrases = res.getStringArray(R.array.phrases);

        //
        phraseTextView.setText(quizPhrases[currentPhraseIndex]);

    }


    @Override
    public void onSpeechRecognized(ArrayList<String> recognizedSentences) {


        //check if the user spoke the sentence right
        boolean userHit = false;
        for(String sentence : recognizedSentences){
            if(sentence.equals(quizPhrases[currentPhraseIndex])){
                userHit = true;
            }
        }

        MySpeechRecognizerFragment speechRecognizerFragment = (MySpeechRecognizerFragment) getFragmentManager().findFragmentById(R.id.speech_recognizer_fragment);

        //if the user hit
        if(userHit){
            //if still there are phrases to speak
            if(currentPhraseIndex < quizPhrases.length - 1){
                //set next phrase on textView
                currentPhraseIndex++;
                phraseTextView.setText(quizPhrases[currentPhraseIndex]);

                //advise the hit to the user
                Toast.makeText(this, R.string.hit_advise, Toast.LENGTH_SHORT).show();

                //refresh speech recognizer fragment
                speechRecognizerFragment.recognizeSentence();

            }else{
                //set up an alertDialog builder
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
                alertBuilder.setTitle(R.string.congratulations_title);
                alertBuilder.setMessage(R.string.final_congratulation);
                alertBuilder.setNegativeButton(R.string.exit_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onExitCommand();
                    }
                });
                alertBuilder.setPositiveButton(R.string.restart_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onRestartCommand();
                    }
                });

                //create and show the alert dialog
                AlertDialog alert = alertBuilder.create();
                alert.show();
            }

        }else{
            //advise the miss to the user
            Toast.makeText(this, res.getString(R.string.miss_advise), Toast.LENGTH_SHORT).show();

            //refresh speech recognizer fragment
            speechRecognizerFragment.recognizeSentence();
        }

    }

    public void onTryAgainCommand(View view){
        MySpeechRecognizerFragment speechRecognizerFragment = (MySpeechRecognizerFragment) getFragmentManager().findFragmentById(R.id.speech_recognizer_fragment);
        speechRecognizerFragment.recognizeSentence();

    }

    public void onRestartCommand() {
        finish();
    }

    public void onExitCommand() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory( Intent.CATEGORY_HOME );
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(homeIntent);
        finish();
    }

}
