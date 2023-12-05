package com.currency_converter;

import javax.sql.DataSource;

import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.springbatch.domain.Users;
import com.springbatch.domain.UsersRowMapper;

public class CurrencyConverter {
	
	@Autowired
    public DataSource dataSource;
	
	public JdbcCursorItemReader<Users> jdbcCursorItemReader(@Value("#{jobParameters['email']}") String email) {
		JdbcCursorItemReader<Users> itemReader = new JdbcCursorItemReader<>();
        itemReader.setDataSource(dataSource);
        itemReader.setSql("select * from users where email = email = '" + email + "'");
        itemReader.setRowMapper(new UsersRowMapper());
        return (JdbcCursorItemReader<Users>) itemReader;
    }
	

}
