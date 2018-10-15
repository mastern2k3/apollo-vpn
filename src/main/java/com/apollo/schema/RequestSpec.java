package com.apollo.schema;

import java.util.UUID;

public class RequestSpec {

    String uri;
    String id;

    public static RequestSpec of(String uri) {
        RequestSpec requestSpec = new RequestSpec();

        requestSpec.uri = uri;
        requestSpec.id = UUID.randomUUID().toString();

        return requestSpec;
    }
}
