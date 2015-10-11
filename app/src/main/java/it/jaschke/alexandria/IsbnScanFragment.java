package it.jaschke.alexandria;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import me.dm7.barcodescanner.zbar.BarcodeFormat;
import me.dm7.barcodescanner.zbar.Result;
import me.dm7.barcodescanner.zbar.ZBarScannerView;

/**
 * Created by NICK on 9/29/2015.
 */
public class IsbnScanFragment extends Fragment implements ZBarScannerView.ResultHandler {

    private static final String TAG = IsbnScanFragment.class.getSimpleName();
    private static final String FLASH_STATE = "FLASH_STATE";
    private static final String AUTO_FOCUS_STATE = "AUTO_FOCUS_STATE";
    private static final String SELECTED_FORMATS = "SELECTED_FORMATS";
    private static final String CAMERA_ID = "CAMERA_ID";

    private String mScanFormat = "Format:";
    private String mScanContents = "Contents:";
    private ArrayList<Integer> mSelectedIndices;
    private ZBarScannerView mScannerView;
    private int mCameraId = -1;
    private boolean mFlash = false;
    private boolean mAutoFocus = true;
    private Callback mCallback;

    @Override
    public void handleResult(Result result) {
        Log.i(TAG, "raw result was #" + result.getContents() + "#");

        //return the isbn as a string in a result bundle
        //Intent output = new Intent();
        //output.putExtra(AddBook.ISBN_RESULT, result.getContents());
        Log.i(TAG, getActivity().getLocalClassName());
        //getActivity().setResult(0, output);
        //getActivity().finish();
        mCallback.recieveIsbn(result.getContents());
    }

    public interface Callback {
        void recieveIsbn(String isbn);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallback = (Callback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement IsbnScanFragment.Callback");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        mScannerView = new ZBarScannerView(getActivity());
        if (state != null) {
            mFlash = state.getBoolean(FLASH_STATE, false);
            mAutoFocus = state.getBoolean(AUTO_FOCUS_STATE, true);
            mSelectedIndices = state.getIntegerArrayList(SELECTED_FORMATS);
            mCameraId = state.getInt(CAMERA_ID, -1);
        } else {
            mFlash = false;
            mAutoFocus = true;
            mSelectedIndices = null;
            mCameraId = -1;
        }

        //just add the isbn13 format
        List<BarcodeFormat> formats = new ArrayList<>();
        formats.add(BarcodeFormat.ISBN13);
        formats.add(BarcodeFormat.EAN13);
        mScannerView.setFormats(formats);

        return mScannerView;
    }

    @Override
    public void onStart() {
        super.onStart();
        //fetch details from the parent activity through intent.getActivity().getIntent()

    }

    @Override
    public void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this); // Register ourselves as a handler for scan results.
        mScannerView.startCamera();          // Start camera on resume
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();           // Stop camera on pause
    }

}
