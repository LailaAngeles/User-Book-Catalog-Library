package com.example.parasuratanuser;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class FirestoreSync {

    // Ginagamit natin ‘to para ma-access ang Room database
    private static AppDatabase db;
    //  Ito yung connection natin sa Firebase Firestore
    private static final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    // Gumagamit tayo ng Executor para ‘di ma-block ang main UI thread habang nagsi-sync
    private static final Executor executor = Executors.newSingleThreadExecutor();
    private static final String TAG = "FirestoreSync";
    // Ito yung parang constructor ng class na ‘to, tinatawag sa main para i-setup ang local DB
    public static void init(AppDatabase appDatabase) {
        db = appDatabase;
    }


    // Ito ‘yung function na nagsi-sync ng data ng mga libro galing Firestore papuntang Room (Library)
    public static void syncLibraryFromFirestore(LibraryActivity context, Runnable onComplete) {
        executor.execute(() -> {
            // Kunin lahat ng libro sa local Room DB
            List<LibraryEntity> library = db.libraryDao().getAll();
            // Kung walang ID, skip na agad
            for (LibraryEntity local : library) {
                if (local.id == null || local.id.isEmpty()) continue;

                String firestoreId = local.id;
                // Kunin yung document sa Firestore gamit ang ID
                firestore.collection("Documents")
                        .document(firestoreId)
                        .get()
                        .addOnSuccessListener(snapshot -> {
                            if (snapshot.exists()) {
                                BookDetails b = snapshot.toObject(BookDetails.class);
                                if (b == null) return;
                                // Convert Firestore document papuntang BookDetails object
                                b.setDocumentId(firestoreId);
                                // Convert BookDetails papuntang Room entity
                                LibraryEntity remote = convertBookDetailsToLibraryEntity(b);
                                remote.id = firestoreId;

                                executor.execute(() -> {
                                    // Kung may difference sa local at remote, i-update natin
                                    if (!remote.equals(local)) {
                                        remote.status = local.status;

                                        db.libraryDao().insert(remote);

                                        Log.d(TAG, "♻️ Updated library item with ID: " + firestoreId);

                                        // Show Toast on main thread

                                    } else {
                                        Log.d(TAG, "✅ No changes for library ID: " + firestoreId);
                                    }
                                });

                            } else {
                                // Kung wala na sa Firestore ang doc, i-delete sa Room
                                Log.w(TAG, "⚠️ Firestore doc with ID " + firestoreId + " no longer exists");

                                // Show Toast on main thread
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    Toast.makeText(context, "Admin deleted a book: " + local.title, Toast.LENGTH_SHORT).show();
                                });

                                executor.execute(() -> {
                                    db.libraryDao().delete(local);
                                    Log.d(TAG, "🗑️ Deleted library item with ID: " + firestoreId + " from Room DB");
                                });
                            }
                        })
                        .addOnFailureListener(e -> Log.e(TAG, "❌ Failed to fetch document with ID: " + firestoreId, e));
            }

            if (onComplete != null) onComplete.run();
        });
    }

    //  Convert BookDetails (galing Firestore) to LibraryEntity (para sa Room)
    public static LibraryEntity convertBookDetailsToLibraryEntity(BookDetails b) {
        LibraryEntity entity = new LibraryEntity();
        entity.id = b.getDocumentId();
        entity.author = b.getAuthor();
        entity.title = b.getTitle();
        entity.year = b.getYear();
        entity.publisher = b.getPublisher();
        entity.genre = b.getGenre();
        entity.description = b.getDescription();
        entity.tags = b.getTags();
        entity.imageBase64 = b.getImageBase64();

        return entity;
    }

    // ✅ Sync for Favorites
    public static void syncFavoritesFromFirestore(FavoriteActivity context, Runnable onComplete) {
        executor.execute(() -> {
            List<FavoriteEntity> favorites = db.favoriteDao().getAllFavorites();

            for (FavoriteEntity local : favorites) {
                if (local.id == null || local.id.isEmpty()) continue;

                String firestoreId = local.id;

                firestore.collection("Documents")
                        .document(firestoreId)
                        .get()
                        .addOnSuccessListener(snapshot -> {
                            if (snapshot.exists()) {
                                BookDetails b = snapshot.toObject(BookDetails.class);
                                if (b == null) return;

                                b.setDocumentId(firestoreId);
                                FavoriteEntity remote = convertBookDetailsToFavoriteEntity(b);
                                remote.id = firestoreId;

                                executor.execute(() -> {
                                    if (!remote.equals(local)) {
                                        // Retain the local status
                                        remote.status = local.status;

                                        db.favoriteDao().insert(remote);

                                        Log.d(TAG, "♻️ Updated favorite with ID: " + firestoreId);

                                        // Show Toast on UI thread
                                        new Handler(Looper.getMainLooper()).post(() -> {
                                            Toast.makeText(context, "Admin updated the book: " + b.getTitle(), Toast.LENGTH_SHORT).show();
                                        });
                                    } else {
                                        Log.d(TAG, "✅ No changes for favorite ID: " + firestoreId);
                                    }
                                });

                            } else {
                                Log.w(TAG, "⚠️ Firestore doc with ID " + firestoreId + " no longer exists");

                                // Show Toast on UI thread
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    Toast.makeText(context, "Admin deleted a book: " + local.title, Toast.LENGTH_SHORT).show();
                                });

                                executor.execute(() -> {
                                    db.favoriteDao().delete(local);
                                    Log.d(TAG, "🗑️ Deleted favorite with ID: " + firestoreId + " from Room DB");
                                });
                            }
                        })
                        .addOnFailureListener(e -> Log.e(TAG, "❌ Failed to fetch document with ID: " + firestoreId, e));
            }

            if (onComplete != null) onComplete.run();
        });
    }



    public static FavoriteEntity convertBookDetailsToFavoriteEntity(BookDetails b) {
        FavoriteEntity entity = new FavoriteEntity();
        entity.id = b.getDocumentId();
        entity.author = b.getAuthor();
        entity.title = b.getTitle();
        entity.year = b.getYear();
        entity.publisher = b.getPublisher();
        entity.genre = b.getGenre();
        entity.description = b.getDescription();
        entity.tags = b.getTags();
        entity.imageBase64 = b.getImageBase64();
        return entity;
    }
}
