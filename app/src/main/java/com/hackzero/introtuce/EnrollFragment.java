package com.hackzero.introtuce;


import android.content.ContentResolver;
import android.content.Intent;

import android.net.Uri;
import android.os.Bundle;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import android.widget.ProgressBar;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import java.util.Calendar;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.app.Activity.RESULT_OK;

public class EnrollFragment extends Fragment {
    Uri imageUri;
    CircleImageView photo;

    EditText firstName, lastName, dob, gender, country, state, hometown, phone;

    ImageButton calendarBtn;

    Button addUser;

    ProgressBar loadingBar;

    User user;
    DatabaseReference ref;
    StorageReference storageReference;

    StorageTask uploadTask;
    String imageURL;

    boolean duplicateRecords = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.enroll_fragment, container, false);

        ref = FirebaseDatabase.getInstance().getReference("uploads");
        storageReference = FirebaseStorage.getInstance().getReference("uploads");

        photo = view.findViewById(R.id.profile_photo);
        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent gallery = new Intent();
                gallery.setType("image/*");
                gallery.setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(gallery, "Select Profile Photo"), 1);
            }
        });


        firstName = view.findViewById(R.id.reg_first_name);
        lastName = view.findViewById(R.id.reg_last_name);
        dob = view.findViewById(R.id.reg_dob);
        gender = view.findViewById(R.id.reg_gender);
        country = view.findViewById(R.id.reg_country);
        state = view.findViewById(R.id.reg_state);
        hometown = view.findViewById(R.id.reg_hometown);
        phone = view.findViewById(R.id.reg_phone);


        calendarBtn = view.findViewById(R.id.calendar_btn);
        calendarBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar calendar =Calendar.getInstance();
                calendar.add(Calendar.DATE, 1);
            }
        });

        loadingBar = view.findViewById(R.id.loading);


        addUser = view.findViewById(R.id.addUserBtn);
        addUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadingBar.setVisibility(View.VISIBLE);

                final String fName = firstName.getText().toString();
                final String lName = lastName.getText().toString();
                final String d = dob.getText().toString();
                final String gen = gender.getText().toString();
                final String c = country.getText().toString();
                final String s = state.getText().toString();
                final String ht = hometown.getText().toString();
                final String phn = phone.getText().toString();



                if (verifyDetails(fName, lName, d, gen, c, s, ht, phn)) {

                    // check for duplicate users based on phone numbers
                    ref.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for(DataSnapshot userSnapshot : snapshot.getChildren()) {
                                User u = userSnapshot.getValue(User.class);
                                if(u.getPhone().equals(phn)) {
                                    loadingBar.setVisibility(View.INVISIBLE);
                                    Toast.makeText(getActivity(), "User already exists", Toast.LENGTH_SHORT).show();
                                    duplicateRecords = true;
                                    break;
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });

                    if(uploadTask != null && uploadTask.isInProgress())
                        Toast.makeText(getActivity(), "Upload is in progress", Toast.LENGTH_SHORT).show();
                    else {
                        uploadPicture();

                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                user = new User(imageURL, fName, lName, d, gen, c, s, ht, phn);

                                if(duplicateRecords == false) {
                                    ref.push().setValue(user);
                                    Toast.makeText(getActivity(), "User is added", Toast.LENGTH_SHORT).show();
                                    loadingBar.setVisibility(View.INVISIBLE);

                                    //empty the fields
                                    photo.setImageResource(R.drawable.ic_account_circle);
                                    firstName.setText("");
                                    lastName.setText("");
                                    dob.setText("");
                                    gender.setText("");
                                    country.setText("");
                                    state.setText("");
                                    hometown.setText("");
                                    phone.setText("");
                                }
                            }
                        },10000);


                    }
                } else {
                    Toast.makeText(getActivity(), "Fill all the fields", Toast.LENGTH_SHORT).show();
                    loadingBar.setVisibility(View.INVISIBLE);
                }

            }
        });

        return view;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            photo.setImageURI(imageUri);
        }
    }

    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = getActivity().getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(contentResolver.getType(uri));
    }


    private void uploadPicture() {
        if(imageUri != null) {
            final StorageReference fileReference = storageReference.child(System.currentTimeMillis() + "." + getFileExtension(imageUri));
            uploadTask = fileReference.putFile(imageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Task<Uri> result = taskSnapshot.getStorage().getDownloadUrl();
                            result.addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    imageURL = uri.toString();
                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getActivity(), "Uploading Failed" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                            // when the uploading of the image is in progress
                        }
                    });
        }
        else
            Toast.makeText(getActivity(), "No Image is Selected", Toast.LENGTH_SHORT).show();
    }

    public boolean verifyDetails(String fName, String lName, String d, String gen, String c, String s, String ht, String phn) {
        if (fName.length() == 0) {
            firstName.setError("Enter first name");
            firstName.requestFocus();
            return false;
        }
        if (lName.length() == 0) {
            lastName.setError("Enter last name");
            lastName.requestFocus();
            return false;
        }
        if (d.length() == 0) {
            dob.setError("Enter valid dob");
            dob.requestFocus();
            return false;
        }
        if (!gen.equals("Male") && !gen.equals("Female") && !gen.equals("Other")) {
            firstName.setError("Invalid entry");
            firstName.requestFocus();
            return false;
        }
        if (c.length() == 0) {
            country.setError("Enter country");
            country.requestFocus();
            return false;
        }
        if (s.length() == 0) {
            state.setError("Enter state");
            state.requestFocus();
            return false;
        }
        if (ht.length() == 0) {
            hometown.setError("Enter hometown");
            hometown.requestFocus();
            return false;
        }
        if (phn.length() != 10) {
            phone.setError("Invalid Phone Number");
            phone.requestFocus();
            return false;
        }

        return true;
    }
}
