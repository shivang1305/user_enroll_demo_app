package com.hackzero.introtuce;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {
    private Context mContext;
    private List<User> mUsers;
    private FirebaseStorage mStorage;
    private DatabaseReference mDatabaseRef;

    public UsersAdapter(Context context, List<User> users) {
        mContext = context;
        mUsers = users;
        mStorage = FirebaseStorage.getInstance();
        mDatabaseRef = FirebaseDatabase.getInstance().getReference("uploads");
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(mContext).inflate(R.layout.users_list, parent, false);
        return new UserViewHolder(v);
    }


    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        final User currentUser = mUsers.get(position);

        // Image of the user
        Picasso.with(mContext)
                .load(currentUser.getImageUrl())
                .placeholder(R.mipmap.ic_launcher)
                .fit()
                .into(holder.profilePhoto);

        // full name of the user
        String name = currentUser.getFirstName() + " " + currentUser.getLastName();
        holder.fullName.setText(name);

        // age of the user


        // gender of the user
        holder.gender.setText(currentUser.getGender());

        // hometown of the user
        holder.hometown.setText(currentUser.getHometown());

        // delete user button
        holder.deleteUserBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // delete user from firebase
                final String selectedKey = currentUser.getmKey();

                StorageReference userRef = mStorage.getReferenceFromUrl(currentUser.getImageUrl());
                userRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        mDatabaseRef.child(selectedKey).removeValue();
                        Toast.makeText(mContext, "User is deleted", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @Override
    public int getItemCount() {
        return mUsers.size();
    }

    public class UserViewHolder extends RecyclerView.ViewHolder {
        public TextView fullName, age, gender, hometown;
        public ImageView deleteUserBtn;
        public CircleImageView profilePhoto;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);

            profilePhoto = itemView.findViewById(R.id.account_img);
            fullName = itemView.findViewById(R.id.user_full_name);
            age = itemView.findViewById(R.id.user_age);
            gender = itemView.findViewById(R.id.user_gender);
            hometown = itemView.findViewById(R.id.user_hometown);
            deleteUserBtn = itemView.findViewById(R.id.delete_user_btn);

        }
    }
}
