
package com.example.beadand;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GameActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        final Button ContinueButton = findViewById(R.id.Continue_Button);

        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        final String GameId= getIntent().getStringExtra("GAME_ID");
        final int PlayerId = getIntent().getIntExtra("PLAYER_ID",0);
        final int[] PlayerNumbers = {0};
        final Random r = new Random();

        final boolean[] OpenedDialog = {false};
        final int[][] PlayerTextViewIds = new int[1][1];

        final List<Integer> SelectedCardsQue = new ArrayList<Integer>();

        /** Vissza gomb -> Szoba választás **/
        final Button BackButton = findViewById(R.id.BackButton_game);
        BackButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
/*                final DocumentReference docRef = db.collection("Games").document(GameId);
                docRef.update("Players." + Integer.toString(PlayerId), FieldValue.delete());*/


                startActivity(new Intent(GameActivity.this, RoomSelector.class));
            }
        });

        final DocumentReference docRef = db.collection("Games").document(GameId);
        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    new AlertDialog.Builder(GameActivity.this).setMessage("Valami hiba történt, ellenőrizd az internetkapcsolatot!")
                            .setPositiveButton("OK", null)
                            .show();
                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    final Map<String, Object> Players = (Map<String, Object>) snapshot.get("Players");
                    /** Most indult a játék **/
                    if (PlayerNumbers[0] == 0) {

                        /** Játékosok számának meghatározása**/
                        PlayerNumbers[0] = 1;
                        while (Players.containsKey(Integer.toString(PlayerNumbers[0]))) {
                            PlayerNumbers[0]++;
                        }

                        /** bal felső kiírás: player név és score **/
                        PlayerTextViewIds[0] = new int[PlayerNumbers[0]];
                        for (int i = 1; i < PlayerNumbers[0]; i++) {
                            PlayerTextViewIds[0][i - 1] = r.nextInt(100000);

                            Map<String, Object> Player = (Map<String, Object>) Players.get(Integer.toString(i));
                            TextView myText = new TextView(GameActivity.this);
                            myText.setText(Player.get("PlayerName").toString() + ": " + Player.get("PlayerScore"));
                            myText.setId(PlayerTextViewIds[0][i - 1]);

                            myText.setTextSize(18);
                            myText.setTextColor(Color.rgb(175,173, 73));

                            LinearLayout layout = findViewById(R.id.GameLayout);
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            layout.addView(myText, lp);
                            PlayerTextViewIds[0][i - 1] = myText.getId();
                        }
                    }

