package com.be.network;

import android.arch.lifecycle.ViewModel;

import retrofit2.Call;

public class BaseViewModel extends ViewModel {

    protected MutableResponse call(Call<?> call) {
        final MutableResponse data = new MutableResponse();
        call.enqueue(new CoreCall<>(call.request(), data::postValue));
        return data;
    }
}

