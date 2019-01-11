package com.be.network;

import android.support.annotation.NonNull;

import com.annimon.stream.function.Consumer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import lombok.Getter;
import lombok.Setter;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CoreCall<ResponseType> implements Callback<ResponseType> {

    private Consumer<RawResponse<ResponseType>> responseCommand;
    private Request request;

    @Getter
    @Setter
    public static Runnable showProgress;
    @Getter
    @Setter
    public static Runnable hideProgress;

    /*
    Mock constructor only
     */
    public CoreCall() {
    }

    public CoreCall(Request request, Consumer<RawResponse<ResponseType>> responseCommand) {
        if (showProgress != null) {
            showProgress.run();
        }
        this.responseCommand = responseCommand;
        this.request = request;
    }

    //TODO: Use this for error stream read later
    public static void onError(HttpURLConnection httpURLConnection) throws IOException {
        if (hideProgress != null) {
            hideProgress.run();
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getErrorStream()));
        String line;
        StringBuilder output = new StringBuilder();
        while ((line = in.readLine()) != null) {
            output.append(line);
        }
       /* activity.runOnUiThread(() -> {
            Context context = UiContainer.getBaseContext();
            DialogHelper.showSimplestDialog(context, context.getString(R.string.error), output.toString(), null);
        });*/
    }

    @Override
    public void onFailure(@NonNull Call call, @NonNull Throwable t) {
        if (hideProgress != null) {
            hideProgress.run();
        }
        RawResponse rawResponse = new RawResponse();
        boolean showSnackbarMessage = false;
        if (request != null) {
            rawResponse.setRequest(request);
            //Logg.d(Str.Log.RestApi.REQUEST_STRING + request);
        }
        if (t instanceof SocketTimeoutException) {
            rawResponse.setNetworkResponseStatus(NetworkResponse.TIMEOUT);
            //  rawResponse.setErrorString(UiContainer.getBaseContext().getString(R.string.error_request_timed_out));
    /*        UiContainer.getActivity().runOnUiThread(() -> {
                Context context = UiContainer.getBaseContext();
                DialogHelper.showSimplestDialog(UiContainer.getActivity(), context.getString(R.string.error), context.getString(R.string.error_request_timed_out), null);
            });*/
        } else if (t instanceof IOException) {
            if (t instanceof UnknownHostException) {
             /*   UiContainer.getActivity().runOnUiThread(() -> {
                    Context context = UiContainer.getBaseContext();
                    DialogHelper.showSimplestDialog(UiContainer.getActivity(), context.getString(R.string.error), context.getString(R.string.error_request_no_internet_connection), null);
                });*/
                rawResponse.setNetworkResponseStatus(NetworkResponse.NO_CONNECTION);
                //rawResponse.setErrorString(UiContainer.getBaseContext().getString(R.string.error_request_no_internet_connection));
            } else {
                // possibly not only connection problems will get in this case, but full list of connection related exceptions is unclear
                rawResponse.setNetworkResponseStatus(NetworkResponse.NO_CONNECTION);
                showSnackbarMessage = request == null || !request.toString().contains("validateDevice");
            }
        } else {
            rawResponse.setNetworkResponseStatus(NetworkResponse.ERROR_UNEXPECTED);
        }
        if (showSnackbarMessage) {
            //TODO: Show error here
        }
        String message = t.getMessage();
        //Logg.w(S.Log.RestApi.RESPONSE_FAILURE + (message != null ? message : t.getClass()));
        responseCommand.accept(rawResponse);
    }

    @Override
    public void onResponse(@NonNull Call<ResponseType> call, @NonNull Response<ResponseType> response) {
        RawResponse rawResponse = new RawResponse<ResponseType>();
        if (request != null) {
            rawResponse.setRequest(request);
            //Logg.d(Str.Log.RestApi.REQUEST_STRING + request);
        }
        //Logg.d(Str.Log.RestApi.RESPONSE_CODE + response.code());
        //Logg.d(Str.Log.RestApi.RESPONSE_BODY + response.body());
        // We need to store error body cause if we read it once it will be cleared
        // And this must be done through call of string() method cause it's closes related resource
        if (response.errorBody() != null) {
          /*  try {
                if (PROTO_ERROR.equals(response.headers().get(PROTOBUF_OBJECT_TYPE_HEADER))) {
                    ProtoError protoError = ProtoError.parseFrom(response.errorBody().bytes());
                    rawResponse.setErrorString(protoError.getMessage());
                } else {
                    rawResponse.setErrorString(UiContainer.getBaseContext().getString(R.string.dialog_error_unknown));
                }
            } catch (IOException | NullPointerException e) {
                Logg.e(S.Log.FAIL);
            }*/
        }
        switch (response.code()) {
            // Generic cases
            case HttpURLConnection.HTTP_NO_CONTENT:
            case HttpURLConnection.HTTP_CREATED:
            case HttpURLConnection.HTTP_OK:
                rawResponse.setNetworkResponseStatus(NetworkResponse.SUCCESS);
                if (response.body() != null) {
                    rawResponse.setData(response.body());
                }
                break;
            case HttpURLConnection.HTTP_NOT_FOUND:
                rawResponse.setNetworkResponseStatus(NetworkResponse.NOT_FOUND);
                break;
            case HttpURLConnection.HTTP_INTERNAL_ERROR:
            case HttpURLConnection.HTTP_BAD_GATEWAY:
            case HttpURLConnection.HTTP_UNAVAILABLE:
            case HttpURLConnection.HTTP_GATEWAY_TIMEOUT:
                rawResponse.setNetworkResponseStatus(NetworkResponse.SERVER_ERROR);
                break;
            // Custom cases
            case HttpURLConnection.HTTP_BAD_REQUEST:
                // Strangely server returns 400 error code if signup request failed, e.g. user already exists
                rawResponse.setNetworkResponseStatus(NetworkResponse.SIGNUP_REQUEST_FAILED);
                break;
            case HttpURLConnection.HTTP_UNAUTHORIZED:
                rawResponse.setNetworkResponseStatus(NetworkResponse.AUTH_FAILED);
                break;
            // Default case. Shouldn't be reached
            default:
                //Logg.e(Str.Log.DEFAULT_CASE_REACHED + response.code());
                rawResponse.setNetworkResponseStatus(NetworkResponse.ERROR_UNEXPECTED);
                break;
        }
        if (responseCommand != null) {
            responseCommand.accept(rawResponse);
        }
        if (hideProgress != null) {
            hideProgress.run();
        }
    }
}

