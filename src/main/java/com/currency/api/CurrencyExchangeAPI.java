package com.currency.api;

import java.io.*;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.lang.NonNull;

import okhttp3.*;

public class CurrencyExchangeAPI {
	private static String convertedResult;

	public static String currencyConverter(String currentCurrencyString, String targetCurrencyString,
			Double bankBalanceNum) throws IOException {
		OkHttpClient client = new OkHttpClient().newBuilder().build();
		String url1 = "https://api.apilayer.com/exchangerates_data/convert?to=";

		// YouTube tutorial
		String url = url1 + targetCurrencyString + "&from=" + currentCurrencyString + "&amount=" + bankBalanceNum;
		String apiKey = "GHSOg6VcH44yR9oKUQBm19JPhoGbq0mD";

		Request request = new Request.Builder().url(url).addHeader("apiKey", apiKey).build();

		client.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(@NonNull Call call, @NonNull IOException e) {
				e.printStackTrace();
			}

			@Override
			public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
				String myResponse = response.body().string();

				JSONObject obj = null;
				try {
					obj = new JSONObject(myResponse);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				try {
					convertedResult = obj.getString("result");
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		});

		return convertedResult;
	}

}
