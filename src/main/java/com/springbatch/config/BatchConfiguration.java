package com.springbatch.config;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.batch.item.ItemReader; // Use the correct import
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;

import com.springbatch.domain.UserSpending;
import com.springbatch.reader.ProductNameItemReader;
import com.springbatch.domain.UserSpendingFieldSetMapper;
import com.springbatch.domain.UserSpendingRowMapper;

import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;


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
	public ItemReader<UserSpending> jdbcCursorItemReader(@Value("#{jobParameters['storeName']}") String storeName) {
		JdbcCursorItemReader<UserSpending> itemReader = new JdbcCursorItemReader<>();
		itemReader.setDataSource(dataSource);
		itemReader.setSql("select * from budget_tracker_db.user_spending where estore_name = '\" + storeName + \"' order by spending_id");
		itemReader.setRowMapper(new UserSpendingRowMapper());
		return itemReader;
	}
	
	@Bean
	public ItemWriter<UserSpending> flatFileItemWriter() throws Exception {
		FlatFileItemWriter<UserSpending> itemWriter = new FlatFileItemWriter<>();
		itemWriter.setResource(new FileSystemResource("src/main/resources/data/Product_Details_Output2.csv"));
		;
		DelimitedLineAggregator<UserSpending> lineAggregator = new DelimitedLineAggregator<>();
		lineAggregator.setDelimiter(",");
		
		BeanWrapperFieldExtractor<UserSpending> fieldExtractor = new BeanWrapperFieldExtractor<>();
		fieldExtractor.setNames(new String[] {"spendingId", "email", "spendingDate", "storeName","productName","productType","vatRate","price","note","currencyCode","quantity"});
		
		lineAggregator.setFieldExtractor(fieldExtractor);
		
		itemWriter.setLineAggregator(lineAggregator);
		
		return itemWriter;
	}

	//for reading
    @Bean
    public Step step1(@Value("#{jobParameters['storeName']}") String storeName)  throws Exception {
        return this.stepBuilderFactory.get("chunkBasedStep1")
                .<UserSpending, UserSpending>chunk(3)
                .reader(jdbcCursorItemReader(storeName))
                .writer(new ItemWriter<UserSpending>() {
                
                @Override
                public void write(List<? extends UserSpending> items) throws Exception {
        		System.out.println("Chunk processing started");
        		items.forEach(System.out::println);
        		System.out.println("Chunk processing ended");
        	}
        }).build();
    }

    @Bean
    public Job firstJob(@Value("#{jobParameters['storeName']}") String storeName) throws Exception {
        return this.jobBuilderFactory.get("job1")
                .start(step1(storeName))
                .build();
    }
}
