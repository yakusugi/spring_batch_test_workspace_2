package com.springbatch.tasklet;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
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

public class CurrencyExchangeApiTasklet implements Tasklet {

	private final String apiKey = "GHSOg6VcH44yR9oKUQBm19JPhoGbq0mD";

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		String sourceCurrencyCode = (String) chunkContext.getStepContext().getJobParameters().get("sourceCurrencyCode");
		String targetCurrencyCode = (String) chunkContext.getStepContext().getJobParameters().get("targetCurrencyCode");

		Double totalPrice = (Double) chunkContext.getStepContext().getStepExecution().getJobExecution()
				.getExecutionContext().get("totalPrice");

		if (totalPrice == null) {
			throw new IllegalStateException("Total price not set in the execution context.");
		}

		// Ensure the currency codes are valid
		if (sourceCurrencyCode == null || targetCurrencyCode == null) {
			throw new IllegalStateException("Currency codes not set properly.");
		}

		System.out.println(String.format("source and target (%s to %s)", targetCurrencyCode, sourceCurrencyCode));

		String apiUrl = String.format("https://api.apilayer.com/exchangerates_data/latest?symbols=%s&base=%s",
				sourceCurrencyCode, targetCurrencyCode);

		HttpHeaders headers = new HttpHeaders();
		headers.set("apikey", apiKey);
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, String.class);

		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode rootNode = objectMapper.readTree(response.getBody());
		JsonNode ratesNode = rootNode.path("rates");
		double conversionRate = ratesNode.path(sourceCurrencyCode).asDouble();

		double convertedTotal = totalPrice * conversionRate;

		System.out.println(String.format("Converted Total (%s to %s): %f", targetCurrencyCode, sourceCurrencyCode,
				convertedTotal));
		
		chunkContext.getStepContext()
    		.getStepExecution()
    		.getJobExecution()
    		.getExecutionContext()
    		.put("convertedTotal", convertedTotal);

		return RepeatStatus.FINISHED;
	}
}
