package com.example.beadand;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class RoomSelector extends AppCompatActivity {

    public static final String MyPREFERENCES = "GamePrefs" ;
    SharedPreferences sharedpreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_selector);
        sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        FirebaseApp.initializeApp(this);
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        final Random r = new Random();
        LayoutInflater factory = LayoutInflater.from(this);

//text_entry is an Layout XML file containing two text field to display in alert dialog
        final View textEntryView = factory.inflate(R.layout.text_entry, null);

        final EditText input1 = (EditText) textEntryView.findViewById(R.id.roomtitle);
        final TextView pwdtext = textEntryView.findViewById(R.id.pwdtext);
        final EditText input2 = (EditText) textEntryView.findViewById(R.id.pwd);
        /** vissza gomb -> main screen **/
        final Button BackButton = findViewById(R.id.BackButton);
        BackButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(RoomSelector.this, MainActivity.class));
            }
        });
/**********************************************************************************/
/****************************** Új szoba létrehozása ******************************/
/**********************************************************************************/
        final Button NewButton = findViewById(R.id.NewButton);
        NewButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                final EditText input = new EditText(RoomSelector.this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);

                /** Felugró ablak a szoba nevének bevitelére **/
                new AlertDialog.Builder(RoomSelector.this).setMessage("Szoba neve:")
                        .setView(textEntryView)
                        .setPositiveButton("Létrehozás", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                /** Game collection paraméterei **/
                                Map<String, Object> Game = new HashMap<>();
                                Game.put("Name", input1.getText().toString());
                                if(!input2.getText().toString().isEmpty())
                                {
                                    Game.put("Password", input2.getText().toString());
                                }else {
                                    Game.put("Password", "");
                                }
                                Game.put("Admin", sharedpreferences.getString("PlayerName", "Guest"));
                                Game.put("TurnPlayer", 1);
                                Game.put("IsStarted", false);
                                Map<String, Object> Players = new HashMap<>();
                                Map<String, Object> Player = new  HashMap<>();
                                Player.put("PlayerName",sharedpreferences.getString("PlayerName","Guest"));
                                Player.put("PlayerScore", 0);
                                Player.put("SelectedCard",0);
                                Players.put("1",Player);
                                Game.put("Players", Players);
                                Game.put("isStarted",false);
                                Game.put("isContinued",false);
                                Game.put("isTurnEnded",false);

                                /** Továbbléptetés a szobába **/
                                db.collection("Games")
                                        .add(Game)
                                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                            @Override
                                            public void onSuccess(DocumentReference documentReference) {
                                                Intent intent = new Intent(getBaseContext(), WaitingRoomAdmin.class);
                                                intent.putExtra("GAME_ID", documentReference.getId());
                                                startActivity(intent);
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                new AlertDialog.Builder(RoomSelector.this).setMessage("Nem sikerült létrehozni  a szobát!")
                                                        .setPositiveButton("OK", null)
                                                        .show();
                                            }
                                        });

                            }})
                        .show();


            }
        });

/***********************************************************************************/
/**************************** Létező szoba kiválasztása ****************************/
/***********************************************************************************/
        db.collection("Games")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            new AlertDialog.Builder(RoomSelector.this).setMessage("Valami hiba történt, ellenőrizd az internetkapcsolatot! - " + e.getMessage())
                                    .setPositiveButton("OK", null)
                                    .show();
                            return;
                        }

                        /** Minden már létező szobához gomb létrehozása **/
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                Button myButton = new Button(RoomSelector.this);
                                myButton.setText(dc.getDocument().getString("Name"));
                                int i= r.nextInt(100000);
                                myButton.setId(i);

                                LinearLayout layout = findViewById(R.id.Scroll);
                                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                                layout.addView(myButton, lp);

                                int id = myButton.getId();
                                Button btn1 =  findViewById(id);
                                final String DId = dc.getDocument().getId();

                                /** Belépteti a játékost a megfelelő felületre **/
                                btn1.setOnClickListener(new View.OnClickListener() {
                                    public void onClick(View view) {
                                        if(!dc.getDocument().getString("Password").equals(""))
                                        {
                                            final EditText input = new EditText(RoomSelector.this);
                                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                                    LinearLayout.LayoutParams.MATCH_PARENT);


                                            new AlertDialog.Builder(RoomSelector.this).setMessage("Szoba jelszava:")
                                                    .setView(input)
                                                    .setPositiveButton("Belépés", new DialogInterface.OnClickListener(){
                                                        public void onClick(DialogInterface dialog, int which)
                                                        {
                                                            Log.d("sad",dc.getDocument().getString("Password"));
                                                            if(input.getText().toString().equals(dc.getDocument().getString("Password")))
                                                            {
                                                                String player = sharedpreferences.getString("PlayerName", "Guest");
                                                                String admin = dc.getDocument().getString("Admin");
                                                                Intent intent;
                                                                if(player.equals(admin)) {
                                                                    intent = new Intent(getBaseContext(), WaitingRoomAdmin.class);
                                                                }
                                                                else{
                                                                    intent = new Intent(getBaseContext(), WaitingRoom.class);
                                                                    intent.putExtra("GAME_ID", DId);
                                                                    intent.putExtra("UserName", sharedpreferences.getString("PlayerName", "Guest"));
                                                                }
                                                                startActivity(intent);
                                                            }else
                                                            {
                                                                new AlertDialog.Builder(RoomSelector.this).setMessage("No senior, jelszó nem lenni jó! Próbáld meg később...")
                                                                        .setPositiveButton("OK", null)
                                                                        .show();
                                                                return;
                                                            }

                                                        }
                                                    }).show();
                                        }else
                                        {
                                            String player = sharedpreferences.getString("PlayerName", "Guest");
                                            String admin = dc.getDocument().getString("Admin");
                                            Intent intent;
                                            if(player.equals(admin)) {
                                                intent = new Intent(getBaseContext(), WaitingRoomAdmin.class);
                                            }
                                            else{
                                                intent = new Intent(getBaseContext(), WaitingRoom.class);
                                                intent.putExtra("GAME_ID", DId);
                                                intent.putExtra("UserName", sharedpreferences.getString("PlayerName", "Guest"));
                                            }
                                            startActivity(intent);
                                        }

                                    }
                                });
                            }
                        }

                    }
                });

    }
    /** Vissza -> main screen **/
    @Override
    public void onBackPressed() {
        startActivity(new Intent(RoomSelector.this, MainActivity.class));
        super.onBackPressed();
    }
}