/**********************************************************************************/
/****************************** Nyertes kiválasztása ******************************/
/**********************************************************************************/
                    if (snapshot.getBoolean("isTurnEnded") == true && snapshot.getBoolean("isContinued") == false) {

                        final List<String> SelectedWhiteCards = new ArrayList<String>();
                        final Long BlackCardId = snapshot.getLong("BlackCard");
                        final boolean TurnerPlayer = snapshot.getLong("TurnPlayer") == PlayerId;

                        for (int i = 1; i < PlayerNumbers[0]; i++) {
                            Map<String, Object> Player = (Map<String, Object>) Players.get(Integer.toString(i));
                            TextView myText = findViewById(PlayerTextViewIds[0][i - 1]);
                            myText.setText(Player.get("PlayerName").toString() + ": " + Player.get("PlayerScore"));
                            final int PlayerSelectedCardId = i;

                            List<Long> Cards = (List<Long>) Player.get("Cards");
                            db.collection("WhiteCards").whereEqualTo("numb", snapshot.getLong("Players." + Integer.toString(i) + ".SelectedCard"))//Cards.get(snapshot.getLong("Players."+Integer.toString(i)+".SelectedCard").intValue()))//(int) Player.get("SelectedCard")))
                                    .get()
                                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                            if (task.isSuccessful()) {
                                                for (QueryDocumentSnapshot document : task.getResult()) {
                                                    SelectedWhiteCards.add(document.getString("element"));
                                                    SelectedCardsQue.add(PlayerSelectedCardId);
                                                    if (SelectedWhiteCards.size() == PlayerNumbers[0] - 2) {

                                                        db.collection("BlackCards").whereEqualTo("numb", BlackCardId)
                                                                .get()
                                                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                                        if (task.isSuccessful()) {
                                                                            for (QueryDocumentSnapshot document : task.getResult()) {
                                                                                if (TurnerPlayer) {

                                                                                    /** Felugró ablak a többi játékos által választott lapokkal **/
                                                                                    new AlertDialog.Builder(GameActivity.this).setTitle(document.getString("element"))
                                                                                            .setSingleChoiceItems(SelectedWhiteCards.toArray(new String[0]), 0, null)
                                                                                            .setPositiveButton("Kiválaszt", new DialogInterface.OnClickListener() {
                                                                                                @Override
                                                                                                public void onClick(DialogInterface dialog, int id) {
                                                                                                    dialog.dismiss();
                                                                                                    int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                                                                                                    Map<String, Object> Temporary = new HashMap<>();
                                                                                                    Map<String, Object> Player = (Map<String, Object>) Players.get(Integer.toString(SelectedCardsQue.get(selectedPosition)));
                                                                                                    Temporary.put("Players." + Integer.toString(SelectedCardsQue.get(selectedPosition)) + ".PlayerScore", ((long) Player.get("PlayerScore") + 1));
                                                                                                    Temporary.put("isTurnEnded", false);
                                                                                                    docRef.update(Temporary);
                                                                                                }
                                                                                            })
                                                                                            .show();
                                                                                }
                                                                            }
                                                                        } else {
                                                                            new AlertDialog.Builder(GameActivity.this).setMessage("Valami hiba történt!")
                                                                                    .setPositiveButton("OK", null)
                                                                                    .show();
                                                                        }
                                                                    }
                                                                });
                                                    }
                                                }
                                            } else {
                                                new AlertDialog.Builder(GameActivity.this).setMessage("Valami hiba történt!")
                                                        .setPositiveButton("OK", null)
                                                        .show();
                                            }
                                        }
                                    });
                        }

                        if (snapshot.getLong("TurnPlayer") == PlayerId) {


                        }

