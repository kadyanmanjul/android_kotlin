package com.joshtalks.joshskills.conversationRoom.liveRooms;

public class LiveRoomUser {

    private String name;
    private boolean is_speaker;
    private boolean is_moderator;
    private boolean is_hand_raised;
    private boolean is_mic_on;
    private String photo_url;

    public LiveRoomUser(){

    }

    public LiveRoomUser(String name, boolean is_speaker, boolean is_moderator, boolean is_hand_raised, boolean is_mic_on, String photo_url) {
        this.name = name;
        this.is_speaker = is_speaker;
        this.is_moderator = is_moderator;
        this.is_hand_raised = is_hand_raised;
        this.is_mic_on = is_mic_on;
        this.photo_url = photo_url;
    }

    public String getName() {
        return name;
    }

    public boolean isIs_speaker() {
        return is_speaker;
    }

    public boolean isIs_moderator() {
        return is_moderator;
    }

    public boolean isIs_hand_raised() {
        return is_hand_raised;
    }

    public boolean isIs_mic_on() {
        return is_mic_on;
    }

    public String getPhoto_url() {
        return photo_url;
    }
}
