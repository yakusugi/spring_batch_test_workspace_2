package com.springbatch.controller;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JobLaunchController {

	@Autowired
	private JobLauncher jobLauncher;
	
	@Autowired
	@Qualifier("firstJob")
	private Job job;
	
	@GetMapping("/launchJob/{email}/{sourceCurrencyCode}/{targetCurrencyCode}")
	public void handle(
			@PathVariable("email") String email,
			@PathVariable("sourceCurrencyCode") String sourceCurrencyCode,
			@PathVariable("targetCurrencyCode") String targetCurrencyCode
			) throws Exception {
		
		JobParameters jobParameters = new JobParametersBuilder()
				.addString("email", email)
				.addString("sourceCurrencyCode", sourceCurrencyCode)
				.addString("targetCurrencyCode", targetCurrencyCode)
				.toJobParameters();
		jobLauncher.run(job, jobParameters);
		
	}
}
