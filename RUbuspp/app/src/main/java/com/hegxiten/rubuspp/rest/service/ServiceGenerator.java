package com.hegxiten.rubuspp.rest.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hegxiten.rubuspp.utils.Utils;
import com.squareup.okhttp.OkHttpClient;

import retrofit.RestAdapter;
import retrofit.client.OkClient;
import retrofit.converter.GsonConverter;

import static com.hegxiten.rubuspp.utils.Utils.BASE_URL;

/**
 * Created by Shyamal on 3/15/2015.
 */
public class ServiceGenerator {
    // No need to instantiate this class.
    private ServiceGenerator() {}

    public static <S> S createService(Class<S> serviceClass,
                                      String baseUrl,
                                      final String key,
                                      String agencyId,
                                      Utils.TransLocDataType type)
    {
        Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(new ItemTypeAdapterFactory(agencyId, type))
                .setDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss'.'SSS'Z'")
                .create();

        RestAdapter.Builder builder = new RestAdapter.Builder()
                .setEndpoint(BASE_URL)
                .setClient(new OkClient(new OkHttpClient()))
                .setConverter(new GsonConverter(gson))
                .setRequestInterceptor(request -> request.addHeader("x-rapidapi-host", BASE_URL))
                .setRequestInterceptor(request -> request.addHeader("x-rapidapi-key", key));
        RestAdapter adapter = builder.build();
        return adapter.create(serviceClass);
    }
}
