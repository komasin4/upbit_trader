CREATE DEFINER=`root`@`192.168.1.%` FUNCTION `CALC_MA`(
	`DATESTRING` VARCHAR(20),
	`NUM_MA` INTEGER
)
RETURNS double
LANGUAGE SQL
NOT DETERMINISTIC
CONTAINS SQL
SQL SECURITY DEFINER
COMMENT ''
BEGIN

	DECLARE AVG_VALUE DOUBLE;

	SELECT ROUND(AVG(trade_price))
    INTO AVG_VALUE
	FROM (
		SELECT trade_price
		FROM candle
		WHERE candle_time <= DATESTRING
		ORDER BY candle_time DESC
		LIMIT NUM_MA) AS _TMP;

RETURN AVG_VALUE;

END

CREATE DEFINER=`root`@`192.168.1.%` FUNCTION `CALC_BB_UPPER`(
	`DATESTRING` VARCHAR(20),
	`NUM_MA` INTEGER
)
RETURNS double
LANGUAGE SQL
NOT DETERMINISTIC
CONTAINS SQL
SQL SECURITY DEFINER
COMMENT ''
BEGIN

	DECLARE AVG_VALUE DOUBLE;

	SELECT ROUND(AVG(trade_price) + STD(trade_price) * 2)
    INTO AVG_VALUE
	FROM (
		SELECT trade_price
		FROM candle
		WHERE candle_time <= DATESTRING
		ORDER BY candle_time DESC
		LIMIT NUM_MA) AS _TMP;

RETURN AVG_VALUE;

END

CREATE DEFINER=`root`@`192.168.1.%` FUNCTION `CALC_BB_LOWER`(
	`DATESTRING` VARCHAR(20),
	`NUM_MA` INTEGER
)
RETURNS double
LANGUAGE SQL
NOT DETERMINISTIC
CONTAINS SQL
SQL SECURITY DEFINER
COMMENT ''
BEGIN

	DECLARE AVG_VALUE DOUBLE;

	SELECT ROUND(AVG(trade_price) - STD(trade_price) * 2)
    INTO AVG_VALUE
	FROM (
		SELECT trade_price
		FROM candle
		WHERE candle_time <= DATESTRING
		ORDER BY candle_time DESC
		LIMIT NUM_MA) AS _TMP;

RETURN AVG_VALUE;

END