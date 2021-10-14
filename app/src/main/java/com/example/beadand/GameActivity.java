
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
        Button blackbutton = findViewById(R.id.button1);
        Button w1button = findViewById(R.id.button2);
        Button w2button = findViewById(R.id.button3);
        Button w3button = findViewById(R.id.button4);
        Button w4button = findViewById(R.id.button5);
        Button w5button = findViewById(R.id.button6);
        TextView p1 = findViewById(R.id.player1);
        TextView p2 = findViewById(R.id.player2);
        TextView p3 = findViewById(R.id.player3);
        TextView p4 = findViewById(R.id.player4);
        TextView p5 = findViewById(R.id.player5);
        TextView p6 = findViewById(R.id.player6);
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        final String GameId= getIntent().getStringExtra("GAME_ID");
        final int PlayerId = getIntent().getIntExtra("PLAYER_ID",0);
        final int[] PlayerNumbers = {0};
        final Random r = new Random();
        TextView[] playertexts = {p1,p2,p3,p4,p5,p6};
        Button[] buttons = {w1button,w2button,w3button,w4button,w5button};

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
                            playertexts[i].setText(Player.get("PlayerName").toString() + ": " + Player.get("PlayerScore"));
                            PlayerTextViewIds[0][i - 1] = playertexts[i].getId();
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
                                                                                    blackbutton.setText(document.getString("element"));
                                                                                    blackbutton.setVisibility(View.VISIBLE);
                                                                                    for(int i = 0; i<PlayerNumbers[0]-2;i++)
                                                                                    {
                                                                                        buttons[i].setText(SelectedWhiteCards.get(i));
                                                                                        buttons[i].setVisibility(View.VISIBLE);
                                                                                    }
                                                                                    w1button.setOnClickListener(new View.OnClickListener(){
                                                                                        @Override
                                                                                        public void onClick(View v) {
                                                                                            CzarButton(0, Players, SelectedCardsQue, docRef);
                                                                                            hideButtons(buttons);
                                                                                            blackbutton.setVisibility(View.GONE);
                                                                                        }
                                                                                    });
                                                                                    w2button.setOnClickListener(new View.OnClickListener(){
                                                                                        @Override
                                                                                        public void onClick(View v) {
                                                                                            CzarButton(1, Players, SelectedCardsQue, docRef);
                                                                                            hideButtons(buttons);
                                                                                            blackbutton.setVisibility(View.GONE);
                                                                                        }
                                                                                    });
                                                                                    w3button.setOnClickListener(new View.OnClickListener(){
                                                                                        @Override
                                                                                        public void onClick(View v) {
                                                                                            CzarButton(2, Players, SelectedCardsQue, docRef);
                                                                                            hideButtons(buttons);
                                                                                            blackbutton.setVisibility(View.GONE);
                                                                                        }
                                                                                    });
                                                                                    w4button.setOnClickListener(new View.OnClickListener(){
                                                                                        @Override
                                                                                        public void onClick(View v) {
                                                                                            CzarButton(3, Players, SelectedCardsQue, docRef);
                                                                                            hideButtons(buttons);
                                                                                            blackbutton.setVisibility(View.GONE);
                                                                                        }
                                                                                    });
                                                                                    w5button.setOnClickListener(new View.OnClickListener(){
                                                                                        @Override
                                                                                        public void onClick(View v) {
                                                                                            CzarButton(4, Players, SelectedCardsQue, docRef);
                                                                                            hideButtons(buttons);
                                                                                            blackbutton.setVisibility(View.GONE);
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
                                                                                        blackbutton.setText(document.getString("element"));
                                                                                        blackbutton.setVisibility(View.VISIBLE);
                                                                                        for(int i = 0; i<5;i++)
                                                                                        {
                                                                                            buttons[i].setText(s[i]);
                                                                                            buttons[i].setVisibility(View.VISIBLE);
                                                                                        }
                                                                                        w1button.setOnClickListener(new View.OnClickListener(){
                                                                                            @Override
                                                                                            public void onClick(View v) {
                                                                                                SelectPlayCard(0, PlayerId, SelectedCardsQue, docRef);
                                                                                                hideButtons(buttons);
                                                                                                blackbutton.setVisibility(View.GONE);
                                                                                            }
                                                                                        });
                                                                                        w2button.setOnClickListener(new View.OnClickListener(){
                                                                                            @Override
                                                                                            public void onClick(View v) {
                                                                                                SelectPlayCard(1, PlayerId, SelectedCardsQue, docRef);
                                                                                                hideButtons(buttons);
                                                                                                blackbutton.setVisibility(View.GONE);
                                                                                            }
                                                                                        });
                                                                                        w3button.setOnClickListener(new View.OnClickListener(){
                                                                                            @Override
                                                                                            public void onClick(View v) {
                                                                                                SelectPlayCard(2, PlayerId, SelectedCardsQue, docRef);
                                                                                                hideButtons(buttons);
                                                                                                blackbutton.setVisibility(View.GONE);
                                                                                            }
                                                                                        });
                                                                                        w4button.setOnClickListener(new View.OnClickListener(){
                                                                                            @Override
                                                                                            public void onClick(View v) {
                                                                                                SelectPlayCard(3, PlayerId, SelectedCardsQue, docRef);
                                                                                                hideButtons(buttons);
                                                                                                blackbutton.setVisibility(View.GONE);
                                                                                            }
                                                                                        });
                                                                                        w5button.setOnClickListener(new View.OnClickListener(){
                                                                                            @Override
                                                                                            public void onClick(View v) {
                                                                                                SelectPlayCard(4, PlayerId, SelectedCardsQue, docRef);
                                                                                                hideButtons(buttons);
                                                                                                blackbutton.setVisibility(View.GONE);
                                                                                            }
                                                                                        });

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
                                                        //List<Integer> AddedCards = new ArrayList<Integer>();

                                                        for (int i = 0; i < PlayerNumbers[0] - 1; i++) {
                                                            Integer[] Cards = { 0,0,0,0,0 };
                                                            for (int y=0;y<5;y++) {
                                                                int iCard_w = r.nextInt(maximum_w) + 1;
                                                                if (y==0) {
                                                                    Cards[y] = iCard_w;
                                                                }else
                                                                {
                                                                    if(!Arrays.asList(Cards).contains(iCard_w))
                                                                    {
                                                                        Cards[y] = iCard_w;
                                                                    }else {
                                                                        y--;
                                                                    }
                                                                }
                                                                if (y == 4)
                                                                {
                                                                       docRef.update("Players." + Integer.toString(i + 1) + ".Cards", Arrays.asList(Cards));
                                                                        Log.d("cards", Arrays.asList(Cards).toString());
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

    public void CzarButton(int selectedPosition, Map<String, Object> Players, List<Integer> SelectedCardsQue, DocumentReference docRef){
        Map<String, Object> Temporary = new HashMap<>();
        Map<String, Object> Player = (Map<String, Object>) Players.get(Integer.toString(SelectedCardsQue.get(selectedPosition)));
        Temporary.put("Players." + Integer.toString(SelectedCardsQue.get(selectedPosition)) + ".PlayerScore", ((long) Player.get("PlayerScore") + 1));
        Temporary.put("isTurnEnded", false);
        docRef.update(Temporary);
    }
    public void SelectPlayCard(int SelectedPosition, int PlayerId, List<Integer> SelectedCardsQue, DocumentReference docRef)
    {
        int selectedPosition = SelectedPosition;
        docRef.update("Players." + Integer.toString(PlayerId) + ".SelectedCard", SelectedCardsQue.get(selectedPosition));
    }
    public void hideButtons(Button[] buttons)
    {
        for(int i = 0; i<buttons.length;i++)
        {
            buttons[i].setVisibility(View.INVISIBLE);
        }
    }
}