package com.springbatch.tasklet;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.core.io.FileSystemResource;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import com.springbatch.domain.UserSpending;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Collections;

public class TotalPriceCalculatorTasklet implements Tasklet {

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        FlatFileItemReader<UserSpending> reader = new FlatFileItemReader<>();
        reader.setResource(new FileSystemResource("src/main/resources/data/Product_Details_Output9.csv")); // Adjust the path if needed

        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setDelimiter(",");
        tokenizer.setNames(new String[]{"spendingId", "email", "spendingDate", "storeName", "productName", 
                                        "productType", "vatRate", "price", "note", "currencyCode", "quantity"});

        BeanWrapperFieldSetMapper<UserSpending> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(UserSpending.class);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        fieldSetMapper.setCustomEditors(Collections.singletonMap(Date.class, new CustomDateEditor(dateFormat, false)));

        DefaultLineMapper<UserSpending> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);

        reader.setLineMapper(lineMapper);
        reader.open(chunkContext.getStepContext().getStepExecution().getExecutionContext());

        UserSpending userSpending;
        double totalPrice = 0.0;

        while((userSpending = reader.read()) != null) {
            totalPrice += userSpending.getPrice(); // Assuming getPrice() returns the price as a double
        }

        System.out.println("Total Price: " + totalPrice);
        
        chunkContext.getStepContext()
        	.getStepExecution()
        	.getJobExecution()
        	.getExecutionContext()
        	.put("totalPrice", totalPrice);

        reader.close();
        return RepeatStatus.FINISHED;
    }
}
