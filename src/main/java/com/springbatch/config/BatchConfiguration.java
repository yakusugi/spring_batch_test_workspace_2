package com.springbatch.config;

import com.springbatch.domain.UserSpending;
import com.springbatch.domain.UserSpendingRowMapper;
import com.springbatch.validation.EmailValidation;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
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
import org.springframework.core.io.FileSystemResource;

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

    @Bean
    @JobScope
    public ItemStreamReader<UserSpending> jdbcCursorItemReader(
    		@Value("#{jobParameters['storeName']}") String storeName,
    		@Value("#{jobParameters['email']}") String email
    		) {
    	// Validate the email using EmailValidation class
//        EmailValidation emailValidation = new EmailValidation();
//        if (!emailValidation.isEmailValid(email)) {
//            // Handle invalid email (e.g., exit the job, throw an exception, etc.)
//            throw new RuntimeException("Invalid email provided: " + email);
//        }
    	
        JdbcCursorItemReader<UserSpending> itemReader = new JdbcCursorItemReader<>();
        itemReader.setDataSource(dataSource);
        //todo: fix sql
        itemReader.setSql("select * from user_spending where store_name = '" + storeName + "' and email = '" + email + "' order by spending_id");
        itemReader.setRowMapper(new UserSpendingRowMapper());
        return itemReader;
    }

    @Bean
    @JobScope
    public ItemStreamWriter<UserSpending> flatFileItemWriter() throws Exception {
        FlatFileItemWriter<UserSpending> itemWriter = new FlatFileItemWriter<>();
        itemWriter.setResource(new FileSystemResource("src/main/resources/data/Product_Details_Output4.csv"));

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
        return this.stepBuilderFactory.get("step1")
                .<UserSpending, UserSpending>chunk(3)
                .reader(reader)
                .writer(writer)
                .build();
    }

    @Bean
    public Job firstJob(Step step1) {
        return this.jobBuilderFactory.get("job1")
                .start(step1)
                .build();
    }
}