package it.jaschke.alexandria;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;


import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;

import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.data.NetworkUtil;
import it.jaschke.alexandria.services.BookService;


public class AddBook extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = AddBook.class.getSimpleName();
    private EditText ean;
    private final int LOADER_ID = 1;
    private static final int ISBN_SCAN_ACTIVITY = 1;
    private View rootView;
    private final String EAN_CONTENT="eanContent";

    public static final String ISBN_RESULT = "isbnResult";

    public AddBook(){
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(ean!=null) {
            outState.putString(EAN_CONTENT, ean.getText().toString());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null && data.getStringExtra(ISBN_RESULT) != "-1") {
            ean.setText(data.getStringExtra(ISBN_RESULT));
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_add_book, container, false);
        ean = (EditText) rootView.findViewById(R.id.ean);

        ean.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //no need
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //no need
            }

            @Override
            public void afterTextChanged(Editable s) {
                String ean =s.toString();
                //catch isbn10 numbers
                if(ean.length()==10 && !ean.startsWith("978")){
                    ean="978"+ean;
                }
                if(ean.length()<13){
                    clearFields();
                    return;
                }
                //Once we have an ISBN, start a book intent
                /*Intent bookIntent = new Intent(getActivity(), BookService.class);
                bookIntent.putExtra(BookService.EAN, ean);
                bookIntent.setAction(BookService.FETCH_BOOK);
                getActivity().startService(bookIntent);*/
                GetIsbnResultTask getIsbnResultTask = new GetIsbnResultTask();
                getIsbnResultTask.execute(ean);
                //AddBook.this.restartLoader();
            }
        });

        rootView.findViewById(R.id.scan_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // This is the callback method that the system will invoke when your button is
                // clicked. You might do this by launching another app or by including the
                //functionality directly in this app.
                // Hint: Use a Try/Catch block to handle the Intent dispatch gracefully, if you
                // are using an external app.

                Intent scanIntent = new Intent(getActivity(), ScanActivity.class);
                startActivityForResult(scanIntent, ISBN_SCAN_ACTIVITY);
            }
        });

        rootView.findViewById(R.id.save_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ean.setText("");
            }
        });

        rootView.findViewById(R.id.delete_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent bookIntent = new Intent(getActivity(), BookService.class);
                bookIntent.putExtra(BookService.EAN, ean.getText().toString());
                bookIntent.setAction(BookService.DELETE_BOOK);
                getActivity().startService(bookIntent);
                ean.setText("");
            }
        });

        if(savedInstanceState!=null){
            ean.setText(savedInstanceState.getString(EAN_CONTENT));
            ean.setHint("");
        }

        return rootView;
    }

    private void restartLoader(){
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if(ean.getText().length()==0){
            return null;
        }
        String eanStr= ean.getText().toString();
        if(eanStr.length()==10 && !eanStr.startsWith("978")){
            eanStr="978"+eanStr;
        }
        Log.d(TAG, "Searching with eanStr: " + eanStr);
        return new CursorLoader(
                getActivity(),
                AlexandriaContract.BookEntry.buildFullBookUri(Long.parseLong(eanStr)),
                null,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        if (!data.moveToFirst()) {
            if (!NetworkUtil.isNetworkAvailable(getActivity())) {
                Intent messageIntent = new Intent(MainActivity.MESSAGE_EVENT);
                messageIntent.putExtra(MainActivity.MESSAGE_KEY,"Unable to fetch data, you are not connected to the internet.");
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(messageIntent);
                Log.e(TAG, "No internet connection. Load finished with No results.");
            } else {
                Log.e(TAG, "Network IS connected. Load finished with No results.");
            }
            //do nothing found toast
            ((TextView) rootView.findViewById(R.id.bookTitle)).setText("No Book Found.");
            return;
        } else {
            //hide keyboard
            // Check if no view has focus:
            View view = getActivity().getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
            }
        }

        String bookTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.TITLE));
        ((TextView) rootView.findViewById(R.id.bookTitle)).setText(bookTitle);

        String bookSubTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.SUBTITLE));
        ((TextView) rootView.findViewById(R.id.bookSubTitle)).setText(bookSubTitle);

        String[] authorsArr;
        String authors = data.getString(data.getColumnIndex(AlexandriaContract.AuthorEntry.AUTHOR));

        if (authors == null) {
            authors = "No Authors Found";
            authorsArr = new String[]{authors};
        } else {
            authorsArr = authors.split(",");
        }
        Log.e(TAG, authors);

        ((TextView) rootView.findViewById(R.id.authors)).setLines(authorsArr.length);
        ((TextView) rootView.findViewById(R.id.authors)).setText(authors.replace(",","\n"));
        String imgUrl = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.IMAGE_URL));
        if(Patterns.WEB_URL.matcher(imgUrl).matches()){
            //new DownloadImage((ImageView) rootView.findViewById(R.id.bookCover)).execute(imgUrl);
            Glide.with(getActivity()).load(imgUrl).into((ImageView) rootView.findViewById(R.id.bookCover));
            rootView.findViewById(R.id.bookCover).setVisibility(View.VISIBLE);
        }

        //data.close();
        String categories = data.getString(data.getColumnIndex(AlexandriaContract.CategoryEntry.CATEGORY));
        ((TextView) rootView.findViewById(R.id.categories)).setText(categories);



        rootView.findViewById(R.id.save_button).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.delete_button).setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {

    }

    private void clearFields(){
        ((TextView) rootView.findViewById(R.id.bookTitle)).setText("");
        ((TextView) rootView.findViewById(R.id.bookSubTitle)).setText("");
        ((TextView) rootView.findViewById(R.id.authors)).setText("");
        ((TextView) rootView.findViewById(R.id.categories)).setText("");
        rootView.findViewById(R.id.bookCover).setVisibility(View.INVISIBLE);
        rootView.findViewById(R.id.save_button).setVisibility(View.INVISIBLE);
        rootView.findViewById(R.id.delete_button).setVisibility(View.INVISIBLE);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activity.setTitle(R.string.scan);
    }

    //todo: add loading animation here in pre-execute, hide in post execute.  Handle error conditions better
    private class GetIsbnResultTask extends AsyncTask<String, Void, Void> {

        private final String LOG_TAG = GetIsbnResultTask.class.getSimpleName();

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Log.d(LOG_TAG, "on post execute");
            //now search the database for the provided isbn
            restartLoader();
        }

        @Override
        protected Void doInBackground(String... params) {
            if (params == null) return null;
            fetchBook(params[0]);
            return null;
        }

        /**
         * Handle action fetchBook in the provided background thread with the provided
         * parameters.
         */
        private void fetchBook(String ean) {

            if (ean.length() != 13) {
                return;
            }

            //check if book has already been fetched
            Cursor bookEntry = getActivity().getContentResolver().query(
                    AlexandriaContract.BookEntry.buildBookUri(Long.parseLong(ean)),
                    null, // leaving "columns" null just returns all the columns.
                    null, // cols for "where" clause
                    null, // values for "where" clause
                    null  // sort order
            );

            int bookCount = bookEntry.getCount();
            bookEntry.close();

            if (bookCount > 0) return;


            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String bookJsonString = null;

            try {
                final String FORECAST_BASE_URL = "https://www.googleapis.com/books/v1/volumes?";
                final String QUERY_PARAM = "q";

                final String ISBN_PARAM = "isbn:" + ean;

                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, ISBN_PARAM)
                        .build();

                URL url = new URL(builtUri.toString());

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    return;
                }

                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                    buffer.append("\n");
                }

                if (buffer.length() == 0) {
                    return;
                }
                bookJsonString = buffer.toString();
            } catch (UnknownHostException e) {
                //UnknownHostException if site is unreachable.
            } catch (Exception e) {

                Log.e(LOG_TAG, "Error reader" + bookJsonString + e.getClass());
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            final String ITEMS = "items";

            final String VOLUME_INFO = "volumeInfo";

            final String TITLE = "title";
            final String SUBTITLE = "subtitle";
            final String AUTHORS = "authors";
            final String DESC = "description";
            final String CATEGORIES = "categories";
            final String IMG_URL_PATH = "imageLinks";
            final String IMG_URL = "thumbnail";

            try {
                //network was down?
                if (bookJsonString == null) {
                    return;
                }

                JSONObject bookJson = new JSONObject(bookJsonString);
                JSONArray bookArray;
                if (bookJson.has(ITEMS)) {
                    bookArray = bookJson.getJSONArray(ITEMS);
                } else {
                    //Intent messageIntent = new Intent(MainActivity.MESSAGE_EVENT);
                    //messageIntent.putExtra(MainActivity.MESSAGE_KEY,getResources().getString(R.string.not_found));
                    //LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(messageIntent);
                    return;
                }

                JSONObject bookInfo = ((JSONObject) bookArray.get(0)).getJSONObject(VOLUME_INFO);

                String title = bookInfo.getString(TITLE);

                String subtitle = "";
                if (bookInfo.has(SUBTITLE)) {
                    subtitle = bookInfo.getString(SUBTITLE);
                }

                String desc = "";
                if (bookInfo.has(DESC)) {
                    desc = bookInfo.getString(DESC);
                }

                String imgUrl = "";
                if (bookInfo.has(IMG_URL_PATH) && bookInfo.getJSONObject(IMG_URL_PATH).has(IMG_URL)) {
                    imgUrl = bookInfo.getJSONObject(IMG_URL_PATH).getString(IMG_URL);
                }

                writeBackBook(ean, title, subtitle, desc, imgUrl);

                if (bookInfo.has(AUTHORS)) {
                    writeBackAuthors(ean, bookInfo.getJSONArray(AUTHORS));
                }
                if (bookInfo.has(CATEGORIES)) {
                    writeBackCategories(ean, bookInfo.getJSONArray(CATEGORIES));
                }

            } catch (JSONException e) {
                Intent messageIntent = new Intent(MainActivity.MESSAGE_EVENT);
                messageIntent.putExtra(MainActivity.MESSAGE_KEY, "Unexpected, unusable result from search");
                //LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(messageIntent);
                Log.e(LOG_TAG, "Error, here's the web result: " + bookJsonString, e);
                return;
            }
        }

        private void writeBackBook(String ean, String title, String subtitle, String desc, String imgUrl) {
            ContentValues values = new ContentValues();
            values.put(AlexandriaContract.BookEntry._ID, ean);
            values.put(AlexandriaContract.BookEntry.TITLE, title);
            values.put(AlexandriaContract.BookEntry.IMAGE_URL, imgUrl);
            values.put(AlexandriaContract.BookEntry.SUBTITLE, subtitle);
            values.put(AlexandriaContract.BookEntry.DESC, desc);
            getActivity().getContentResolver().insert(AlexandriaContract.BookEntry.CONTENT_URI, values);
        }

        private void writeBackAuthors(String ean, JSONArray jsonArray) throws JSONException {
            ContentValues values = new ContentValues();
            for (int i = 0; i < jsonArray.length(); i++) {
                values.put(AlexandriaContract.AuthorEntry._ID, ean);
                values.put(AlexandriaContract.AuthorEntry.AUTHOR, jsonArray.getString(i));
                getActivity().getContentResolver().insert(AlexandriaContract.AuthorEntry.CONTENT_URI, values);
                values = new ContentValues();
            }
        }

        private void writeBackCategories(String ean, JSONArray jsonArray) throws JSONException {
            ContentValues values = new ContentValues();
            for (int i = 0; i < jsonArray.length(); i++) {
                values.put(AlexandriaContract.CategoryEntry._ID, ean);
                values.put(AlexandriaContract.CategoryEntry.CATEGORY, jsonArray.getString(i));
                getActivity().getContentResolver().insert(AlexandriaContract.CategoryEntry.CONTENT_URI, values);
                values = new ContentValues();
            }
        }
    }
}
