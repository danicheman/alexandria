package it.jaschke.alexandria;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

/**
 * Created by NICK on 9/27/2015.
 * <p/>
 * On a phone layout, launch the scan fragment
 */
public class IsbnScanActivity extends ActionBarActivity implements IsbnScanFragment.Callback {

    private String scannedIsbn = "-1";
    private static final String TAG = IsbnScanActivity.class.getSimpleName();

    //empty constructor necessary to launch this activity.
    public IsbnScanActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_scanner_fragment);

        /*if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new IsbnScanFragment())
                    .commit();
        }*/
    }

    /**
     * When you press back button on the action bar in the details activity,
     * you’ll see the state of the main activity is lost (if you had scrolled
     * in the list somewhere it just reloads). Here’s a neat trick to prevent
     * that from happening:
     */
    @Override
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public Intent getParentActivityIntent() {
        // add the clear top flag - which checks if the parent (main)
        // activity is already running and avoids recreating it
        return super.getParentActivityIntent()
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    //return isbn to addBook screen
    @Override
    public void recieveIsbn(String isbn) {
        Intent output = new Intent();
        output.putExtra(AddBook.ISBN_RESULT, isbn);
        setResult(0, output);
        finish();
    }

    /**
     * set an activity result that tells the previous activity what barcode was scanned
     */
    @Override
    public void onBackPressed() {
        Intent output = new Intent();
        output.putExtra(AddBook.ISBN_RESULT, scannedIsbn);
        setResult(0, output);
        super.onBackPressed();
    }
}