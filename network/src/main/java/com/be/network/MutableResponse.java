package com.be.network;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.annimon.stream.function.Consumer;

//import element.business.helpers.Dialogue;

public class MutableResponse extends MutableLiveData<RawResponse> {

    public void onChange(@NonNull LifecycleOwner owner, Consumer<RawResponse> responseSuccessConsumer) {
        observe(owner, r -> onBaseResponse(r, responseSuccessConsumer));
    }

    private void onBaseResponse(RawResponse response, Consumer<RawResponse> responseSuccessConsumer) {
        onBaseResponse(response, responseSuccessConsumer, null);
    }

    private void onBaseResponse(RawResponse response, Consumer<RawResponse> responseSuccessConsumer, @Nullable Consumer<RawResponse> withCustomError) {
        if (response != null) {
            if (response.getNetworkResponseStatus() == NetworkResponse.SUCCESS) {
                responseSuccessConsumer.accept(response);
            } else {
                if (withCustomError != null) {
                    withCustomError.accept(response);
                } else {
                    //Dialogue.showError(response.getErrorString());
                }
            }
        } else {
            //Dialogue.showError(R.string.common_unexpectedError);
        }
    }
}
