SELECT SUM(price) AS total_sum
FROM user_spending
WHERE spending_date BETWEEN '%d' AND '%d'
AND currency_code = '%s'
AND email = '%s'