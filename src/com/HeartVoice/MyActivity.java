package com.HeartVoice;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import com.iflytek.speech.*;
import com.umeng.analytics.MobclickAgent;

import java.util.ArrayList;

public class MyActivity extends Activity {
    private static final int EXPRESSIONS = 1;
    private static final int SETTINGS = 2;
    private MainView mainView;
    private AudioManager audio;
    private Context context;

    private SpeechSynthesizer mTts;
    private SpeechRecognizer mIat;

//  外部设置的弹出框完成按钮文字
    public static final String TITLE_DONE = "title_done";
//  外部设置的弹出框取消按钮文字
    public static final String TITLE_CANCEL = "title_cancel";
    private static final int REQUEST_CODE_SEARCH = 1099;

//  Called when the activity is first created.
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mainView = new MainView(this);
        setContentView(mainView);
        context = mainView.getContext();

        checkEngines(mainView);
        findViewById(R.id.read_button).setEnabled(false);
        mTts = new SpeechSynthesizer(mainView.getContext(), mTtsInitListener);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case (EXPRESSIONS):
                if (resultCode == Activity.RESULT_OK) {
                    String newText = data.getStringExtra(UsefulExpressionListActivity.EXPRESSION_VALUE);
                    mainView.editText.setText(newText);
                }
                break;
            case (REQUEST_CODE_SEARCH) :
                if (resultCode == Activity.RESULT_OK) {
                    ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    String res = results.get(0);
                    EditText editor = ((EditText)findViewById(R.id.word));
                    editor.setText(res);
                }
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                return true;
            default:
                return false;
        }
    }

    public void clear(View view){
      mainView.clear();
    }

    public void goToExpressionList(View view){
        Intent intent = new Intent(this.getApplicationContext(), UsefulExpressionListActivity.class);
        startActivityForResult(intent, EXPRESSIONS);

    }

    public void goToSettings(View view){
        Intent intent = new Intent(this.getApplicationContext(), SettingsActivity.class);
        startActivityForResult(intent, SETTINGS);
    }

    public void goToReading(View view) {
        if (!checkEngines(mainView))
            return;
// 设置参数
//		mTts.setParameter(SpeechConstant.ENGINE_TYPE, "cloud");
//		mTts.setParameter(SpeechSynthesizer.VOICE_NAME, "vixk");
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String voice = sp.getString(SettingsActivity.KEY_DEFAULT_VOICE, "xiaoyan");
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, "local");
        mTts.setParameter(SpeechSynthesizer.VOICE_NAME, voice);
        mTts.setParameter(SpeechSynthesizer.SPEED, "50");
        mTts.setParameter(SpeechSynthesizer.PITCH, "50");
        int code = mTts.startSpeaking(mainView.editText.getText().toString(), mTtsListener);
    }

    public void goToRecognizer(View view) {
        if (!checkEngines(mainView))
            return;
        Intent intent = new Intent();
        // 指定action名字
        intent.setAction("com.iflytek.speech.action.voiceinput");
        intent.putExtra(SpeechConstant.PARAMS, "asr_ptt=1");
        intent.putExtra(SpeechConstant.VAD_EOS, "1000");
        // 设置弹出框的两个按钮名称
        intent.putExtra(TITLE_DONE, "确定");
        intent.putExtra(TITLE_CANCEL, "取消");
        startActivityForResult(intent, REQUEST_CODE_SEARCH);
    }

    private boolean checkEngines(MainView mainView) {
        // 没有可用的引擎
        if (SpeechUtility.getUtility(this).queryAvailableEngines() == null
                || SpeechUtility.getUtility(this).queryAvailableEngines().length <= 0) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setMessage(getString(R.string.download_confirm_msg));
            dialog.setNegativeButton(R.string.dialog_cancel_button, null);
            dialog.setPositiveButton(getString(R.string.dialog_confirm_button),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialoginterface, int i) {
                            String url = SpeechUtility.getUtility(MyActivity.this).getComponentUrl();
                            String assetsApk="SpeechService.apk";
                            processInstall(MyActivity.this,url,assetsApk);
                        }
                    });
            dialog.show();
            return false;
        }
        // 设置你申请的应用appid
        SpeechUtility.getUtility(MyActivity.this).setAppid("51ce9dca");
        return true;
    }

    private void processInstall(Context context ,String url,String assetsApk){
        // 直接下载方式
        ApkInstaller.openDownloadWeb(context, url);
        // 本地安装方式
//		if(!ApkInstaller.installFromAssets(context, assetsApk)){
//		    Toast.makeText(MainActivity.this, "安装失败", Toast.LENGTH_SHORT).show();
//		}
    }

//  初期化监听。
    private InitListener mTtsInitListener = new InitListener() {

        @Override
        public void onInit(ISpeechModule arg0, int code) {
            if (code == ErrorCode.SUCCESS) {
                findViewById(R.id.read_button).setEnabled(true);
            }
        }
    };

//  合成回调监听。
    private SynthesizerListener mTtsListener = new SynthesizerListener.Stub() {
        @Override
        public void onBufferProgress(int progress) throws RemoteException {
        }

        @Override
        public void onCompleted(int code) throws RemoteException {
        }

        @Override
        public void onSpeakBegin() throws RemoteException {
        }

        @Override
        public void onSpeakPaused() throws RemoteException {
        }

        @Override
        public void onSpeakProgress(int progress) throws RemoteException {
        }

        @Override
        public void onSpeakResumed() throws RemoteException {
        }
    };
}