/**********************************************************************************/
/****************** Játékosok kiválasztják a kijátszandó kártyát ******************/
/**********************************************************************************/
                    } else if (snapshot.getBoolean("isTurnEnded") == false && snapshot.getBoolean("isContinued") == true) {
                        if (snapshot.getLong("TurnPlayer") == PlayerId) {
                            boolean AllAnswered = true;
                            for (int i = 1; i < PlayerNumbers[0]; i++) {
                                if (snapshot.getLong("Players." + Integer.toString(i) + ".SelectedCard") == 0 && i != PlayerId) {
                                    AllAnswered = false;
                                    break;
                                }
                            }
                            if (AllAnswered) {
                                Map<String, Object> Temporary = new HashMap<>();
                                Temporary.put("isTurnEnded", true);
                                Temporary.put("isContinued", false);
                                docRef.update(Temporary);
                            }

                        } else {
                            if (snapshot.getLong("Players." + Integer.toString(PlayerId) + ".SelectedCard") == 0 && OpenedDialog[0] == false) {
                                OpenedDialog[0] = true;
                                final List<String> PlayerWhiteCards = new ArrayList<String>();
                                final Long BlackCardId = snapshot.getLong("BlackCard");
                                List<Integer> PlayerWhiteCardsNumbers = (ArrayList<Integer>) snapshot.get("Players." + Integer.toString(PlayerId) + ".Cards");
                                for (int i = 0; i < PlayerWhiteCardsNumbers.size(); i++) {
                                    db.collection("WhiteCards").whereEqualTo("numb", PlayerWhiteCardsNumbers.get(i))
                                            .get()
                                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                    if (task.isSuccessful()) {
                                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                                            PlayerWhiteCards.add(document.getString("element"));
                                                            SelectedCardsQue.add(document.getLong("numb").intValue());
                                                            if (PlayerWhiteCards.size() == 5) {
                                                                db.collection("BlackCards").whereEqualTo("numb", BlackCardId)
                                                                        .get()
                                                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                                            @Override
                                                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                                                if (task.isSuccessful()) {
                                                                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                                                                        String[] s = PlayerWhiteCards.toArray(new String[0]);
                                                                                        int checkedItem = 0;
                                                                                        new AlertDialog.Builder(GameActivity.this).setTitle(document.getString("element"))
                                                                                                .setSingleChoiceItems(s, checkedItem, null)
                                                                                                .setCancelable(false)
                                                                                                .setPositiveButton("Kiválasztás", new DialogInterface.OnClickListener() {
                                                                                                    @Override
                                                                                                    public void onClick(DialogInterface dialog, int id) {
                                                                                                        dialog.dismiss();
                                                                                                        int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                                                                                                        docRef.update("Players." + Integer.toString(PlayerId) + ".SelectedCard", SelectedCardsQue.get(selectedPosition));
                                                                                                    }
                                                                                                })
                                                                                                .show();
                                                                                        Log.d("alert", "Alert dialog check"+ Integer.toString(PlayerId));
                                                                                    }
                                                                                    Log.d("alert", "Task success");
                                                                                } else {
                                                                                    new AlertDialog.Builder(GameActivity.this).setMessage("Valami hiba történt!")
                                                                                            .setPositiveButton("OK", null)
                                                                                            .show();
                                                                                }
                                                                            }
                                                                        });
                                                            }
                                                        }
                                                    } else {
                                                        new AlertDialog.Builder(GameActivity.this).setMessage("Valami hiba történt!")
                                                                .setPositiveButton("OK", null)
                                                                .show();
                                                    }
                                                }
                                            });
                                }
                            }
                        }
/**********************************************************************************/
/************************************ Kör vége ************************************/
/**********************************************************************************/
                    } else if (snapshot.getBoolean("isTurnEnded") == false && snapshot.getBoolean("isContinued") == false) {
                        if (OpenedDialog[0] == true) {
                            OpenedDialog[0] = false;
                        }
                        if (snapshot.getLong("TurnPlayer") == PlayerId) {
                            ContinueButton.setVisibility(View.VISIBLE);
                        }


                        SelectedCardsQue.removeAll(SelectedCardsQue);

                        /** updateli a player score-t **/
                        for (int i = 1; i < PlayerNumbers[0]; i++) {
                            Map<String, Object> Player = (Map<String, Object>) Players.get(Integer.toString(i));
                            TextView myText = findViewById(PlayerTextViewIds[0][i - 1]);
                            myText.setText(Player.get("PlayerName").toString() + ": " + Player.get("PlayerScore"));
                        }


                    } else {
                        new AlertDialog.Builder(GameActivity.this).setMessage("A szoba már nem létezik!")
                                .setPositiveButton("OK", null)
                                .show();
                    }
                }
            }
        });
