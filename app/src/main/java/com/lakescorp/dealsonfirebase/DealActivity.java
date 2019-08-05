package com.lakescorp.dealsonfirebase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

public class DealActivity extends AppCompatActivity {

    public static final int PICTURE_RESULT = 42;
    FirebaseDatabase mFirebaseDatabase;
    DatabaseReference mDatabaseReference;

    TextView txtTitle, txtPrice, txtDescription;
    TravelDeal deal;

    ImageView imageView;
    private UploadTask mUploadTask;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deal);

        //FirebaseUtils.openFirebaseReference("deals", ListActivity);
        mFirebaseDatabase = FirebaseUtils.mFirebaseDatabase;
        mDatabaseReference = FirebaseUtils.mDatabaseReference;
        mDatabaseReference = mFirebaseDatabase.getReference().child("deals");

        txtTitle = findViewById(R.id.txtTitle);
        txtPrice = findViewById(R.id.txtPrice);
        txtDescription = findViewById(R.id.txtDescription);
        imageView = findViewById(R.id.image);
        progress = findViewById(R.id.progress);

        Intent intent = getIntent();

        TravelDeal deal = (TravelDeal) intent.getSerializableExtra("Deal");

        if(deal == null) {
            deal = new TravelDeal();
        }

        this.deal = deal;

        txtTitle.setText(deal.getTitle());
        txtPrice.setText(deal.getPrice());
        txtDescription.setText(deal.getDescription());
        //Show the image
        showImage(deal.getImageUrl());

        Button btnImage = findViewById(R.id.btnImage);
        btnImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Insert Picture"), PICTURE_RESULT);
            }
        });



        /*mFirebaseDatabase = FirebaseDatabase.getInstance();
        mDatabaseReference = mFirebaseDatabase.getReference().child("deals");*/

    }

    private void saveDeal() {

        /*String title = txtTitle.getText().toString().trim();
        String price = txtPrice.getText().toString().trim();
        String description = txtDescription.getText().toString().trim();
        TravelDeal deal = new TravelDeal(title, price, description, "");
        mDatabaseReference.push().setValue(deal); */
        if(mUploadTask != null && mUploadTask.isInProgress()) {
            Toast.makeText(getApplicationContext(), R.string.file_upload_info, Toast.LENGTH_LONG).show();
        }
        else {
            deal.setTitle(txtTitle.getText().toString().trim());
            deal.setPrice(txtPrice.getText().toString().trim());
            deal.setDescription(txtDescription.getText().toString());

            if(deal.getId() == null) {
                mDatabaseReference.push().setValue(deal);
            }
            else {
                mDatabaseReference.child(deal.getId()).setValue(deal);
            }

            Toast.makeText(this.getApplicationContext(), R.string.deal_saved, Toast.LENGTH_LONG).show();
            clean();
            backToList();
        }



    }

    private void deleteDeal() {
        if (deal == null) {
            Toast.makeText(this, "Please save the deal before deleting", Toast.LENGTH_LONG).show();
            return;
        }

        mDatabaseReference.child(deal.getId()).removeValue();

        //Delete the file from the Storage as well
        if(deal.getImageName() != null && !deal.getImageName().isEmpty()) {
            StorageReference ref = FirebaseUtils.mStorage.getReference().child(deal.getImageName());

            //Delete the file
            ref.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Toast.makeText(getApplicationContext(),
                            "Image removed from cloud storage", Toast.LENGTH_LONG).show();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getApplicationContext(),
                            e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICTURE_RESULT && resultCode == RESULT_OK){

            //Show the upload progress
            toggleProgress();
            //Get the file URI from the data object
            Uri imageUrl = data.getData();

            //Set reference of storage
            //StorageReference ref = FirebaseUtils.mStorageRef.child(imageUrl.getLastPathSegment());
            StorageReference ref = FirebaseUtils.mStorageRef.child(System.currentTimeMillis() + ".jpg");

            mUploadTask = ref.putFile(imageUrl);

            mUploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    //Use this method to get the file url
                    /*String url = taskSnapshot.getMetadata().getReference().getDownloadUrl().toString();
                    Log.v("UPLOAD URL", "The image upload url is " + url );
                    deal.setImageUrl(url);*/


                    //Or this to get the file url
                    if(taskSnapshot.getMetadata() != null) {

                        //Get the imageName into the deal
                        String imageName = taskSnapshot.getStorage().getName();
                        deal.setImageName(imageName);

                        Task<Uri> result = taskSnapshot.getStorage().getDownloadUrl();
                        result.addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {

                                String url = uri.toString();
                                //Log.d("IMAGE_URL", "image url received from firebase " + url);
                                toggleProgress();
                                deal.setImageUrl(url);
                                showImage(url);
                                Toast.makeText(getApplicationContext(), "Upload completed", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }


                }
            });
        }
    }

    private void showImage(String url) {

        if(url != null && !url.isEmpty()) {

            //get the screen width
            int width = Resources.getSystem().getDisplayMetrics().widthPixels;
            Picasso.with(this)
                    .load(url)
                    .resize(width, width * 2/3)
                    .centerCrop()
                    .into(imageView);
        }
    }

    private void toggleProgress() {
        if(progress.getVisibility() == View.INVISIBLE) {
            progress.setVisibility(View.VISIBLE);
        }else {
            progress.setVisibility(View.INVISIBLE);
        }
    }

    private void backToList() {
        Intent intent = new Intent(this, ListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }


    private void clean() {

        txtTitle.setText("");
        txtPrice.setText("");
        txtDescription.setText("");

        txtTitle.requestFocus();

    }

    private void enableEditTexts(boolean isEnabled) {
        txtTitle.setEnabled(isEnabled);
        txtDescription.setEnabled(isEnabled);
        txtPrice.setEnabled(isEnabled);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.save_menu, menu);

        //Only show the menu when the user is authorized to see the item
        if(FirebaseUtils.isAdmin) {
            menu.findItem(R.id.delete_menu).setVisible(true);
            menu.findItem(R.id.save_menu).setVisible(true);
            enableEditTexts(true);
            findViewById(R.id.btnImage).setEnabled(true);
        }
        else {
            menu.findItem(R.id.delete_menu).setVisible(false);
            menu.findItem(R.id.save_menu).setVisible(false);
            enableEditTexts(false);
            findViewById(R.id.btnImage).setEnabled(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {

            case(R.id.save_menu):
                saveDeal();
                return true;

            case R.id.delete_menu:
                deleteDeal();
                Toast.makeText(this, R.string.deal_deleted, Toast.LENGTH_LONG).show();
                backToList();
                return true;

                default:
                    return super.onOptionsItemSelected(item);
        }


    }


}
