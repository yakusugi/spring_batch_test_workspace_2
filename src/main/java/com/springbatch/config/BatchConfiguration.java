package com.springbatch.config;

import com.springbatch.domain.UserSpending;
import com.springbatch.domain.UserSpendingRowMapper;
import com.springbatch.tasklet.ExitCodeCheckingTasklet;
import com.springbatch.validation.EmailValidation;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.batch.item.ItemStreamReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import javax.batch.api.chunk.ItemProcessor;
import javax.sql.DataSource;


@Configuration
@EnableBatchProcessing
public class BatchConfiguration {

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Autowired
    public DataSource dataSource;
    
    //Validation check (currency exist)
    @Bean
    @JobScope
    public ItemStreamReader<UserSpending> currencyExistingValidation(
    		@Value("#{jobParameters['email']}") String email,
    		@Value("#{jobParameters['sourceCurrencyCode']}") String sourceCurrencyCode,
    		@Value("#{jobParameters['targetCurrencyCode']}") String targetCurrencyCode
    		) {
    	
//        JdbcCursorItemReader<UserSpending> itemReader = new JdbcCursorItemReader<>();
//        itemReader.setDataSource(dataSource);
//        //todo: fix sql
//        itemReader.setSql("select * from user_spending where email = '" + email + "' and currency_code = '" + sourceCurrencyCode + "' or currency_code = '" + targetCurrencyCode + "' order by spending_id");
//        itemReader.setRowMapper(new UserSpendingRowMapper());
//        return itemReader;
        
        JdbcCursorItemReader<UserSpending> itemReader = new JdbcCursorItemReader<>();
        itemReader.setDataSource(dataSource);
        itemReader.setSql(getSqlFromFile("src/main/resources/data/sql/user_spending_query.sql", email, sourceCurrencyCode, targetCurrencyCode));
        itemReader.setRowMapper(new UserSpendingRowMapper());
        return itemReader;
    }
    
    private String getSqlFromFile(String filePath, String email, String sourceCurrencyCode, String targetCurrencyCode) {
        // Load SQL from file
        Resource resource = new ClassPathResource(filePath);
        String sqlTemplate = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            sqlTemplate = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            // Handle exception
            e.printStackTrace();
        }

        // Replace placeholders with actual values
        return String.format(sqlTemplate, email, sourceCurrencyCode, targetCurrencyCode);
    }
    
    
    @Bean
    @JobScope
    public ItemStreamWriter<UserSpending> flatFileItemWriter() throws Exception {
        FlatFileItemWriter<UserSpending> itemWriter = new FlatFileItemWriter<>();
        itemWriter.setResource(new FileSystemResource("src/main/resources/data/Product_Details_Output8.csv"));

        DelimitedLineAggregator<UserSpending> lineAggregator = new DelimitedLineAggregator<>();
        lineAggregator.setDelimiter(",");

        BeanWrapperFieldExtractor<UserSpending> fieldExtractor = new BeanWrapperFieldExtractor<>();
        fieldExtractor.setNames(new String[]{"spendingId", "email", "spendingDate", "storeName", "productName", "productType", "vatRate", "price", "note", "currencyCode", "quantity"});

        lineAggregator.setFieldExtractor(fieldExtractor);

        itemWriter.setLineAggregator(lineAggregator);

        return itemWriter;
    }

    //for reading
    @Bean
    @JobScope
    public Step step1(ItemReader<UserSpending> reader, ItemWriter<UserSpending> writer) {
    	TaskletStep step = this.stepBuilderFactory.get("step1")
                .<UserSpending, UserSpending>chunk(3)
                .reader(currencyExistingValidation(null, null, null))
                .writer(writer)
                .build();
        
        step.setTasklet(new ExitCodeCheckingTasklet(step.getTasklet()));

        return step;
    }
    
    

    @Bean
    public Job firstJob(Step step1) {
        return this.jobBuilderFactory.get("job1")
                .start(step1)
                .build();
    }
}