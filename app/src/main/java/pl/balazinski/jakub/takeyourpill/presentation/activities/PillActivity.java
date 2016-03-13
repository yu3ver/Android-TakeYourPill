package pl.balazinski.jakub.takeyourpill.presentation.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import pl.balazinski.jakub.takeyourpill.R;
import pl.balazinski.jakub.takeyourpill.data.Constants;
import pl.balazinski.jakub.takeyourpill.data.database.DatabaseHelper;
import pl.balazinski.jakub.takeyourpill.data.database.DatabaseRepository;
import pl.balazinski.jakub.takeyourpill.data.database.OuterPillDatabase;
import pl.balazinski.jakub.takeyourpill.data.database.Pill;
import pl.balazinski.jakub.takeyourpill.presentation.OutputProvider;

/**
 * Activity lets you add or edit pills
 */
public class PillActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private final String TAG = getClass().getSimpleName();

    //States for setting up components for adding or edition

    private enum State {
        NEW, EDIT
    }

    //Setting up components for activity
    @Bind(R.id.toolbarPill)
    Toolbar toolbar;

    public AutoCompleteTextView pillNameEditText;

    @Bind(R.id.pill_desc)
    public EditText pillDescEditText;
    @Bind(R.id.pill_dose)
    public EditText pillDosageEditText;

    //Optional
    @Bind(R.id.optional_layout)
    public LinearLayout optionalLayout;
    @Bind(R.id.pill_count)
    public EditText pillCountEditText;
    @Bind(R.id.active_substance_et)
    public EditText pillActiveSubEditText;
    @Bind(R.id.pill_price)
    public EditText pillPriceEditText;
    @Bind(R.id.pill_barcode)
    public EditText pillBarcodeEditText;
    @Bind(R.id.add_photo)
    public Button addPhoto;
    @Bind(R.id.add_pill)
    public Button addPill;

    private OuterPillDatabase outerPillDatabase;
    private OutputProvider outputProvider;
    private String mName;
    private State state;
    private Pill mPill = null;
    private Uri imageUri = null;
    public static int id = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pill);
        ButterKnife.bind(this);
        Bundle extras = getIntent().getExtras();
        outputProvider = new OutputProvider(this);
        outerPillDatabase = new OuterPillDatabase(getApplicationContext());


        pillNameEditText = (AutoCompleteTextView) findViewById(R.id.pill_name);
        pillNameEditText.setOnItemClickListener(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, getPillNameList());
        pillNameEditText.setAdapter(adapter);
        pillNameEditText.setThreshold(2);


        /*
         * If extras are empty state is new otherwise
         * state is edit and edited pill must be loaded
         * from database.
         */

        if (extras == null) {
            state = State.NEW;
            setView(state);
        } else {
            state = State.EDIT;
            Long mId = extras.getLong(Constants.EXTRA_LONG_ID);


            mPill = DatabaseRepository.getPillByID(this, mId);
            if (mPill == null)
                outputProvider.displayShortToast("Error loading pills");
            else {
                setView(state);
                imageUri = Uri.parse(mPill.getPhoto());
            }
        }

        /*
         * Setting up notification bar color:
         * 1. Clear FLAG_TRANSLUCENT_STATUS flag
         * 2. Add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
         * 3. Change the color
         */
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.notification_bar));


        //Setting up toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    /**
     * Sets components to view.
     *
     * @param state lets method know to set up add or edit view
     */
    private void setView(State state) {


        if (state == State.NEW) {
            addPill.setText("SAVE");
            addPhoto.setText("ADD PHOTO");
        } else if (state == State.EDIT) {
            addPill.setText("UPDATE");
            addPhoto.setText("EDIT PHOTO");

            pillNameEditText.setText(mPill.getName());

            if (!mPill.getDescription().equals(""))
                pillDescEditText.setText(mPill.getDescription());
            if (mPill.getDosage() != -1)
                pillDosageEditText.setText(String.valueOf(mPill.getDosage()));
            if (mPill.getPillsCount() != -1)
                pillCountEditText.setText(String.valueOf(mPill.getPillsCount()));
            if (!mPill.getActiveSubstance().equals(""))
                pillActiveSubEditText.setText(mPill.getActiveSubstance());
            if (!mPill.getPrice().equals(""))
                pillPriceEditText.setText(mPill.getPrice());
            if (mPill.getBarcodeNumber() != -1)
                pillBarcodeEditText.setText(String.valueOf(mPill.getBarcodeNumber()));
        }

        final int version = Build.VERSION.SDK_INT;
        if (version >= 23) {
            addPill.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.button_background));
            addPhoto.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.button_background));
        } else {
            addPill.setBackground(getApplicationContext().getResources().getDrawable(R.drawable.button_background));
            addPhoto.setBackground(getApplicationContext().getResources().getDrawable(R.drawable.button_background));
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String pillDbName = (String) parent.getItemAtPosition(position);
        Cursor cursor = outerPillDatabase.getReadableDatabase()
                .rawQuery("SELECT * FROM " + Constants.TABLE_NAME + " where nazwa = " + "\"" + pillDbName + "\"" + ";", null);

        if (cursor.getCount() != 0) {
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToPosition(i);
            }
            String activeSubstance = cursor.getString(1);
            String nameAndDesc = cursor.getString(2);
            String[] nameSplit = nameAndDesc.split(",");
            String name = nameSplit[0];
            StringBuilder builder = new StringBuilder();
            for (int i = 1; i < nameSplit.length; i++)
                builder.append(nameSplit[i]);

            String description = builder.toString();

            String countString = cursor.getString(3);
            String[] countSplit = countString.split(" ");
            int count = Integer.parseInt(countSplit[0]);
            long barcode = cursor.getLong(4);
            String price = cursor.getString(5);
            int dosage = -1;

            Uri uri = Uri.parse("android.resource://pl.balazinski.jakub.takeyourpill/" + R.drawable.pill_white_background);

            mPill = new Pill();
            mPill.setName(name);
            mPill.setDescription(description);
            mPill.setDosage(dosage);
            mPill.setPillsCount(count);
            mPill.setActiveSubstance(activeSubstance);
            mPill.setPrice(price);
            mPill.setBarcodeNumber(barcode);
            mPill.setPhoto(uri);
            onChecked(true);

            setView(State.EDIT);
            setView(State.NEW);
        }

    }

    @OnClick(R.id.add_pill)
    public void addPill(View view) {
        int mCount = -1;
        int mDosage = -1;
        String mDesc = "";
        String activeSubstance = "";
        String price = "";
        long barcode = -1;


        if (pillNameEditText.getText() != null)
            mName = pillNameEditText.getText().toString();
        if (pillDescEditText.getText() != null)
            mDesc = pillDescEditText.getText().toString();
        if (!pillDosageEditText.getText().toString().equals(""))
            mDosage = Integer.parseInt(pillDosageEditText.getText().toString());

        //if(optionalLayout.getVisibility() == View.VISIBLE)
        if (!pillCountEditText.getText().toString().equals(""))
            mCount = Integer.parseInt(pillCountEditText.getText().toString());
        if (pillActiveSubEditText.getText() != null)
            activeSubstance = pillActiveSubEditText.getText().toString();
        if (pillPriceEditText.getText() != null)
            price = pillPriceEditText.getText().toString();
        if (!pillBarcodeEditText.getText().toString().equals(""))
            barcode = Long.parseLong(pillBarcodeEditText.getText().toString());


        if (TextUtils.isEmpty(mName))
            pillNameEditText.setError("Set name to your pill");
        /*else if (TextUtils.isEmpty(mDesc)) {
            pillDescEditText.setError("Set description to your pill");
        } else if (mDosage == -1) {
            pillDosageEditText.setError("Set dosage");*/
        else if (state == State.EDIT) {
            mPill.setName(mName);
            mPill.setDescription(mDesc);
            mPill.setDosage(mDosage);
            mPill.setPillsCount(mCount);
            mPill.setActiveSubstance(activeSubstance);
            mPill.setPrice(price);
            mPill.setBarcodeNumber(barcode);
            mPill.setPhoto(getImageUri());
            DatabaseHelper.getInstance(this).getPillDao().update(mPill);
            Intent i = new Intent(PillActivity.this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        } else if (state == State.NEW) {
            String path;
            //If path equals "" pill image is empty.
            if (getImageUri() != null)
                path = getImageUri().toString();
            else {
                Uri uri = Uri.parse("android.resource://pl.balazinski.jakub.takeyourpill/" + R.drawable.pill_white_background);
                path = uri.toString();
            }

            mPill = new Pill(mName, mDesc, mCount, mDosage, path, activeSubstance, price, barcode);
            DatabaseRepository.addPill(this, mPill);
            Intent i = new Intent(PillActivity.this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        }

    }

    @OnClick(R.id.add_photo)
    public void addPhoto(View view) {
        selectImage();
    }

    @OnCheckedChanged(R.id.optional_checkbox)
    public void onChecked(boolean checked) {
        if (checked)
            optionalLayout.setVisibility(View.VISIBLE);
        else
            optionalLayout.setVisibility(View.GONE);
    }

    /**
     * Function build dialog that let you choose between capturing
     * photo or choosing from gallery.
     */
    private void selectImage() {
        final CharSequence[] items = {"Take Photo", "Choose from Library", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(PillActivity.this);
        builder.setTitle("Add Photo!");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (items[item].equals("Take Photo")) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(intent, Constants.REQUEST_CAMERA);
                } else if (items[item].equals("Choose from Library")) {
                    Intent intent = new Intent(
                            Intent.ACTION_PICK,
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    intent.setType("image/*");
                    startActivityForResult(
                            Intent.createChooser(intent, "Select File"), Constants.SELECT_FILE);
                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    /**
     * @param requestCode Requests camera or select file
     * @param resultCode  1 if result is valid
     * @param data        Photo or Uri
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == Constants.REQUEST_CAMERA) {
                Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                if (thumbnail != null) {
                    thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
                }
                File destination = new File(Environment.getExternalStorageDirectory(),
                        System.currentTimeMillis() + ".jpg");
                FileOutputStream fo;
                try {
                    destination.createNewFile();
                    fo = new FileOutputStream(destination);
                    fo.write(bytes.toByteArray());
                    fo.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                imageUri = Uri.fromFile(destination);

            } else if (requestCode == Constants.SELECT_FILE) {
                Uri selectedImageUri = data.getData();
                setImageUri(selectedImageUri);
            }
        }
    }

    public Uri getImageUri() {
        return imageUri;
    }

    public void setImageUri(Uri imageUri) {
        this.imageUri = imageUri;
    }

    public List<String> getPillNameList() {
        List<String> arrayList = new ArrayList<>();
        Cursor cursor = outerPillDatabase.getReadableDatabase()
                .rawQuery("SELECT nazwa FROM " + Constants.TABLE_NAME + ";", null);

        if (cursor.getCount() != 0) {
            while (cursor.moveToNext()) {
                arrayList.add(cursor.getString(0));

            }
            cursor.close();
        }
        return arrayList;
    }

}