package com.example.chat_application;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_MESSAGE_SENT = 0;
    private static final int TYPE_MESSAGE_RECEIVED = 1;
    private static final int TYPE_IMAGE_SENT = 2;
    private static final int TYPE_IMAGE_RECEIVED = 3;
    private static final int TYPE_AUDIO_SENT = 4;
    private static final int TYPE_AUDIO_RECEIVED = 5;
    private static final int TYPE_VIDEO_SENT = 6;
    private static final int TYPE_VIDEO_RECEIVED = 7;
    private static final int TYPE_GIF_SENT = 8;
    private static final int TYPE_GIF_RECEIVED = 9;

    private final LayoutInflater inflater;
    private final Context context;
    private final List<JSONObject> messages = new ArrayList<>();
    private final List<JSONObject> allMessages = new ArrayList<>();

    public MessageAdapter(LayoutInflater inflater) {
        this.inflater = inflater;
        this.context = inflater.getContext();
    }

    private class SentMessageHolder extends RecyclerView.ViewHolder {
        TextView messageTxt,timestampTxt,readStatusTxt;
        SentMessageHolder(@NonNull View itemView) {
            super(itemView);
            messageTxt = itemView.findViewById(R.id.sentTxt);
            timestampTxt = itemView.findViewById(R.id.sentTimestamp);
            readStatusTxt = itemView.findViewById(R.id.readStatus);

        }
    }

    private class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        TextView nameTxt, messageTxt,timestampTxt;
        ReceivedMessageHolder(@NonNull View itemView) {
            super(itemView);
            nameTxt = itemView.findViewById(R.id.nameTxt);
            messageTxt = itemView.findViewById(R.id.receivedTxt);
            timestampTxt = itemView.findViewById(R.id.receivedTimestamp); // ← add this
        }
    }

    private class SentImageHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView timestampTxt;
        SentImageHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            timestampTxt = itemView.findViewById(R.id.sentTimestamp);
        }
    }

    private class ReceivedImageHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView nameTxt,timestampTxt;
        ReceivedImageHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            nameTxt = itemView.findViewById(R.id.nameTxt);
            timestampTxt = itemView.findViewById(R.id.receivedTimestamp); // ← add this
        }
    }

    private class SentAudioHolder extends RecyclerView.ViewHolder {
        ImageView playIcon;
        TextView timestampTxt;
        SentAudioHolder(@NonNull View itemView) {
            super(itemView);
            playIcon = itemView.findViewById(R.id.playAudioBtn);
            timestampTxt = itemView.findViewById(R.id.sentTimestamp);
        }
    }

    private class ReceivedAudioHolder extends RecyclerView.ViewHolder {
        TextView nameTxt,timestampTxt;
        ImageView playIcon;
        ReceivedAudioHolder(@NonNull View itemView) {
            super(itemView);
            nameTxt = itemView.findViewById(R.id.nameTxt);
            playIcon = itemView.findViewById(R.id.playAudioBtn);
            timestampTxt = itemView.findViewById(R.id.receivedTimestamp); // ← add this
        }
    }

    private class SentVideoHolder extends RecyclerView.ViewHolder {
        VideoView videoView;
        TextView timestampTxt;
        ImageView playBtnOverlay;
        SentVideoHolder(@NonNull View itemView) {
            super(itemView);
            videoView = itemView.findViewById(R.id.videoView);
            timestampTxt = itemView.findViewById(R.id.sentTimestamp);
            playBtnOverlay=itemView.findViewById(R.id.playBtnOverlay);
        }
    }

    private class ReceivedVideoHolder extends RecyclerView.ViewHolder {
        TextView nameTxt,timestampTxt;
        VideoView videoView;
        ImageView playBtnOverlay;
        ReceivedVideoHolder(@NonNull View itemView) {
            super(itemView);
            nameTxt = itemView.findViewById(R.id.nameTxt);
            videoView = itemView.findViewById(R.id.videoView);
            playBtnOverlay=itemView.findViewById(R.id.playBtnOverlay);
            timestampTxt = itemView.findViewById(R.id.receivedTimestamp); // ← add this
        }
    }


    private class SentGifHolder extends RecyclerView.ViewHolder {
        GifImageView gifImageView;
        TextView timestampTxt;
        SentGifHolder(@NonNull View itemView) {
            super(itemView);
            gifImageView = itemView.findViewById(R.id.gifImageView);
            timestampTxt = itemView.findViewById(R.id.sentTimestamp);
        }
    }

    private class ReceivedGifHolder extends RecyclerView.ViewHolder {
        TextView nameTxt,timestampTxt;
        GifImageView gifImageView;
        ReceivedGifHolder(@NonNull View itemView) {
            super(itemView);
            nameTxt = itemView.findViewById(R.id.nameTxt);
            gifImageView = itemView.findViewById(R.id.gifImageView);
            timestampTxt = itemView.findViewById(R.id.receivedTimestamp); // ← add this
        }
    }



    @Override
    public int getItemViewType(int position) {
        JSONObject msg = messages.get(position);
        try {
            boolean sent = msg.getBoolean("isSent");
            if (sent) {
                if (msg.has("message")) return TYPE_MESSAGE_SENT;
                if (msg.has("image")) return TYPE_IMAGE_SENT;
                if (msg.has("audio")) return TYPE_AUDIO_SENT;
                if (msg.has("video")) return TYPE_VIDEO_SENT;
                if (msg.has("gif")) return TYPE_GIF_SENT;
            } else {
                if (msg.has("message")) return TYPE_MESSAGE_RECEIVED;
                if (msg.has("image")) return TYPE_IMAGE_RECEIVED;
                if (msg.has("audio")) return TYPE_AUDIO_RECEIVED;
                if (msg.has("video")) return TYPE_VIDEO_RECEIVED;
                if (msg.has("gif")) return TYPE_GIF_RECEIVED;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        int layoutId = switch (type) {
            case TYPE_MESSAGE_SENT -> R.layout.sent_message;
            case TYPE_MESSAGE_RECEIVED -> R.layout.received_message;
            case TYPE_IMAGE_SENT -> R.layout.sent_image;
            case TYPE_IMAGE_RECEIVED -> R.layout.received_image;
            case TYPE_AUDIO_SENT -> R.layout.sent_audio;
            case TYPE_AUDIO_RECEIVED -> R.layout.received_audio;
            case TYPE_VIDEO_SENT -> R.layout.sent_video;
            case TYPE_VIDEO_RECEIVED -> R.layout.received_video;
            case TYPE_GIF_SENT -> R.layout.sent_gif;
            case TYPE_GIF_RECEIVED -> R.layout.received_gif;
            default -> throw new IllegalArgumentException("Invalid view type");
        };
        View view = inflater.inflate(layoutId, parent, false);

        return switch (type) {
            case TYPE_MESSAGE_SENT -> new SentMessageHolder(view);
            case TYPE_MESSAGE_RECEIVED -> new ReceivedMessageHolder(view);
            case TYPE_IMAGE_SENT -> new SentImageHolder(view);
            case TYPE_IMAGE_RECEIVED -> new ReceivedImageHolder(view);
            case TYPE_AUDIO_SENT -> new SentAudioHolder(view);
            case TYPE_AUDIO_RECEIVED -> new ReceivedAudioHolder(view);
            case TYPE_VIDEO_SENT -> new SentVideoHolder(view);
            case TYPE_VIDEO_RECEIVED -> new ReceivedVideoHolder(view);
            case TYPE_GIF_SENT -> new SentGifHolder(view);
            case TYPE_GIF_RECEIVED -> new ReceivedGifHolder(view);
            default -> throw new IllegalStateException();
        };
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        JSONObject message = messages.get(position);
        try {
            boolean isSent = message.getBoolean("isSent");
            long timestamp = message.optLong("timestamp", 0);
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getDefault());
            String formattedTime = sdf.format(new Date(timestamp));
            // Text
            if (message.has("message")) {
                if (isSent) {
                    ((SentMessageHolder) holder).messageTxt.setText(message.getString("message"));
                    ((SentMessageHolder) holder).timestampTxt.setText(formattedTime);
                    boolean isRead = message.optBoolean("read", false);
                    ((SentMessageHolder) holder).readStatusTxt.setText(isRead ? "✓✓ Read" : "✓ Sent");
                    ((SentMessageHolder) holder).readStatusTxt.setTextColor(isRead ? Color.BLUE : Color.GRAY);

                } else {
                    ((ReceivedMessageHolder) holder).nameTxt.setText(message.getString("name"));
                    ((ReceivedMessageHolder) holder).messageTxt.setText(message.getString("message"));
                    ((ReceivedMessageHolder) holder).timestampTxt.setText(formattedTime);
                }
                return;
            }


            // Image
            if (message.has("image")) {
                Bitmap bitmap = getBitmapFromString(message.getString("image"));
                if (isSent){
                    ((SentImageHolder) holder).imageView.setImageBitmap(bitmap);
                    ((SentImageHolder) holder).timestampTxt.setText(formattedTime);
                }
                else {
                    ((ReceivedImageHolder) holder).nameTxt.setText(message.getString("name"));
                    ((ReceivedImageHolder) holder).imageView.setImageBitmap(bitmap);
                    ((ReceivedImageHolder) holder).timestampTxt.setText(formattedTime);
                }
                return;
            }

            // Audio
            if (message.has("audio")) {
                ImageButton playBtn = (ImageButton) (isSent ? ((SentAudioHolder) holder).playIcon : ((ReceivedAudioHolder) holder).playIcon);

                if (isSent) {
                    ((SentAudioHolder) holder).timestampTxt.setText(formattedTime);
                } else {
                    ((ReceivedAudioHolder) holder).nameTxt.setText(message.getString("name"));
                    ((ReceivedAudioHolder) holder).timestampTxt.setText(formattedTime);
                }

                playBtn.setImageResource(R.drawable.baseline_play_arrow_24); // Reset button on bind

                playBtn.setOnClickListener(v -> {
                    try {
                        playOrPauseAudio(message.getString("audio"), playBtn);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                });
                return;
            }



            if (message.has("video")) {
                String path = decodeToTempFile(message.getString("video"), ".mp4");

                if (isSent) {
                    SentVideoHolder vh = (SentVideoHolder) holder;
                    vh.videoView.setVideoPath(path);
                    vh.timestampTxt.setText(formattedTime);
                    vh.videoView.seekTo(1);

                    // Initial state: show play button
                    vh.playBtnOverlay.setImageResource(R.drawable.baseline_play_circle_24);
                    vh.playBtnOverlay.setVisibility(View.VISIBLE);

                    vh.playBtnOverlay.setOnClickListener(v -> {
                        if (vh.videoView.isPlaying()) {
                            vh.videoView.pause();
                            vh.playBtnOverlay.setImageResource(R.drawable.baseline_play_circle_24);
                            vh.playBtnOverlay.setVisibility(View.VISIBLE);
                        } else {
                            vh.videoView.start();
                            vh.playBtnOverlay.setImageResource(R.drawable.baseline_pause_black);
                            // Optionally hide button during playback or leave it visible
                        }
                    });

                    // Optional: reset play icon when video ends
                    vh.videoView.setOnCompletionListener(mp -> {
                        vh.playBtnOverlay.setImageResource(R.drawable.baseline_play_circle_24);
                        vh.playBtnOverlay.setVisibility(View.VISIBLE);
                    });

                } else {
                    ReceivedVideoHolder vh = (ReceivedVideoHolder) holder;
                    vh.nameTxt.setText(message.getString("name"));
                    vh.videoView.setVideoPath(path);
                    vh.timestampTxt.setText(formattedTime);
                    vh.videoView.seekTo(1);

                    vh.playBtnOverlay.setImageResource(R.drawable.baseline_play_circle_24);
                    vh.playBtnOverlay.setVisibility(View.VISIBLE);

                    vh.playBtnOverlay.setOnClickListener(v -> {
                        if (vh.videoView.isPlaying()) {
                            vh.videoView.pause();
                            vh.playBtnOverlay.setImageResource(R.drawable.baseline_play_circle_24);
                            vh.playBtnOverlay.setVisibility(View.VISIBLE);
                        } else {
                            vh.videoView.start();
                            vh.playBtnOverlay.setImageResource(R.drawable.baseline_pause_black);
                        }
                    });

                    vh.videoView.setOnCompletionListener(mp -> {
                        vh.playBtnOverlay.setImageResource(R.drawable.baseline_play_circle_24);
                        vh.playBtnOverlay.setVisibility(View.VISIBLE);
                    });
                }
                return;
            }


            byte[] gifBytes = Base64.decode(message.getString("gif"), Base64.DEFAULT);
            GifDrawable gifDrawable = new GifDrawable(gifBytes);
            gifDrawable.setLoopCount(0); // loop forever

            if (isSent) {
                ((SentGifHolder) holder).gifImageView.setImageDrawable(gifDrawable);
                ((SentGifHolder) holder).timestampTxt.setText(formattedTime);
            } else {
                ((ReceivedGifHolder) holder).nameTxt.setText(message.getString("name"));
                ((ReceivedGifHolder) holder).gifImageView.setImageDrawable(gifDrawable);
                ((ReceivedGifHolder) holder).timestampTxt.setText(formattedTime);

            }

            gifDrawable.start();


        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Bitmap getBitmapFromString(String base64) {
        byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    private MediaPlayer mediaPlayer;
    private ImageButton currentPlayButton = null;

    private void playOrPauseAudio(String base64, ImageButton playButton) {
        try {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                if (currentPlayButton == playButton) {
                    // Pause if same button
                    mediaPlayer.pause();
                    playButton.setImageResource(R.drawable.baseline_play_arrow_24);
                } else {
                    // Stop old playback and start new
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    currentPlayButton.setImageResource(R.drawable.baseline_play_arrow_24);
                    mediaPlayer = null;
                    startNewAudio(base64, playButton);
                }
            } else if (mediaPlayer != null) {
                // Resume paused playback
                mediaPlayer.start();
                playButton.setImageResource(R.drawable.baseline_pause_24);
                currentPlayButton = playButton;
            } else {
                // First time play
                startNewAudio(base64, playButton);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startNewAudio(String base64, ImageButton playButton) {
        try {
            byte[] data = Base64.decode(base64, Base64.DEFAULT);
            File temp = File.createTempFile("audio_", ".mp3", context.getCacheDir());
            try (FileOutputStream fos = new FileOutputStream(temp)) {
                fos.write(data);
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(temp.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();

            playButton.setImageResource(R.drawable.baseline_pause_24);
            currentPlayButton = playButton;

            mediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                mediaPlayer = null;
                if (currentPlayButton != null) {
                    currentPlayButton.setImageResource(R.drawable.baseline_play_arrow_24);
                    currentPlayButton = null;
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private String decodeToTempFile(String base64, String extension) {
        try {
            byte[] data = Base64.decode(base64, Base64.DEFAULT);
            File temp = File.createTempFile("media_", extension, context.getCacheDir());
            try (FileOutputStream fos = new FileOutputStream(temp)) {
                fos.write(data);
            }
            return temp.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void addItem(JSONObject jsonObject) {
        messages.add(jsonObject);
        allMessages.add(jsonObject);
        notifyDataSetChanged();
    }

    public void filter(String text) {
        messages.clear();
        if (text.isEmpty()) {
            messages.addAll(allMessages);
        } else {
            for (JSONObject msg : allMessages) {
                try {
                    if (msg.has("message") && msg.getString("message").toLowerCase().contains(text.toLowerCase())) {
                        messages.add(msg);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        notifyDataSetChanged();
    }

}
