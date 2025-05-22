package com.example.bankingapplication.Network; // Hoặc com.example.bankingapplication.Api

import com.example.bankingapplication.Model.Routes.ComputeRoutesRequest;
import com.example.bankingapplication.Model.Routes.ComputeRoutesResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface RoutesApiService {
    @POST("directions/v2:computeRoutes")
    Call<ComputeRoutesResponse> computeRoutes(
            @Header("X-Goog-Api-Key") String apiKey,
            @Header("X-Goog-FieldMask") String fieldMask,
            @Body ComputeRoutesRequest requestBody
    );
}