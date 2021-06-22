package com.joshtalks.joshskills.conversationRoom.liveRooms;

public class LiveRoomUser {

    private String name;
    private boolean is_speaker;
    private boolean is_moderator;
    private boolean is_hand_raised;
    private boolean is_mic_on;
    private String photo_url;
    private String mentor_id;
    private boolean is_speaking;
    private int sort_order = 0;

    public LiveRoomUser(){

    }

    public LiveRoomUser(String name, boolean is_speaker, boolean is_moderator, boolean is_hand_raised, boolean is_mic_on, String photo_url,
                        String mentor_id, boolean is_speaking, int sort_order) {
        this.name = name;
        this.is_speaker = is_speaker;
        this.is_moderator = is_moderator;
        this.is_hand_raised = is_hand_raised;
        this.is_mic_on = is_mic_on;
        this.photo_url = photo_url;
        this.mentor_id = mentor_id;
        this.is_speaking = is_speaking;
        this.sort_order = sort_order;
    }

    public String getName() {
        return name;
    }

    public boolean isIs_speaker() {
        return is_speaker;
    }

    public boolean isIs_speaking() {
        return is_speaking;
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

    public String getMentor_id() {
        return mentor_id;
    }

    public int getSort_order() {
        return sort_order;
    }
}
