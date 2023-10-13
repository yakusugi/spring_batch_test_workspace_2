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

//    @Bean
//    public ItemReader<String> itemReader() {
//        List<String> productList = new ArrayList<>();
//        productList.add("Product 1");
//        productList.add("Product 2");
//        productList.add("Product 3");
//        productList.add("Product 4");
//        productList.add("Product 5");
//        productList.add("Product 6");
//        productList.add("Product 7");
//        productList.add("Product 8");
//        
//        return new ProductNameItemReader(productList);
//    }
    
	@Bean
	public ItemReader<UserSpending> flatFileItemReader() {
		FlatFileItemReader<UserSpending> itemReader = new FlatFileItemReader<>();
		itemReader.setLinesToSkip(1);
		itemReader.setResource(new ClassPathResource("/data/Product_Details.csv"));

		DefaultLineMapper<UserSpending> lineMapper = new DefaultLineMapper<>();

		DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
		lineTokenizer.setNames("product_id", "product_name", "product_category", "product_price");

		lineMapper.setLineTokenizer(lineTokenizer);
		lineMapper.setFieldSetMapper(new UserSpendingFieldSetMapper());

		itemReader.setLineMapper(lineMapper);

		return itemReader;
	}
	
	@Bean
	public ItemReader<UserSpending> jdbcCursorItemReader() {
		JdbcCursorItemReader<UserSpending> itemReader = new JdbcCursorItemReader<>();
		itemReader.setDataSource(dataSource);
		itemReader.setSql("select * from budget_tracker_db.user_spending where email = 'masterplan_life@protonmail.com' order by spending_id");
		itemReader.setRowMapper(new UserSpendingRowMapper());
		return itemReader;
		
	}
	
//	@Bean
//	public ItemReader<UserSpending> jdbcPagingItemReader() throws Exception {
//		JdbcPagingItemReader<UserSpending> itemReader = new JdbcPagingItemReader<>();
//		itemReader.setDataSource(dataSource);
//		
//		SqlPagingQueryProviderFactoryBean factory = new SqlPagingQueryProviderFactoryBean();
//		factory.setDataSource(dataSource);
//		factory.setSelectClause("select PRODUCT_ID, PRODUCT_NAME, PRODUCT_CATEGORY, PRODUCT_PRICE");
//		factory.setFromClause("from PRODUCT_DETAILS");
//		factory.setSortKey("PRODUCT_ID");
//		
//		itemReader.setQueryProvider(factory.getObject());
//		itemReader.setRowMapper(new UserSpendingRowMapper());
//		itemReader.setPageSize(2);
//		
//		return itemReader;
//	}
	
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
    public Step step1() throws Exception {
        return this.stepBuilderFactory.get("chunkBasedStep1")
                .<UserSpending, UserSpending>chunk(3)
//                .reader(flatFileItemReader())
                .reader(jdbcCursorItemReader())
//                .reader(jdbcPagingItemReader())
                .writer(new ItemWriter<UserSpending>() {
                
                @Override
                public void write(List<? extends UserSpending> items) throws Exception {
        		System.out.println("Chunk processing started");
        		items.forEach(System.out::println);
        		System.out.println("Chunk processing ended");
        	}
        }).build();
    }
    
	
	
//    @Bean
//    public Step step1() throws Exception {
//        return this.stepBuilderFactory.get("chunkBasedStep1")
//                .<UserSpending, UserSpending>chunk(3)
//                  .reader(flatFileItemReader())
//                  .reader(jdbcCursorItemReader())
//                .reader(jdbcPagingItemReader())
//                .writer(flatFileItemWriter()).build();
//    }
   
    // for writing to a csv file
//    @Bean
//    public Step step1() throws Exception {
//        return this.stepBuilderFactory.get("chunkBasedStep1")
//                .<UserSpending, UserSpending>chunk(3)
//                  .reader(flatFileItemReader())
////                  .reader(jdbcCursorItemReader())
//                .reader(jdbcCursorItemReader())
//                .writer(flatFileItemWriter()).build();
//    }

    @Bean
    public Job firstJob() throws Exception {
        return this.jobBuilderFactory.get("job1")
                .start(step1())
                .build();
    }
}
