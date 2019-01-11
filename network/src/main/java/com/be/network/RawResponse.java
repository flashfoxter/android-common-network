package com.be.network;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@ToString(callSuper = true)
public class RawResponse<Response> extends BaseResponse {

    @Getter
    @Setter
    private Response data;

    public RawResponse(NetworkResponse networkResponseStatus) {
        super(networkResponseStatus);
    }

}