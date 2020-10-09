package com.tyagiabhinav.dialogflowchatlibrary.templateutil;


import com.tyagiabhinav.dialogflowchatlibrary.ImageDialogFragment;

public interface OnClickCallback {
    void OnUserClickAction(ReturnMessage msg, boolean isImageClick, ImageDialogFragment fragment); //TODO to be changed to Message class for sending info back to bot
}