/**********************************************************************************/
/************************************* Új kör *************************************/
/**********************************************************************************/
        ContinueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                db.collection("WhiteCards").orderBy("numb", Query.Direction.DESCENDING).limit(1).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                final int maximum_w = Integer.parseInt(document.get("numb").toString());

                                docRef.get(Source.SERVER).addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        if (task.isSuccessful()) {
                                            DocumentSnapshot document = task.getResult();
                                            final Long NextPlayer = document.getLong("TurnPlayer") + 1;

                                            db.collection("BlackCards").orderBy("numb", Query.Direction.DESCENDING).limit(1).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                    if (task.isSuccessful()) {
                                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                                            int maximum_b = Integer.parseInt(document.get("numb").toString());
                                                            int iCard_b = r.nextInt(maximum_b) + 1;
                                                            Map<String, Object> Temporary = new HashMap<>();
                                                            if (NextPlayer >= PlayerNumbers[0]) {
                                                                Temporary.put("TurnPlayer", 1);
                                                            } else {
                                                                Temporary.put("TurnPlayer", NextPlayer);
                                                            }
                                                            Temporary.put("isContinued", true);
                                                            Temporary.put("BlackCard", iCard_b);
                                                            for (int i = 1; i < PlayerNumbers[0]; i++) {
                                                                Temporary.put("Players." + Integer.toString(i) + ".SelectedCard", 0);
                                                            }
                                                            docRef.update(Temporary);
                                                        }
                                                        List<Integer> AddedCards = new ArrayList<Integer>();

                                                        for (int i = 0; i < PlayerNumbers[0] - 1; i++) {
                                                            Integer[] Cards = { 0,0,0,0,0 };
                                                            for (int y=0;y<5;y++) {
                                                                int iCard_w = r.nextInt(maximum_w) + 1;
                                                                if (!AddedCards.contains(iCard_w)) {
                                                                    AddedCards.add(iCard_w);
                                                                    Cards[y] = iCard_w;
                                                                    if (y == 4) {
                                                                       docRef.update("Players." + Integer.toString(i + 1) + ".Cards", Arrays.asList(Cards));
                                                                        Log.d("cards", Arrays.asList(Cards).toString());
                                                                    }
                                                                } else {
                                                                    y--;
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        new AlertDialog.Builder(GameActivity.this).setMessage("Valami hiba történt, ellenőrizd az internetkapcsolatot!")
                                                                .setPositiveButton("OK", null)
                                                                .show();
                                                    }
                                                }
                                            });


                                        } else {
                                            new AlertDialog.Builder(GameActivity.this).setMessage("Valami hiba történt!")
                                                    .setPositiveButton("OK", null)
                                                    .show();
                                        }
                                    }
                                });
                            }
                        }else {
                            new AlertDialog.Builder(GameActivity.this).setMessage("Valami hiba történt!")
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
                    }
                });

                ContinueButton.setVisibility(View.GONE);

            }
        });

    }

        /*ContinueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {



                docRef.get(Source.SERVER).addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            final Long NextPlayer = document.getLong("TurnPlayer")+1;


                            db.collection("BlackCards").orderBy("numb", Query.Direction.DESCENDING).limit(1).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if (task.isSuccessful()) {
                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                            int maximum_b = Integer.parseInt(document.get("numb").toString());
                                            int iCard = r.nextInt(maximum_b) + 1;
                                            Map<String,Object> Temporary =  new HashMap<>();
                                            if(NextPlayer>=PlayerNumbers[0])
                                            {
                                                Temporary.put("TurnPlayer",1);
                                            }
                                            else
                                            {
                                                Temporary.put("TurnPlayer",NextPlayer);
                                            }
                                            Temporary.put("isContinued",true);
                                            Temporary.put("BlackCard",iCard);
                                            for(int i=0;i<PlayerNumbers[0];i++)
                                            {
                                                Temporary.put("Players."+Integer.toString(i)+".SelectedCard",0);
                                            }
                                            docRef.update(Temporary);
                                        } }else {
                                        new AlertDialog.Builder(GameActivity.this).setMessage("Valami hiba történt, ellenőrizd az internetkapcsolatot!")
                                                .setPositiveButton("OK", null)
                                                .show();
                                    }
                                }
                            });

                        }
                        else {
                            new AlertDialog.Builder(GameActivity.this).setMessage("Valami hiba történt!")
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
                    }
                });

                ContinueButton.setVisibility(View.GONE);

            }
        });

    }*/

    @Override
    public void onBackPressed() {
/*        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        final String GameId= getIntent().getStringExtra("GAME_ID");
        final int PlayerId = getIntent().getIntExtra("PLAYER_ID",0);
        final DocumentReference docRef = db.collection("Games").document(GameId);

        docRef.update("Players." + Integer.toString(PlayerId), FieldValue.delete());*/




        startActivity(new Intent(GameActivity.this, RoomSelector.class));
        super.onBackPressed();
    }

}