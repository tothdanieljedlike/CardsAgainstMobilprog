package com.example.beadand;

import android.app.AlertDialog;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.core.OrderBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class WaitingRoomAdmin extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_room_admin);

        final String GameId= getIntent().getStringExtra("GAME_ID");
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        final Random r = new Random();
        final int[] AddedPlayers = {0};

        final Button BackButton = findViewById(R.id.BackButton_wradmin);
        BackButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(WaitingRoomAdmin.this, RoomSelector.class));
            }
        });

        final DocumentReference docRef = db.collection("Games").document(GameId);
        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    new AlertDialog.Builder(WaitingRoomAdmin.this).setMessage("Valami hiba történt, ellenőrizd az internetkapcsolatot!")
                            .setPositiveButton("OK", null)
                            .show();
                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    Map<String,Object> Players =   (Map<String,Object>) snapshot.get("Players");

                    int PlayerNumbers = 1;
                    while(Players.containsKey(Integer.toString(PlayerNumbers)))
                    {
                        PlayerNumbers++;
                    }

                    if (AddedPlayers[0] < PlayerNumbers) {
                        for (int i = AddedPlayers[0] + 1; i < PlayerNumbers; i++) {
                            Map<String, Object> Player = (Map<String, Object>) Players.get(Integer.toString(i));
                            Button myButton = new Button(WaitingRoomAdmin.this);
                            myButton.setText(Player.get("PlayerName").toString());
                            myButton.setId(r.nextInt(100000));

                            LinearLayout layout = findViewById(R.id.WaitingLayout);
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            layout.addView(myButton, lp);

                            /*int id = myButton.getId();
                                Button btn1 = findViewById(id);
                           final String DId = dc.getDocument().getId();

                           btn1.setOnClickListener(new View.OnClickListener() {
                               public void onClick(View view) {
                                   Intent intent = new Intent(getBaseContext(), WaitingRoom.class);
                                   intent.putExtra("GAME_ID", DId);
                                   startActivity(intent);
                               }
                           });*/

                            AddedPlayers[0] = PlayerNumbers;
                        }
                    }


                } else {
                    new AlertDialog.Builder(WaitingRoomAdmin.this).setMessage("A szoba már nem létezik!")
                            .setPositiveButton("OK", null)
                            .show();
                }
            }
        });

        Button startButton = findViewById(R.id.StartButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                db.collection("WhiteCards").orderBy("numb", Query.Direction.DESCENDING).limit(1).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                final int maximum = Integer.parseInt(document.get("numb").toString());
                                docRef.get(Source.SERVER).addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        if (task.isSuccessful()) {
                                            DocumentSnapshot document = task.getResult();
                                            if (document.exists()) {
                                                /** Playerek számának meghatározása **/
                                                Map<String,Object> Players =   (Map<String,Object>) document.get("Players");

                                                int PlayerNumbers = 1;
                                                while(Players.containsKey(Integer.toString(PlayerNumbers)))
                                                {
                                                    PlayerNumbers++;
                                                }
                                                /** Kártyák kiosztása **/
                                                List<Integer> AddedCards = new ArrayList<Integer>();

                                                for (int i=0;i<PlayerNumbers-1;i++) {
                                                    Integer[] Cards = { 0,0,0,0,0 };
                                                    for (int y=0;y<5;y++) {
                                                        int iCard = r.nextInt(maximum) + 1;
                                                        if (!AddedCards.contains(iCard)) {
                                                            AddedCards.add(iCard);
                                                            Cards[y] = iCard;
                                                            if (y == 4) {
                                                                docRef.update("Players." + Integer.toString(i + 1) + ".Cards", Arrays.asList(Cards));
                                                            }
                                                        } else {
                                                            y--;
                                                        }
                                                    }
                                                }
                                                /** fekete kártya meghatározása **/

                                                db.collection("BlackCards").orderBy("numb", Query.Direction.DESCENDING).limit(1).get(Source.SERVER).addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                        if (task.isSuccessful()) {
                                                            for (QueryDocumentSnapshot document : task.getResult()) {
                                                                int maximum = Integer.parseInt(document.get("numb").toString());
                                                                int iCard = r.nextInt(maximum) + 1;
                                                                docRef.update("BlackCard",iCard);
                                                            } }else {
                                                            new AlertDialog.Builder(WaitingRoomAdmin.this).setMessage("Valami hiba történt, ellenőrizd az internetkapcsolatot!")
                                                                    .setPositiveButton("OK", null)
                                                                    .show();
                                                        }
                                                    }
                                                });


                                                docRef.update("isTurnEnded",false).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        docRef.update("isContinued",true).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                docRef.update("isStarted",true).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<Void> task) {
                                                                        Intent intent = new Intent(getBaseContext(), GameActivity.class);
                                                                        intent.putExtra("GAME_ID", GameId);
                                                                        intent.putExtra("PLAYER_ID", 1);
                                                                        startActivity(intent);
                                                                    }
                                                                });

                                                            }
                                                        });
                                                    }
                                                });


                                            } else {
                                                new AlertDialog.Builder(WaitingRoomAdmin.this).setMessage("Valami hiba történt, ellenőrizd az internetkapcsolatot!")
                                                        .setPositiveButton("OK", null)
                                                        .show();
                                            }
                                        } else {
                                            new AlertDialog.Builder(WaitingRoomAdmin.this).setMessage("Valami hiba történt, ellenőrizd az internetkapcsolatot!")
                                                    .setPositiveButton("OK", null)
                                                    .show();
                                        }
                                    }
                                });

                            }
                        } else {
                            new AlertDialog.Builder(WaitingRoomAdmin.this).setMessage("Valami hiba történt, ellenőrizd az internetkapcsolatot!")
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
                    }
                });
            }
        });
    }
    @Override
    public void onBackPressed() {
        startActivity(new Intent(WaitingRoomAdmin.this, RoomSelector.class));
        super.onBackPressed();
    }
}