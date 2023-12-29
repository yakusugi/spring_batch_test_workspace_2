package com.springbatch.tasklet;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import java.util.Collections;

//... [Other imports and class declaration]

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.io.FileSystemResource;
import com.springbatch.domain.UserSpending;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;

public class CurrencyExchangeApiTasklet implements Tasklet {

    private final OkHttpClient client = new OkHttpClient();
    private final String apiKey = "GHSOg6VcH44yR9oKUQBm19JPhoGbq0mD";

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        String sourceCurrencyCode = (String) chunkContext.getStepContext().getJobParameters().get("sourceCurrencyCode");
        String targetCurrencyCode = (String) chunkContext.getStepContext().getJobParameters().get("targetCurrencyCode");

        // Setting up the reader for the CSV file
        FlatFileItemReader<UserSpending> reader = new FlatFileItemReader<>();
        reader.setResource(new FileSystemResource("src/main/resources/data/Product_Details_Output9.csv"));
        reader.setLineMapper(getLineMapper());
        reader.open(chunkContext.getStepContext().getStepExecution().getExecutionContext());

        UserSpending userSpending;
        while ((userSpending = reader.read()) != null) {
            // Call the API for each record
            double conversionRate = getConversionRate(userSpending.getSpendingDate(), sourceCurrencyCode, targetCurrencyCode);
            double convertedPrice = userSpending.getPrice() * conversionRate;

            System.out.println(String.format("Original: %f, Converted: %f", userSpending.getPrice(), convertedPrice));
            // You can also add this data to a list or another data structure
        }

        reader.close();
        return RepeatStatus.FINISHED;
    }

    private double getConversionRate(Date date, String sourceCurrency, String targetCurrency) throws IOException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate = dateFormat.format(date);

        String url = String.format("https://api.apilayer.com/exchangerates_data/%s?symbols=%s&base=%s", 
                                  formattedDate, targetCurrency, sourceCurrency);

        Request request = new Request.Builder()
            .url(url)
            .addHeader("apikey", apiKey)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            // Handle the response to extract the conversion rate
            return parseConversionRate(response.body().string(), targetCurrency);
        }
    }

    private double parseConversionRate(String responseBody, String targetCurrency) {
        // Implement JSON parsing logic to extract the conversion rate from the response
        // This is a placeholder logic. You need to replace it with actual parsing based on the response structure.
        return 0; // Placeholder return value
    }

    private DefaultLineMapper<UserSpending> getLineMapper() {
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setDelimiter(",");
        lineTokenizer.setNames(new String[]{"spendingId", "email", "spendingDate", "storeName", "productName", 
                                            "productType", "vatRate", "price", "note", "currencyCode", "quantity"});

        BeanWrapperFieldSetMapper<UserSpending> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(UserSpending.class);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        fieldSetMapper.setCustomEditors(Collections.singletonMap(Date.class, new CustomDateEditor(dateFormat, false)));

        DefaultLineMapper<UserSpending> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(lineTokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);

        return lineMapper;
    }
}
