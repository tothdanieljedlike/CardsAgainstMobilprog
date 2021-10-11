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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class WaitingRoom extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_room);

        final String GameId= getIntent().getStringExtra("GAME_ID");

        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        final Random r = new Random();
        final int[] Added = {0};
        final int[] AddedPlayers = {0};

        /** Vissza gomb -> Szoba választás **/
        final Button BackButton = findViewById(R.id.BackButton_wr);
        BackButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(WaitingRoom.this, RoomSelector.class));
            }
        });

        final DocumentReference docRef = db.collection("Games").document(GameId);

        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    new AlertDialog.Builder(WaitingRoom.this).setMessage("Valami hiba történt, ellenőrizd az internetkapcsolatot!")
                            .setPositiveButton("OK", null)
                            .show();
                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    if(snapshot.getBoolean("isStarted"))
                    {
                        Intent intent = new Intent(getBaseContext(), GameActivity.class);
                        intent.putExtra("GAME_ID", GameId);
                        intent.putExtra("PLAYER_ID", Added[0]);
                        startActivity(intent);
                    }
                    else {
                        Map<String, Object> Players = (Map<String, Object>) snapshot.get("Players");

                        int PlayerNumbers = 1;
                        while (Players.containsKey(Integer.toString(PlayerNumbers))) {
                            PlayerNumbers++;
                        }

                        if (Added[0] == 0) {
                            Map<String, Object> NPlayer = new HashMap<>();
                            NPlayer.put("PlayerName", getIntent().getStringExtra("UserName"));
                            NPlayer.put("PlayerScore", 0);
                            NPlayer.put("SelectedCard", 0);
                            Added[0] = PlayerNumbers;
                            docRef.update("Players." + Integer.toString(PlayerNumbers), NPlayer);
                        } else if (AddedPlayers[0] < PlayerNumbers) {

                            for (int i = AddedPlayers[0] + 1; i < PlayerNumbers; i++) {
                                Map<String, Object> Player = (Map<String, Object>) Players.get(Integer.toString(i));
                                Button myButton = new Button(WaitingRoom.this);
                                myButton.setText(Player.get("PlayerName").toString());
                                myButton.setId(r.nextInt(100000));

                                LinearLayout layout = findViewById(R.id.WaitingLayout);
                                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                                layout.addView(myButton, lp);

/*                        int id = myButton.getId();
                       Button btn1 =  findViewById(id);
                       final String DId = dc.getDocument().getId();

                       btn1.setOnClickListener(new View.OnClickListener() {
                           public void onClick(View view) {
                               Intent intent = new Intent(getBaseContext(), WaitingRoom.class);
                               intent.putExtra("GAME_ID", DId);
                               startActivity(intent);
                           }
                       });*/

                            }
                            AddedPlayers[0] = PlayerNumbers;
                        }
                    }

                } else {
                    new AlertDialog.Builder(WaitingRoom.this).setMessage("A szoba már nem létezik!")
                            .setPositiveButton("OK", null)
                            .show();
                }
            }
        });
    }
    @Override
    public void onBackPressed() {
        startActivity(new Intent(WaitingRoom.this, RoomSelector.class));
        super.onBackPressed();
    }
}