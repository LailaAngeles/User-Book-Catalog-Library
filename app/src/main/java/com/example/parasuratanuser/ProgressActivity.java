// package name natin, parang folder kung saan nakaorganize yung code
package com.example.parasuratanuser;

// ginagamit para sa animation ng kulay
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
// para sa kulay at style ng progress bar
import android.graphics.Color;
import android.graphics.PorterDuff;
// para sa Android lifecycle method tulad ng onCreate
import android.os.Bundle;
// UI elements na progress bar at text
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

// para makagamit ng AppCompatActivity (basic activity na may support features)
import androidx.appcompat.app.AppCompatActivity;
// Room database para sa local data
import androidx.room.Room;

// kuha tayo ng data mula sa cloud gamit Firestore
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

// utilities para sa pag-store ng data
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProgressActivity extends AppCompatActivity {
    private ProgressBar loadingIndicator;
    private FirebaseFirestore firestore;  // dito kukunin yung data ng books mula sa Firebase (cloud)
    private AppDatabase roomDb;  // local database gamit Room (pang Library at Favorites)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);  // tawagin parent method
        setContentView(R.layout.progresslayout);  // set natin yung UI layout na gagamitin

        firestore = FirebaseFirestore.getInstance();  // initialize si firestore para makuha natin data mula cloud
        roomDb    = Room.databaseBuilder(this, AppDatabase.class, "parasuratan.db").build();  // setup ng local database

        findViewById(R.id.backButton).setOnClickListener(v -> finish());  // kapag pinindot yung back button, babalik sa previous screen
        loadingIndicator = findViewById(R.id.loadingIndicator);
        fetchBookDataAndCalculateProgress();  // simulan agad ang pagkuha ng data at compute ng progress
    }

    @Override
    protected void onResume() {
        super.onResume();  // tawagin yung default behavior
        fetchBookDataAndCalculateProgress();  // i-refresh yung progress data kapag balik sa screen
    }

    // kuha ng data sa Firestore at icocompute yung progress base sa genre
    private void fetchBookDataAndCalculateProgress() {
        loadingIndicator.setVisibility(View.VISIBLE);
        firestore.collection("Documents").get().addOnSuccessListener(querySnapshots -> {
            Map<String, Integer> genreTotalMap = new HashMap<>();  // para bilangin kung ilan ang books kada genre
            int totalBooks = 0;  // total ng lahat ng books sa Firestore

            // loop sa lahat ng nakuha nating documents (books)
            for (DocumentSnapshot doc : querySnapshots) {
                String genre = mapGenre(doc.getString("genre"));  // ayusin ang genre name para uniform

                if (genre == null) continue;  // kung walang genre, i-skip

                // dagdagan yung bilang ng genre na yun
                genreTotalMap.put(genre, genreTotalMap.getOrDefault(genre, 0) + 1);
                totalBooks++;  // dagdagan ang total books
            }

            // compute na yung progress base sa data natin sa Room database
            calculateProgressWithRoom(genreTotalMap, totalBooks);
        });
    }

    // convert natin status (tulad ng "Reading") sa numeric value para macompute
    private float getStatusScore(String status) {
        if (status == null) return 0f;  // kung walang status, score ay 0
        String s = status.trim().toLowerCase();  // tanggal spaces at gawing lowercase
        switch (s) {
            case "read":
            case "done reading":
            case "completed":
                return 1f;  // kung tapos na basahin, full score
            case "reading":
                return 0.5f;  // kung binabasa pa lang, kalahating score
            default:
                return 0f;  // ibang status, wala tayong score
        }
    }

    // dito na kinocompute yung progress ng bawat genre at total progress
    private void calculateProgressWithRoom(Map<String, Integer> genreTotalMap, int totalBooksFromFirestore) {
        new Thread(() -> {  // background thread para di mablock yung UI
            List<LibraryEntity> libraryBooks = roomDb.libraryDao().getAll();  // kunin lahat ng books sa Library
            List<FavoriteEntity> favoriteBooks = roomDb.favoriteDao().getAllFavorites();  // kunin lahat ng books sa Favorites

            Map<String, Float> progressMap = new HashMap<>();  // dito ilalagay ang progress per unique book

            // process natin lahat ng nasa Library
            for (LibraryEntity book : libraryBooks) {
                float score = getStatusScore(book.status);  // kuha status score
                if (score > 0f) {
                    String key = generateBookKey(book.title, book.author, book.genre);  // unique key para sa book
                    progressMap.put(key, Math.max(progressMap.getOrDefault(key, 0f), score));  // kunin yung pinakamataas na score
                    //pero ung book naman is only counted once and same id lang naman so walang duplication
                }
            }

            // process din natin lahat ng nasa Favorites
            for (FavoriteEntity fav : favoriteBooks) {
                float score = getStatusScore(fav.status);
                if (score > 0f) {
                    String key = generateBookKey(fav.title, fav.author, fav.genre);
                    progressMap.put(key, Math.max(progressMap.getOrDefault(key, 0f), score));
                }
            }

            // setup ng mga counters per genre at total
            Map<String, Float> genreProgress = new HashMap<>();  // para sa total progress kada genre
            Map<String, Integer> genreCompletedCount = new HashMap<>();  // bilang ng tapos na kada genre
            final float[] totalProgress = {0f};  // total progress overall
            final int[] totalCompletedCount = {0};  // total na tapos na books

            // kuha ulit tayo ng data sa Firestore para i-match
            firestore.collection("Documents").get().addOnSuccessListener(querySnapshots -> {
                for (DocumentSnapshot doc : querySnapshots) {
                    String title = doc.getString("title");
                    String author = doc.getString("author");
                    String genre = doc.getString("genre");

                    if (title == null || author == null || genre == null) continue;  // skip kung kulang data

                    String key = generateBookKey(title, author, genre);  // gawa ulit ng key para ma-match
                    float progress = progressMap.getOrDefault(key, 0f);  // kuha progress kung meron

                    if (progress > 0f) {
                        totalProgress[0] += progress;  // dagdag sa overall progress
                        String mappedGenre = mapGenre(genre);  // ayusin ang genre name

                        genreProgress.put(mappedGenre, genreProgress.getOrDefault(mappedGenre, 0f) + progress);  // dagdag sa genre progress

                        if (progress == 1f) {  // kung fully completed
                            genreCompletedCount.put(mappedGenre,
                                    genreCompletedCount.getOrDefault(mappedGenre, 0) + 1);
                            totalCompletedCount[0]++;  // dagdag sa total completed
                        }
                    }
                }

                // balik tayo sa main thread para i-update UI
                runOnUiThread(() -> {
                    // i-update bawat genre progress bar
                    updateProgressBar(R.id.fantasyProgress, R.id.fantasyPercentage, R.id.fantasyCount,
                            genreProgress.getOrDefault("Fantasy/Adventure", 0f),
                            genreCompletedCount.getOrDefault("Fantasy/Adventure", 0),
                            genreTotalMap.getOrDefault("Fantasy/Adventure", 0));



                    updateProgressBar(R.id.mythologyProgress, R.id.mythologyPercentage, R.id.mythologyCount,
                            genreProgress.getOrDefault("Mythology/Folklore", 0f),
                            genreCompletedCount.getOrDefault("Mythology/Folklore", 0),
                            genreTotalMap.getOrDefault("Mythology/Folklore", 0));

                    updateProgressBar(R.id.educationalProgress, R.id.educationalPercentage, R.id.educationalCount,
                            genreProgress.getOrDefault("Educational", 0f),
                            genreCompletedCount.getOrDefault("Educational", 0),
                            genreTotalMap.getOrDefault("Educational", 0));

                    updateProgressBar(R.id.realisticProgress, R.id.realisticPercentage, R.id.realisticCount,
                            genreProgress.getOrDefault("Realistic Fiction", 0f),
                            genreCompletedCount.getOrDefault("Realistic Fiction", 0),
                            genreTotalMap.getOrDefault("Realistic Fiction", 0));

                    updateProgressBar(R.id.unknownProgress, R.id.unknownPercentage, R.id.unknownCount,
                            genreProgress.getOrDefault("Unknown Genre", 0f),
                            genreCompletedCount.getOrDefault("Unknown Genre", 0),
                            genreTotalMap.getOrDefault("Unknown Genre", 0));

                    updateProgressBar(R.id.overallProgress, R.id.overallPercentage, R.id.overallCount,
                            totalProgress[0], totalCompletedCount[0], totalBooksFromFirestore);
                    loadingIndicator.setVisibility(View.GONE);
                });
            });

        }).start();
    }

    // gumagawa ng unique key para sa bawat book gamit title + author + genre
    private String generateBookKey(String title, String author, String genre) {
        return normalize(title) + "|" + normalize(author) + "|" + normalize(genre);
    }

    // ayusin string para walang sobrang space at uniform ang format
    private String normalize(String input) {
        return input == null ? "" : input.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    // ayusin genre para pare-pareho ang format (e.g. "fantasy" → "Fantasy/Adventure")
    private String mapGenre(String genre) {
        if (genre == null) return "Unknown Genre";
        genre = genre.trim().toLowerCase();
        if (genre.contains("fantasy")) return "Fantasy/Adventure";
        if (genre.contains("history")) return "Historical";
        if (genre.contains("myth") || genre.contains("folk")) return "Mythology/Folklore";
        if (genre.contains("educat")) return "Educational";
        if (genre.contains("realistic")) return "Realistic Fiction";
        return "Unknown Genre";  // default kung di tugma
    }

    // update ng progress bar at percentage sa UI
    private void updateProgressBar(int progressBarId,
                                   int percentageTextId,
                                   int countTextId,
                                   float readProgress,
                                   int completedCount,
                                   int totalCount) {

        ProgressBar progressBar = findViewById(progressBarId);  // hanapin progress bar
        TextView percentage = findViewById(percentageTextId);  // hanapin percentage text
        TextView count = findViewById(countTextId);  // hanapin count text

        int progressPercent = (totalCount == 0) ? 0 : Math.round((readProgress * 100f) / totalCount);  // compute percentage
        //percentage = (totalReadProgress / totalAvailableBooks) * 100
        
        // animate yung paglipat ng progress value
        ObjectAnimator.ofInt(progressBar, "progress",
                        progressBar.getProgress(), progressPercent)
                .setDuration(500)
                .start();

        // baguhin kulay depende sa progress value
        int color = getColorForProgress(progressPercent);
        progressBar.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);

        percentage.setText(progressPercent + "%");  // display percentage
        count.setText(completedCount + " / " + totalCount + " Completed");  // display bilang ng natapos
    }

    // kunin kulay depende sa progress (red → yellow → green)
    private int getColorForProgress(int progress) {
        if (progress <= 50) {
            return (Integer) new ArgbEvaluator().evaluate(
                    progress / 50f, Color.RED, Color.YELLOW);
        } else {
            return (Integer) new ArgbEvaluator().evaluate(
                    (progress - 50) / 50f, Color.YELLOW, Color.GREEN);
        }
    }
    /**
     * Red to Yellow from 0% to 50%
     * Yellow to Green from 51% to 100%
     */

}