package com.cleanyourtv.launcher;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AppTileAdapter extends RecyclerView.Adapter<AppTileAdapter.TileViewHolder> {

    public interface OnAppFocusListener {
        void onAppFocused(AppEntry app);
    }

    private final Context context;
    private final List<AppEntry> apps;
    private final OnAppFocusListener focusListener;

    public AppTileAdapter(Context context, List<AppEntry> apps, OnAppFocusListener focusListener) {
        this.context = context;
        this.apps = apps;
        this.focusListener = focusListener;
    }

    @NonNull
    @Override
    public TileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_app_tile, parent, false);
        return new TileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TileViewHolder holder, int position) {
        AppEntry app = apps.get(position);
        holder.ivIcon.setImageDrawable(app.icon);
        holder.tvLabel.setText(app.label);

        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                v.animate().scaleX(1.12f).scaleY(1.12f).setDuration(150).start();
                holder.tvLabel.setVisibility(View.VISIBLE);
                if (focusListener != null) {
                    focusListener.onAppFocused(app);
                }
            } else {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
                holder.tvLabel.setVisibility(View.INVISIBLE);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (app.launchIntent != null) {
                try {
                    context.startActivity(app.launchIntent);
                } catch (Exception ignored) {
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    static class TileViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvLabel;

        TileViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_app_icon);
            tvLabel = itemView.findViewById(R.id.tv_app_label);
        }
    }
}
