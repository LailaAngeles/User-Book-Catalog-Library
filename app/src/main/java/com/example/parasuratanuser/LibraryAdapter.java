package com.example.parasuratanuser;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;


public class LibraryAdapter extends RecyclerView.Adapter<LibraryAdapter.BookViewHolder> {

    private final List<BookDetails> bookList;

    public LibraryAdapter(List<BookDetails> bookList) {
        this.bookList = bookList;
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.book_smallstory_adaptor, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        BookDetails book = bookList.get(position);

        holder.titleTextView.setText(book.getTitle());

        // --- cover image ---
        if (book.hasImage()) {
            try {
                byte[] decodedBytes = Base64.decode(book.getImageBase64(), Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                holder.imageView.setImageBitmap(bitmap != null
                        ? bitmap
                        : BitmapFactory.decodeResource(holder.imageView.getResources(), R.drawable.placeholder_image));
            } catch (Exception e) {
                holder.imageView.setImageResource(R.drawable.placeholder_image);
            }
        } else {
            holder.imageView.setImageResource(R.drawable.placeholder_image);
        }

        // --- navigate to BookDetails ---
        holder.itemView.setOnClickListener(v -> {
            Context context = holder.itemView.getContext();

            // Cache the image so the detail screen can load instantly
            SharedPreferences prefs = context.getSharedPreferences("Images", Context.MODE_PRIVATE);
            prefs.edit().putString(book.getTitle(), book.getImageBase64()).apply();

            Intent intent = new Intent(context, BookDetailsActivity.class);
            intent.putExtra("documentId", book.getDocumentId());
            intent.putExtra("title", book.getTitle());
            intent.putExtra("author", book.getAuthor());
            intent.putExtra("year", book.getYear());
            intent.putExtra("publisher", book.getPublisher());
            intent.putExtra("genre", book.getGenre());
            intent.putExtra("description", book.getDescription());
            intent.putExtra("tags", book.getTags());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return bookList.size(); }

    static class BookViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView titleTextView;
        BookViewHolder(View itemView) {
            super(itemView);
            imageView      = itemView.findViewById(R.id.book_image);
            titleTextView  = itemView.findViewById(R.id.book_title);
        }
    }
}

